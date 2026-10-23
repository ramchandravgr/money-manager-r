package com.mmr.feature.stats

import com.mmr.feature.stats.model.StatsPeriodMode
import com.mmr.feature.stats.model.StatsUiEvent
import com.mmr.feature.stats.viewmodel.StatsViewModel
import com.mmr.test.FakeTransactionRepository
import com.mmr.test.MainDispatcherRule
import com.mmr.test.robolectricDisplaySettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun invalidCustomStartDate_setsValidationError() =
        runTest {
            val viewModel = StatsViewModel(FakeTransactionRepository(), robolectricDisplaySettingsStore())

            viewModel.uiState.first { !it.isLoading }

            viewModel.onEvent(StatsUiEvent.OnCustomPeriodStartSelected(-1L))

            val state = viewModel.uiState.first { it.errorMessage != null }
            assertEquals("Please choose a valid start date.", state.errorMessage)
        }

    @Test
    fun retryClick_clearsLoadError() =
        runTest {
            val repository = FakeTransactionRepository()
            val viewModel = StatsViewModel(repository, robolectricDisplaySettingsStore())

            viewModel.uiState.first { !it.isLoading }
            viewModel.onEvent(StatsUiEvent.OnPeriodModeSelected(StatsPeriodMode.MONTHLY))

            viewModel.onEvent(StatsUiEvent.OnRetryClick)
            val state = viewModel.uiState.first { !it.isLoading }
            assertNotNull(state.summary)
        }
}
