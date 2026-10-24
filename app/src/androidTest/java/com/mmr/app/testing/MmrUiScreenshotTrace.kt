package com.mmr.app.testing

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Saves PNG frames under the app external files dir:
 * `Android/data/com.mmr.app/files/ui-test-trace/<runId>/<test>/`
 *
 * Pull to the dev machine: `./gradlew :app:pullUiTestScreenshots`
 */
object MmrUiScreenshotTrace {
    /** Set false to skip captures (e.g. CI emulator without pull). */
    @Volatile
    var enabled: Boolean = true

    private val runId: String = System.currentTimeMillis().toString()

    private var testDir: File? = null
    private var stepIndex: Int = 0

    fun startTest(
        className: String,
        methodName: String,
    ) {
        stepIndex = 0
        if (!enabled) return
        val root = traceRootDir()
        val safeClass = className.substringAfterLast('.')
        testDir =
            File(root, "$safeClass/$methodName").apply {
                deleteRecursively()
                mkdirs()
            }
        writeManifestLine("TEST_START $className#$methodName")
    }

    fun finishTest() {
        if (!enabled) return
        writeManifestLine("TEST_END")
        testDir = null
    }

    fun capture(
        rule: ComposeTestRule,
        phase: String,
        actionLabel: String,
    ) {
        if (!enabled) return
        val dir = testDir ?: return
        rule.waitForIdle()
        val seq = stepIndex++
        val slug = slugify(actionLabel)
        val file = File(dir, "${seq.toString().padStart(4, '0')}_${phase}_$slug.png")
        val saved =
            runCatching {
                val image = rule.onRoot().captureToImage()
                val bitmap = image.asAndroidBitmap()
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                writeManifestLine("$seq $phase $actionLabel -> ${file.name}")
                true
            }.getOrDefault(false)
        if (saved) return
        val fallbackSaved =
            runCatching {
                val bitmap =
                    InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                if (bitmap == null) return@runCatching false
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                writeManifestLine("$seq $phase $actionLabel -> ${file.name} (ui_automation)")
                true
            }.getOrDefault(false)
        if (!fallbackSaved) {
            writeManifestLine("$seq $phase $actionLabel FAILED compose_and_ui_automation_capture")
        }
    }

    fun traceRootDir(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.getExternalFilesDir(null), "ui-test-trace/$runId")
    }

    private fun writeManifestLine(line: String) {
        val dir = testDir ?: return
        File(dir, "trace.log").appendText(line + "\n")
    }

    private fun slugify(raw: String): String {
        val cleaned =
            raw
                .lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
        return cleaned.take(80).ifEmpty { "action" }
    }
}
