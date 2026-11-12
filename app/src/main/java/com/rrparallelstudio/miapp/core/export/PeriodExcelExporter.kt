package com.rrparallelstudio.miapp.core.export

import android.content.Context
import com.rrparallelstudio.miapp.core.database.relation.TransactionWithDetails
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
        private val downloadsSaver: PeriodExcelDownloadsSaver,
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
                        applicationName = context.getString(com.rrparallelstudio.miapp.R.string.app_name),
                        sheetName = context.getString(com.rrparallelstudio.miapp.R.string.app_name),
                        output = output,
                    )
                }
                file
            }

        suspend fun exportPeriodToDownloads(
            items: List<TransactionWithDetails>,
            fileName: String,
            zoneId: ZoneId,
            currencySymbol: String,
            currencyCode: String,
        ): PeriodExcelDownloadsExport {
            val cacheFile =
                exportPeriod(
                    items = items,
                    fileName = fileName,
                    zoneId = zoneId,
                    currencySymbol = currencySymbol,
                    currencyCode = currencyCode,
                )
            val xmlName = fileName.replace(Regex("(?i)\\.xlsx$"), ".xml")
            val xmlFile = File(cacheFile.parentFile, xmlName)
            FileOutputStream(xmlFile).use { xmlOut ->
                PeriodXmlWriter.write(
                    items = items,
                    zoneId = zoneId,
                    currencyCode = currencyCode,
                    applicationName = context.getString(com.rrparallelstudio.miapp.R.string.app_name),
                    output = xmlOut,
                )
            }
            val savedName = downloadsSaver.saveToDownloads(cacheFile, fileName)
            return PeriodExcelDownloadsExport(
                savedFileName = savedName,
                cacheXlsxFile = cacheFile,
                cacheXmlFile = xmlFile,
            )
        }

        suspend fun exportImportTemplateToDownloads(
            currencySymbol: String,
            currencyCode: String,
        ): String =
            withContext(Dispatchers.IO) {
                val appName = context.getString(com.rrparallelstudio.miapp.R.string.app_name)
                val fileName = "${appName}_import_template.xlsx"
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(exportDir, fileName)
                FileOutputStream(file).use { output ->
                    PeriodExcelWriter.writeImportTemplate(
                        currencySymbol = currencySymbol,
                        currencyCode = currencyCode,
                        applicationName = appName,
                        sheetName = appName,
                        output = output,
                    )
                }
                downloadsSaver.saveToDownloads(file, fileName)
            }
    }

data class PeriodExcelDownloadsExport(
    val savedFileName: String,
    val cacheXlsxFile: File,
    val cacheXmlFile: File,
)
