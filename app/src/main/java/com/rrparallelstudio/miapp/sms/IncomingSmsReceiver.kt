package com.rrparallelstudio.miapp.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rrparallelstudio.miapp.core.data.preferences.SmsInboundSettingsStore
import com.rrparallelstudio.miapp.core.parser.policy.MmrSmsTelephonyPolicy

/** Not registered in the Play manifest. Re-enable with RECEIVE_SMS + [MmrSmsTelephonyPolicy]. */
class IncomingSmsReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (!MmrSmsTelephonyPolicy.INBOX_SCAN_AND_RECEIVE_ENABLED) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!SmsInboundSettingsStore.isNotifyOnNewPendingEnabled(context)) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        val sender =
            parts
                .firstOrNull()
                ?.originatingAddress
                ?.trim()
                .orEmpty()
        val body = parts.mapNotNull { it.messageBody }.joinToString(separator = "")
        val timestamp = parts.firstOrNull()?.timestampMillis ?: return
        if (sender.isEmpty() || body.isEmpty()) return

        val input =
            Data
                .Builder()
                .putString(ProcessIncomingSmsWorker.KEY_SENDER, sender)
                .putString(ProcessIncomingSmsWorker.KEY_BODY, body)
                .putLong(ProcessIncomingSmsWorker.KEY_TIMESTAMP, timestamp)
                .build()

        val request =
            OneTimeWorkRequestBuilder<ProcessIncomingSmsWorker>()
                .setInputData(input)
                .build()
        WorkManager.getInstance(context.applicationContext).enqueue(request)
    }
}
