package com.mmr.app.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    @Inject
    lateinit var database: MmrDatabase

    @Before
    fun baseSetUp() {
        hiltRule.inject()
        runBlocking { MmrUiTestFixtures.resetUiTestDatabase(database) }
        composeRule.waitForIdle()
    }
}
