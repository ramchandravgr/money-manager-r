package com.rrparallelstudio.miapp.core.parser.scanner

import android.content.Context
import android.net.Uri
import com.rrparallelstudio.miapp.core.parser.duplicate.SmsCrossChannelDedup
import com.rrparallelstudio.miapp.core.parser.engine.SmsParserEngine
import com.rrparallelstudio.miapp.core.parser.model.RawSmsMessage
import com.rrparallelstudio.miapp.core.parser.model.SmsInboxScanSummary
import com.rrparallelstudio.miapp.core.parser.model.SmsParseIntent
import com.rrparallelstudio.miapp.core.parser.model.SmsParseResult
import com.rrparallelstudio.miapp.core.parser.permission.SmsIntakeCapabilities
import com.rrparallelstudio.miapp.core.parser.permission.SmsPermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSmsScannerService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val smsParserEngine: SmsParserEngine,
    ) {
        /**
         * Reads device SMS inbox messages purely locally for the given lookback window.
         * No data is transmitted outside the device.
         */
        suspend fun scanInbox(monthsBack: Int): SmsInboxScanSummary =
            withContext(Dispatchers.IO) {
                if (!SmsIntakeCapabilities.inboxScanAllowed(context)) {
                    return@withContext SmsInboxScanSummary(transactions = emptyList())
                }
                if (!SmsPermissionHelper.hasSmsPermission(context)) {
                    return@withContext SmsInboxScanSummary(transactions = emptyList())
                }
                val sinceTimestamp = SmsScanPeriod.sinceTimestampMillis(monthsBack)
                val candidates = mutableListOf<com.rrparallelstudio.miapp.core.parser.model.ParsedSmsCandidate>()
                val cardRefunds = mutableListOf<com.rrparallelstudio.miapp.core.parser.model.ParsedSmsCandidate>()
                var mandateNotices = 0
                var statementNotices = 0
                var investmentNotices = 0
                var cardPaymentNotices = 0
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf("_id", "address", "body", "date")
                val selection = "date >= ?"
                val selectionArgs = arrayOf(sinceTimestamp.toString())
                val sortOrder = "date DESC"

                try {
                    val cursor =
                        context.contentResolver.query(
                            uri,
                            projection,
                            selection,
                            selectionArgs,
                            sortOrder,
                        )

                    cursor?.use {
                        val idIdx = it.getColumnIndexOrThrow("_id")
                        val addressIdx = it.getColumnIndexOrThrow("address")
                        val bodyIdx = it.getColumnIndexOrThrow("body")
                        val dateIdx = it.getColumnIndexOrThrow("date")

                        while (it.moveToNext()) {
                            val id = it.getString(idIdx) ?: ""
                            val sender = it.getString(addressIdx) ?: ""
                            val body = it.getString(bodyIdx) ?: ""
                            val date = it.getLong(dateIdx)

                            val raw =
                                RawSmsMessage(
                                    id = id,
                                    sender = sender,
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
                } catch (e: SecurityException) {
                    return@withContext SmsInboxScanSummary(transactions = emptyList())
                } catch (e: Exception) {
                    return@withContext SmsInboxScanSummary(transactions = emptyList())
                }

                val deduped = SmsCrossChannelDedup.deduplicate(candidates)
                val dedupedRefunds = SmsCrossChannelDedup.deduplicate(cardRefunds)
                SmsInboxScanSummary(
                    transactions = deduped.kept,
                    mandateNotices = mandateNotices,
                    statementNotices = statementNotices,
                    investmentNotices = investmentNotices,
                    cardPaymentNotices = cardPaymentNotices,
                    cardRefunds = dedupedRefunds.kept,
                    duplicateSuppressed = deduped.suppressedCount + dedupedRefunds.suppressedCount,
                )
            }
    }
