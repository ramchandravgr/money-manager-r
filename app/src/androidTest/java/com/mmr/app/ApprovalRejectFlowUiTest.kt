package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.model.enums.SmsApprovalState
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ApprovalRejectFlowUiTest : MmrHiltUiTest() {
    @Inject
    lateinit var approvalRepository: ApprovalRepository

    @Test
    fun quickReject_removesPendingCard() {
        val merchant = "UiTest reject ${System.currentTimeMillis()}"
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedPendingApproval(
                    database = database,
                    approvalRepository = approvalRepository,
                    merchant = merchant,
                    amount = 99.0,
                )
            }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.homeTopBarApprovals).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithTag(MmrTestTags.approvalCard(fixture.approvalId))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag(MmrTestTags.approvalQuickReject(fixture.approvalId)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No pending transactions.", substring = true).assertExists()

        val pending =
            runBlocking {
                approvalRepository.getApprovalsByState(SmsApprovalState.PENDING).first()
            }
        assertEquals(0, pending.size)
    }
}
