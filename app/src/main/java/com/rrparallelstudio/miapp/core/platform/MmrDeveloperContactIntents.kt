package com.rrparallelstudio.miapp.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.rrparallelstudio.miapp.R

object MmrDeveloperContactIntents {
    const val SUPPORT_EMAIL = "rrparallelstudio@gmail.com"

    fun migrationImportRequestMailtoUri(
        context: Context,
        email: String = SUPPORT_EMAIL,
    ): Uri {
        val subject = context.getString(R.string.backup_migration_email_subject)
        val body = context.getString(R.string.backup_migration_email_body)
        return buildMailtoUri(
            email = email,
            subject = subject,
            body = body,
        )
    }

    fun openMigrationImportRequest(
        context: Context,
        email: String = SUPPORT_EMAIL,
    ) {
        val uri = migrationImportRequestMailtoUri(context, email)
        val intent =
            Intent(Intent.ACTION_SENDTO, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.backup_migration_no_email_app, email),
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    internal fun buildMailtoUri(
        email: String,
        subject: String,
        body: String,
    ): Uri =
        Uri
            .parse("mailto:$email")
            .buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()
}
