package com.rrparallelstudio.miapp.core.monefy

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.data.repository.MmrInstrumentedRepositoryGraph
import com.rrparallelstudio.miapp.core.database.MmrDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device verification of the dev Monefy sample (`docs/samples`, androidTest assets).
 * Does not use Hilt (same pattern as [com.rrparallelstudio.miapp.core.backup.BackupRestoreInstrumentedTest]).
 */
@RunWith(AndroidJUnit4::class)
class MonefyDevSampleImportInstrumentedTest {
    companion object {
        private const val MONEYFY_DEV_SAMPLE = "monefy_backup_2026_11_12_07_09_25"
    }

    private lateinit var database: MmrDatabase
    private lateinit var repoGraph: MmrInstrumentedRepositoryGraph

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repoGraph = MmrInstrumentedRepositoryGraph(context)
        database = repoGraph.database
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun devSample_decryptAndImportOnDevice() {
        val bytes = loadDevSampleBytes()
        assertTrue(MonefyBackupFormat.isProbablyMonefyBackup(bytes, MONEYFY_DEV_SAMPLE))
        val snapshot = MonefyBackupReader.read(bytes, File.createTempFile("monefy", ".db").parentFile!!)
        assertTrue(snapshot.accounts.isNotEmpty())
        assertTrue(snapshot.transactions.isNotEmpty())

        val result =
            runBlocking {
                repoGraph.monefyLedgerImporter.importFromBytes(
                    bytes,
                    ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir,
                )
            }
        assertTrue(
            "Expected transactions or transfers",
            result.transactionsAdded + result.transfersAdded > 0,
        )
        runBlocking {
            val count =
                database
                    .transactionDao()
                    .getApprovedTransactionsDetailed()
                    .first()
                    .size
            assertTrue("Room has imported rows", count > 0)
        }
    }

    @Test
    fun devSample_contentResolverRoundTrip_matchesPickerRead() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val raw = loadDevSampleBytes()
        val cacheFile =
            File(context.cacheDir, "exports/$MONEYFY_DEV_SAMPLE").also {
                it.parentFile?.mkdirs()
                it.writeBytes(raw)
            }
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.exportfileprovider",
                cacheFile,
            )
        context.grantUriPermission(context.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val fromResolver =
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("contentResolver failed to open $uri")
        assertArrayEquals("Picker path must read full encrypted blob", raw, fromResolver)

        val result =
            runBlocking {
                repoGraph.monefyLedgerImporter.importFromBytes(
                    fromResolver,
                    context.cacheDir,
                )
            }
        assertTrue(result.transactionsAdded + result.transfersAdded > 0)
    }

    private fun loadDevSampleBytes(): ByteArray =
        ApplicationProvider
            .getApplicationContext<android.content.Context>()
            .assets
            .open(MONEYFY_DEV_SAMPLE)
            .use { it.readBytes() }
}
