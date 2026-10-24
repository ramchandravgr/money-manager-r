package com.mmr.app.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.mmr.app.MainActivity
import com.mmr.core.database.MmrDatabase
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

    @Before
    fun baseSetUp() {
        hiltRule.inject()
        runBlocking { MmrUiTestFixtures.resetUiTestDatabase(database) }
        composeRule.waitForIdle()
        composeRule.traceAction("database_reset_idle") { }
    }

    /** Call when a test needs the main bottom nav before interacting (physical devices). */
    protected fun traceActivityRecreate(label: String = "activity_recreate") {
        composeRule.traceAction(label) {
            composeRule.activityRule.scenario.recreate()
        }
        composeRule.waitForIdle()
    }

    protected fun waitForMainNavReady() {
        composeRule.traceWaitUntil("main_nav_home_visible", timeoutMillis = 20_000) {
            runCatching {
                composeRule
                    .onAllNodesWithTag(MmrTestTags.navHome)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.waitForIdle()
    }
}
