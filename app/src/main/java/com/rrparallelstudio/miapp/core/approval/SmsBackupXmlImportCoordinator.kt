package com.rrparallelstudio.miapp.core.approval

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.rrparallelstudio.miapp.core.data.preferences.SmsBackupFolderStore
import com.rrparallelstudio.miapp.core.data.preferences.SmsScanSettingsStore
import com.rrparallelstudio.miapp.core.database.dao.SmsMessageRecordDao
import com.rrparallelstudio.miapp.core.domain.repository.ApprovalRepository
import com.rrparallelstudio.miapp.core.parser.duplicate.SmsCrossChannelDedup
import com.rrparallelstudio.miapp.core.parser.engine.SmsParserEngine
import com.rrparallelstudio.miapp.core.parser.model.ParsedSmsCandidate
import com.rrparallelstudio.miapp.core.parser.model.RawSmsMessage
import com.rrparallelstudio.miapp.core.parser.model.SmsInboxScanSummary
import com.rrparallelstudio.miapp.core.parser.model.SmsParseIntent
import com.rrparallelstudio.miapp.core.parser.model.SmsParseResult
import com.rrparallelstudio.miapp.core.parser.scanner.SmsScanPeriod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsBackupXmlImportCoordinator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val smsBackupFolderStore: SmsBackupFolderStore,
        private val smsScanSettingsStore: SmsScanSettingsStore,
        private val smsMessageRecordDao: SmsMessageRecordDao,
        private val smsParserEngine: SmsParserEngine,
        private val approvalRepository: ApprovalRepository,
    ) {
        private val coldStartImportAttempted = AtomicBoolean(false)

        suspend fun importLatest(updateUserMessage: Boolean): ImportResult =
            withContext(Dispatchers.IO) {
                val treeUri =
                    smsBackupFolderStore.getFolderTreeUri()
                        ?: return@withContext ImportResult(
                            userMessage =
                                if (updateUserMessage) {
                                    "No SMS backup folder selected. Open Settings → Transaction scan."
                                } else {
                                    null
                                },
                        )

                val latest =
                    findLatestBackupFile(treeUri)
                        ?: return@withContext ImportResult(
                            userMessage =
                                if (updateUserMessage) {
                                    "No sms-*.xml backup found in the selected folder."
                                } else {
                                    null
                                },
                        )

                val sinceTimestamp = SmsScanPeriod.sinceTimestampMillis(smsScanSettingsStore.getScanMonths())
                val watermark = smsMessageRecordDao.maxReceivedTime() ?: 0L

                if (!updateUserMessage && !coldStartImportAttempted.compareAndSet(false, true)) {
                    return@withContext ImportResult(userMessage = null)
                }

                val sourceKey = latest.uri.toString()
                val lastModified = latest.lastModified()
                if (
                    sourceKey == smsBackupFolderStore.getLastImportSourceUri() &&
                    lastModified > 0L &&
                    lastModified == smsBackupFolderStore.getLastImportSourceModified() &&
                    watermark > 0L
                ) {
                    val backupDate = peekBackupDateMillis(latest.uri)
                    if (backupDate != null && backupDate <= watermark) {
                        return@withContext ImportResult(
                            userMessage =
                                if (updateUserMessage) {
                                    "SMS backups are already up to date."
                                } else {
                                    null
                                },
                        )
                    }
                }

                try {
                    val summary =
                        parseBackupFile(
                            fileUri = latest.uri,
                            sinceTimestamp = sinceTimestamp,
                            watermarkMillis = watermark,
                        )

                    if (
                        summary.transactions.isEmpty() &&
                        summary.cardRefunds.isEmpty() &&
                        summary.informationalCount == 0
                    ) {
                        smsBackupFolderStore.recordSuccessfulImport(sourceKey, lastModified, latest.name)
                        return@withContext ImportResult(
                            userMessage =
                                if (updateUserMessage) {
                                    "No new bank SMS found in ${latest.name ?: "backup"}."
                                } else {
                                    null
                                },
                        )
                    }

                    val cleared =
                        if (updateUserMessage) {
                            approvalRepository.reconcilePendingWithCurrentParser()
                        } else {
                            0
                        }
                    val addedTxn = approvalRepository.insertCandidates(summary.transactions)
                    val addedRefund = approvalRepository.insertCandidates(summary.cardRefunds)
                    smsBackupFolderStore.recordSuccessfulImport(sourceKey, lastModified, latest.name)

                    val message =
                        if (updateUserMessage) {
                            ApprovalInboxSmsScanCoordinator
                                .buildScanResultMessage(
                                    added = addedTxn + addedRefund,
                                    txnCount = summary.transactions.size + summary.cardRefunds.size,
                                    summary = summary,
                                    clearedPending = cleared,
                                ).let { base ->
                                    buildString {
                                        append("Imported from ${latest.name ?: "backup"}. ")
                                        append(base)
                                    }
                                }
                        } else {
                            null
                        }

                    ImportResult(
                        userMessage = message,
                        addedCount = addedTxn + addedRefund,
                    )
                } catch (_: Exception) {
                    return@withContext ImportResult(
                        userMessage =
                            if (updateUserMessage) {
                                "Could not read SMS backup file. Check that it is a valid SMS Backup & Restore export."
                            } else {
                                null
                            },
                    )
                }
            }

        private fun findLatestBackupFile(treeUri: Uri): DocumentFile? {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val pattern = Regex("""sms-\d{14}\.xml""", RegexOption.IGNORE_CASE)
            return root
                .listFiles()
                .filter { it.isFile && pattern.matches(it.name.orEmpty()) }
                .maxByOrNull { it.name.orEmpty() }
        }

        private fun peekBackupDateMillis(fileUri: Uri): Long? {
            return try {
                context.contentResolver.openInputStream(fileUri)?.use { input ->
                    val buffer = ByteArray(4096)
                    val read = input.read(buffer)
                    if (read <= 0) return null
                    val chunk = buffer.copyOf(read).toString(Charsets.UTF_8)
                    val match = BACKUP_DATE_ATTR.find(chunk) ?: return null
                    match.groupValues[1].toLongOrNull()
                }
            } catch (_: Exception) {
                null
            }
        }

        private fun parseBackupFile(
            fileUri: Uri,
            sinceTimestamp: Long,
            watermarkMillis: Long,
        ): SmsInboxScanSummary =
            try {
                parseBackupFileUnchecked(fileUri, sinceTimestamp, watermarkMillis)
            } catch (_: Exception) {
                SmsInboxScanSummary(transactions = emptyList())
            }

        private fun parseBackupFileUnchecked(
            fileUri: Uri,
            sinceTimestamp: Long,
            watermarkMillis: Long,
        ): SmsInboxScanSummary {
            val candidates = mutableListOf<ParsedSmsCandidate>()
            val cardRefunds = mutableListOf<ParsedSmsCandidate>()
            var mandateNotices = 0
            var statementNotices = 0
            var investmentNotices = 0
            var cardPaymentNotices = 0

            context.contentResolver.openInputStream(fileUri)?.use { input ->
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(input, "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "sms") {
                        val address = parser.getAttributeValue(null, "address").orEmpty()
                        val body = parser.getAttributeValue(null, "body").orEmpty()
                        val date = parser.getAttributeValue(null, "date")?.toLongOrNull() ?: 0L
                        val inScanWindow = date >= sinceTimestamp
                        val newerThanWatermark = watermarkMillis <= 0L || date > watermarkMillis
                        if (body.isNotBlank() && inScanWindow && newerThanWatermark) {
                            val raw =
                                RawSmsMessage(
                                    id = "xml-$address-$date",
                                    sender = address,
                                    body = body,
                                    timestamp = date,
                                )
                            when (val result = smsParserEngine.parseResult(raw)) {
                                is SmsParseResult.Transaction -> candidates.add(result.candidate)
                                is SmsParseResult.CardRefund -> cardRefunds.add(result.candidate)
                                is SmsParseResult.Informational ->
                                    when (result.intent) {
                                        SmsParseIntent.MANDATE -> mandateNotices++
                                        SmsParseIntent.STATEMENT -> statementNotices++
                                        SmsParseIntent.INVESTMENT -> investmentNotices++
                                        SmsParseIntent.CARD_PAYMENT -> cardPaymentNotices++
                                        SmsParseIntent.CARD_REFUND -> Unit
                                        SmsParseIntent.SETTLED -> Unit
                                    }
                                SmsParseResult.Ignored -> Unit
                            }
                        }
                    }
                    event = parser.next()
                }
            }

            val deduped = SmsCrossChannelDedup.deduplicate(candidates)
            val dedupedRefunds = SmsCrossChannelDedup.deduplicate(cardRefunds)
            return SmsInboxScanSummary(
                transactions = deduped.kept,
                mandateNotices = mandateNotices,
                statementNotices = statementNotices,
                investmentNotices = investmentNotices,
                cardPaymentNotices = cardPaymentNotices,
                cardRefunds = dedupedRefunds.kept,
                duplicateSuppressed = deduped.suppressedCount + dedupedRefunds.suppressedCount,
            )
        }

        data class ImportResult(
            val userMessage: String?,
            val addedCount: Int = 0,
        )

        private companion object {
            val BACKUP_DATE_ATTR = Regex("""backup_date="(\d+)"""")
        }
    }
