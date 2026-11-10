package com.rrparallelstudio.miapp.feature.home

import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.export.PeriodExcelDownloadsSaver
import com.rrparallelstudio.miapp.core.export.PeriodExcelExporter
import com.rrparallelstudio.miapp.feature.home.model.HomeUiEvent
import com.rrparallelstudio.miapp.feature.home.model.HomeViewTab
import com.rrparallelstudio.miapp.navigation.HomeLedgerTabReselectNotifier
import com.rrparallelstudio.miapp.test.FakeApprovalRepository
import com.rrparallelstudio.miapp.test.FakeCategoryRepository
import com.rrparallelstudio.miapp.test.FakeLanSyncRepository
import com.rrparallelstudio.miapp.test.FakeTransactionRepository
import com.rrparallelstudio.miapp.test.MainDispatcherRule
import com.rrparallelstudio.miapp.test.robolectricDisplaySettingsStore
import com.rrparallelstudio.miapp.ui.state.MmrUiMessages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadFailure_surfacesFriendlyErrorMessage() =
        runTest {
            val transactions =
                FakeTransactionRepository().apply {
                    approvedTransactionsError = RuntimeException()
                }
            val categories = FakeCategoryRepository(listOf(CategoryEntity(name = "Food")))
            val approvals = FakeApprovalRepository()
            val context = RuntimeEnvironment.getApplication()
            val viewModel =
                HomeViewModel(
                    context,
                    transactions,
                    categories,
                    approvals,
                    robolectricDisplaySettingsStore(),
                    PeriodExcelExporter(context, PeriodExcelDownloadsSaver(context)),
                    FakeLanSyncRepository(),
                    HomeLedgerTabReselectNotifier(),
                )

            backgroundScope.launch { viewModel.uiState.collect { } }
            val state = viewModel.uiState.first { !it.isLoading && it.errorMessage != null }
            assertEquals(MmrUiMessages.GENERIC_LOAD, state.errorMessage)
        }

    @Test
    fun retryClick_clearsLoadError() =
        runTest {
            val transactions =
                FakeTransactionRepository().apply {
                    approvedTransactionsError = RuntimeException()
                }
            val categories = FakeCategoryRepository()
            val approvals = FakeApprovalRepository()
            val context = RuntimeEnvironment.getApplication()
            val viewModel =
                HomeViewModel(
                    context,
                    transactions,
                    categories,
                    approvals,
                    robolectricDisplaySettingsStore(),
                    PeriodExcelExporter(context, PeriodExcelDownloadsSaver(context)),
                    FakeLanSyncRepository(),
                    HomeLedgerTabReselectNotifier(),
                )

            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.uiState.first { it.errorMessage != null }
            transactions.approvedTransactionsError = null
            viewModel.onEvent(HomeUiEvent.OnRetryClick)

            val afterRetry = viewModel.uiState.first { it.errorMessage == null }
            assertNull(afterRetry.errorMessage)
        }

    @Test
    fun ledgerTabReselect_resetsDailyAndCurrentMonth() =
        runTest {
            val viewModel =
                HomeViewModel(
                    RuntimeEnvironment.getApplication(),
                    FakeTransactionRepository(),
                    FakeCategoryRepository(),
                    FakeApprovalRepository(),
                    robolectricDisplaySettingsStore(),
                    PeriodExcelExporter(
                        RuntimeEnvironment.getApplication(),
                        PeriodExcelDownloadsSaver(RuntimeEnvironment.getApplication()),
                    ),
                    FakeLanSyncRepository(),
                    HomeLedgerTabReselectNotifier(),
                )
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.uiState.first { !it.isLoading }

            viewModel.onEvent(HomeUiEvent.OnViewTabSelected(HomeViewTab.TOTAL))
            viewModel.onEvent(HomeUiEvent.OnPreviousMonth)
            val beforeReselect = viewModel.uiState.value
            assertEquals(HomeViewTab.TOTAL, beforeReselect.selectedViewTab)
            assertEquals(YearMonth.now().minusMonths(1).monthValue, beforeReselect.selectedMonth)

            viewModel.onEvent(HomeUiEvent.OnLedgerTabReselected)
            val after = viewModel.uiState.value
            assertEquals(HomeViewTab.DAILY, after.selectedViewTab)
            assertEquals(YearMonth.now().monthValue, after.selectedMonth)
            assertEquals(YearMonth.now().year, after.selectedYear)
        }
}
