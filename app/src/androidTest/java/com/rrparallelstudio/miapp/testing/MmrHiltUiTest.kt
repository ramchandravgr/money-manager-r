package com.rrparallelstudio.miapp.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.rrparallelstudio.miapp.BuildConfig
import com.rrparallelstudio.miapp.MainActivity
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.AppSecuritySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.FtueStore
import com.rrparallelstudio.miapp.core.data.preferences.LaunchPermissionStore
import com.rrparallelstudio.miapp.core.data.preferences.ReleaseNotesStore
import com.rrparallelstudio.miapp.core.database.MmrDatabase
import com.rrparallelstudio.miapp.security.MmrDeviceCredentialGate
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Base for full-activity Compose UI tests: Hilt + clean ledger before each test method.
 *
 * Subclasses must also declare `@HiltAndroidTest`; Hilt does not inherit it from this type.
 */
abstract class MmrHiltUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val uiTraceRule = MmrUiTestTraceRule(composeRule)

    @Inject
    lateinit var database: MmrDatabase

    @Inject
    lateinit var ftueStore: FtueStore

    @Inject
    lateinit var launchPermissionStore: LaunchPermissionStore

    @Inject
    lateinit var releaseNotesStore: ReleaseNotesStore

    @Inject
    lateinit var displaySettingsStore: AppDisplaySettingsStore

    @Inject
    lateinit var appSecuritySettingsStore: AppSecuritySettingsStore

    @Before
    fun baseSetUp() {
        hiltRule.inject()
        runBlocking { MmrUiTestFixtures.resetUiTestDatabase(database) }
        MmrUiTestShell.stabilizeLaunchPrefs(
            ftueStore = ftueStore,
            launchPermissionStore = launchPermissionStore,
            releaseNotesStore = releaseNotesStore,
            displaySettingsStore = displaySettingsStore,
            appSecuritySettingsStore = appSecuritySettingsStore,
            appVersionName = BuildConfig.VERSION_NAME,
        )
        MmrDeviceCredentialGate.bypassForInstrumentedTests = true
        traceActivityRecreate("prefs_applied_recreate")
        composeRule.traceAction("database_reset_idle") { }
    }

    /** Past splash / app lock (after credential bypass) and FTUE; waits for any main tab. */
    protected fun prepareMainShell() {
        MmrDeviceCredentialGate.bypassForInstrumentedTests = true
        ftueStore.markCompleted()
        composeRule.waitForIdle()
        MmrUiTestShell.dismissAppLockIfPresent(composeRule)
        MmrUiTestShell.dismissReleaseNotesIfPresent(composeRule)
        MmrUiTestShell.waitForAnyMainNavTab(composeRule)
        MmrUiTestShell.dismissFtueOverlayIfPresent(composeRule)
        MmrUiTestShell.dismissReleaseNotesIfPresent(composeRule)
        MmrUiTestShell.waitForAnyMainNavTab(composeRule)
    }

    /** Call when a test needs the main bottom nav before interacting (physical devices). */
    protected fun traceActivityRecreate(label: String = "activity_recreate") {
        composeRule.traceAction(label) {
            composeRule.activityRule.scenario.recreate()
        }
        composeRule.waitForIdle()
    }

    protected fun waitForMainNavReady() {
        MmrUiTestShell.waitForAnyMainNavTab(composeRule)
    }
}
