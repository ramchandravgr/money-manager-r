package com.mmr.feature.loan

import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.feature.loan.model.LoanListUiEvent
import com.mmr.feature.loan.viewmodel.LoanListViewModel
import com.mmr.test.FakeLoanRepository
import com.mmr.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoanListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadFailure_setsFriendlyErrorMessage() =
        runTest {
            val repository =
                FakeLoanRepository().apply {
                    allLoansError = RuntimeException()
                }
            val viewModel = LoanListViewModel(repository)

            backgroundScope.launch { viewModel.uiState.collect { } }
            val errorState = viewModel.uiState.first { it.errorMessage != null }
            assertEquals(MmrUiMessages.GENERIC_LOAD, errorState.errorMessage)
        }

    @Test
    fun retryClick_clearsLoadError() =
        runTest {
            val repository =
                FakeLoanRepository().apply {
                    allLoansError = RuntimeException()
                }
            val viewModel = LoanListViewModel(repository)

            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.uiState.first { it.errorMessage != null }

            repository.allLoansError = null
            viewModel.onEvent(LoanListUiEvent.OnRetryClick)

            val cleared = viewModel.uiState.first { it.errorMessage == null }
            assertNull(cleared.errorMessage)
        }
}
