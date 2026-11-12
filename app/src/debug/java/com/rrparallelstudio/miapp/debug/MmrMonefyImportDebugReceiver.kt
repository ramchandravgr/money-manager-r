package com.rrparallelstudio.miapp.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rrparallelstudio.miapp.core.monefy.MonefyBackupCrypto
import com.rrparallelstudio.miapp.core.monefy.MonefyBackupReader
import com.rrparallelstudio.miapp.core.monefy.MonefyBackupFormat
import com.rrparallelstudio.miapp.di.MonefyImportDebugEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Debug-only: probe or import a Monefy backup file by absolute path (adb broadcast).
 *
 * `adb shell am broadcast -a com.rrparallelstudio.miapp.action.MONEYFY_IMPORT_PROBE -n com.rrparallelstudio.miapp/.debug.MmrMonefyImportDebugReceiver --es path /sdcard/Download/monefy_backup_2026_11_12_07_09_25`
 */
class MmrMonefyImportDebugReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        when (intent?.action) {
            ACTION_PROBE -> probe(context, intent)
            ACTION_IMPORT -> importIntoLedger(context, intent)
            else -> return
        }
    }

    private fun probe(
        context: Context,
        intent: Intent,
    ) {
        val path = intent.requirePath()
        Log.i(TAG, "probe start path=$path exists=${File(path).exists()}")
        val bytes =
            runCatching { File(path).readBytes() }.getOrElse { error ->
                val cacheCopy = File(context.cacheDir, "monefy_backup_dev")
                Log.w(TAG, "probe: could not read $path (${error.message}); trying cacheDir copy")
                cacheCopy.readBytes()
            }
        Log.i(TAG, "probe path=$path bytes=${bytes.size} nameHint=${File(path).name}")
        if (!MonefyBackupFormat.isProbablyMonefyBackup(bytes, File(path).name)) {
            Log.e(TAG, "probe: isProbablyMonefyBackup=false (size mod16=${bytes.size % 16 == 0})")
        }
        try {
            MonefyBackupCrypto.decrypt(bytes)
            val snapshot = MonefyBackupReader.read(bytes, context.cacheDir)
            Log.i(
                TAG,
                "probe OK accounts=${snapshot.accounts.size} categories=${snapshot.categories.size} " +
                    "txns=${snapshot.transactions.size} transfers=${snapshot.transfers.size}",
            )
        } catch (e: Exception) {
            Log.e(TAG, "probe FAILED: ${e.message}", e)
        }
    }

    private fun importIntoLedger(
        context: Context,
        intent: Intent,
    ) {
        val path = intent.requirePath()
        val bytes = File(path).readBytes()
        val entry =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                MonefyImportDebugEntryPoint::class.java,
            )
        runBlocking {
            try {
                val result = entry.monefyLedgerImporter().importFromBytes(bytes, context.cacheDir)
                Log.i(
                    TAG,
                    "import OK txnsAdded=${result.transactionsAdded} transfers=${result.transfersAdded} " +
                        "accountsCreated=${result.accountsCreated}",
                )
            } catch (e: Exception) {
                Log.e(TAG, "import FAILED: ${e.message}", e)
            }
        }
    }

    private fun Intent.requirePath(): String {
        val path = getStringExtra(EXTRA_PATH)?.trim().orEmpty()
        require(path.isNotEmpty()) { "Missing string extra path=" }
        return path
    }

    companion object {
        private const val TAG = "MmrMonefyImportDebug"
        const val ACTION_PROBE = "com.rrparallelstudio.miapp.action.MONEYFY_IMPORT_PROBE"
        const val ACTION_IMPORT = "com.rrparallelstudio.miapp.action.MONEYFY_IMPORT_RUN"
        const val EXTRA_PATH = "path"
    }
}
