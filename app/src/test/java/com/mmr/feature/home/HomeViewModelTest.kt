package com.mmr.feature.home

import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.feature.home.model.HomeUiEvent
import com.mmr.test.FakeApprovalRepository
import com.mmr.test.FakeCategoryRepository
import com.mmr.test.FakeTransactionRepository
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
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadFailure_surfacesFriendlyErrorMessage() = runTest {
        val transactions = FakeTransactionRepository().apply {
            approvedTransactionsError = RuntimeException()
        }
        val categories = FakeCategoryRepository(listOf(CategoryEntity(name = "Food")))
        val approvals = FakeApprovalRepository()
        val viewModel = HomeViewModel(transactions, categories, approvals)

        backgroundScope.launch { viewModel.uiState.collect { } }
        val state = viewModel.uiState.first { !it.isLoading && it.errorMessage != null }
        assertEquals(MmrUiMessages.GENERIC_LOAD, state.errorMessage)
    }

    @Test
    fun retryClick_clearsLoadError() = runTest {
        val transactions = FakeTransactionRepository().apply {
            approvedTransactionsError = RuntimeException()
        }
        val categories = FakeCategoryRepository()
        val approvals = FakeApprovalRepository()
        val viewModel = HomeViewModel(transactions, categories, approvals)

        backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.uiState.first { it.errorMessage != null }
        transactions.approvedTransactionsError = null
        viewModel.onEvent(HomeUiEvent.OnRetryClick)

        val afterRetry = viewModel.uiState.first { it.errorMessage == null }
        assertNull(afterRetry.errorMessage)
    }
}
