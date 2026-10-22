package com.mmr.feature.stats

import com.mmr.feature.stats.model.DateRangeType
import com.mmr.feature.stats.model.StatsUiEvent
import com.mmr.feature.stats.viewmodel.StatsViewModel
import com.mmr.test.FakeTransactionRepository
import com.mmr.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun invalidCustomDateRange_setsValidationError() = runTest {
        val viewModel = StatsViewModel(FakeTransactionRepository())

        viewModel.uiState.first { !it.isLoading }

        viewModel.onEvent(StatsUiEvent.OnCustomDateRangeSet(-1L, -1L))

        val state = viewModel.uiState.first { it.errorMessage != null }
        assertEquals("Please choose a valid date range.", state.errorMessage)
    }

    @Test
    fun retryClick_clearsLoadError() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = StatsViewModel(repository)

        viewModel.uiState.first { !it.isLoading }
        viewModel.onEvent(StatsUiEvent.OnDateRangeTypeSelected(DateRangeType.THIS_MONTH))

        viewModel.onEvent(StatsUiEvent.OnRetryClick)
        val state = viewModel.uiState.first { !it.isLoading }
        assertNotNull(state.summary)
    }
}
