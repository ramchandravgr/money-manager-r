package com.mmr.core.export

import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.model.enums.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.util.zip.ZipInputStream

class PeriodExcelWriterTest {
    @Test
    fun write_producesXlsxZip() {
        val item =
            TransactionWithDetails(
                transaction =
                    TransactionEntity(
                        id = 1L,
                        dateTime = 1_700_000_000_000L,
                        amount = 42.5,
                        type = TransactionType.EXPENSE,
                        accountId = 1L,
                        note = "Coffee",
                    ),
                category = null,
                subcategory = null,
                account = null,
                relatedAccount = null,
                loan = null,
            )
        val bytes =
            ByteArrayOutputStream().use { output ->
                PeriodExcelWriter.write(
                    items = listOf(item),
                    zoneId = ZoneId.of("UTC"),
                    currencySymbol = "₹",
                    currencyCode = "INR",
                    applicationName = "MMR",
                    sheetName = "MMR",
                    output = output,
                )
                output.toByteArray()
            }
        ZipInputStream(bytes.inputStream()).use { zip ->
            val entry = zip.nextEntry
            assertTrue(entry != null)
            assertTrue(entry.name.startsWith("xl/"))
        }
    }
}
