package com.rrparallelstudio.miapp.core.approval

import android.content.Context
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.data.preferences.SmsBackupFolderStore
import com.rrparallelstudio.miapp.core.parser.permission.SmsIntakeCapabilities
import com.rrparallelstudio.miapp.core.parser.permission.SmsPermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play release: SMS Backup XML from [SmsBackupFolderStore].
 * Debug / sideload APK (READ_SMS in manifest): device inbox first.
 */
@Singleton
class ApprovalSmsIntakeCoordinator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val smsBackupFolderStore: SmsBackupFolderStore,
        private val smsBackupXmlImportCoordinator: SmsBackupXmlImportCoordinator,
        private val approvalInboxSmsScanCoordinator: ApprovalInboxSmsScanCoordinator,
    ) {
        suspend fun importOnColdStartIfConfigured() {
            runCatching {
                if (SmsIntakeCapabilities.inboxScanAllowed(context) &&
                    SmsPermissionHelper.hasSmsPermission(context)
                ) {
                    approvalInboxSmsScanCoordinator.scanInboxOnColdStartIfPermitted()
                    return@runCatching
                }
                if (smsBackupFolderStore.isConfigured()) {
                    smsBackupXmlImportCoordinator.importLatest(updateUserMessage = false)
                }
            }
        }

        suspend fun refreshForUserAction(): IntakeResult {
            if (SmsIntakeCapabilities.inboxScanAllowed(context)) {
                if (!SmsPermissionHelper.hasSmsPermission(context)) {
                    return IntakeResult(
                        userMessage = "Allow SMS access to read bank messages from your inbox.",
                        needsReadSmsPermission = true,
                    )
                }
                return runCatching {
                    val inbox = approvalInboxSmsScanCoordinator.scanInboxForUserAction()
                    IntakeResult(
                        userMessage = inbox.userMessage,
                        addedCount = inbox.addedCount,
                        needsReadSmsPermission = inbox.needsReadSmsPermission,
                    )
                }.getOrElse {
                    IntakeResult(
                        userMessage =
                            context.getString(R.string.approval_sms_scan_failed),
                    )
                }
            }
            if (smsBackupFolderStore.isConfigured()) {
                return runCatching {
                    val xml = smsBackupXmlImportCoordinator.importLatest(updateUserMessage = true)
                    IntakeResult(userMessage = xml.userMessage, addedCount = xml.addedCount)
                }.getOrElse {
                    IntakeResult(userMessage = context.getString(R.string.approval_sms_xml_import_failed))
                }
            }
            return IntakeResult(
                userMessage = context.getString(R.string.approval_refresh_sms_setup_required),
            )
        }

        data class IntakeResult(
            val userMessage: String?,
            val addedCount: Int = 0,
            val needsReadSmsPermission: Boolean = false,
        )
    }
