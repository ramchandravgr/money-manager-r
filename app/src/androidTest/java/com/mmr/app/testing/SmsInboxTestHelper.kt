package com.mmr.app.testing

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony

/**
 * Optional **emulator-only** inbox seeding (`content://sms/inbox`). Primary SMS UI coverage uses the
 * real device inbox via [com.mmr.app.ApprovalInboxScanUiTest], not this helper.
 */
object SmsInboxTestHelper {
    fun insertInboxMessage(
        context: Context,
        address: String,
        body: String,
        dateMillis: Long = System.currentTimeMillis(),
    ): Uri? {
        val values =
            ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, dateMillis)
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            }
        return context.contentResolver.insert(Uri.parse("content://sms/inbox"), values)
    }
}
