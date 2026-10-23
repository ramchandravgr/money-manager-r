package com.mmr.core.export

import android.content.Context
import com.mmr.core.database.relation.TransactionWithDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeriodExcelExporter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun exportPeriod(
            items: List<TransactionWithDetails>,
            fileName: String,
            zoneId: ZoneId,
            currencySymbol: String,
            currencyCode: String,
        ): File =
            withContext(Dispatchers.IO) {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val safeName = fileName.replace(Regex("[/\\\\?*\\[\\]:]"), "-")
                val file = File(exportDir, safeName)
                FileOutputStream(file).use { output ->
                    PeriodExcelWriter.write(
                        items = items,
                        zoneId = zoneId,
                        currencySymbol = currencySymbol,
                        currencyCode = currencyCode,
                        applicationName = context.getString(com.mmr.app.R.string.app_name),
                        sheetName = context.getString(com.mmr.app.R.string.app_name),
                        output = output,
                    )
                }
                file
            }
    }
