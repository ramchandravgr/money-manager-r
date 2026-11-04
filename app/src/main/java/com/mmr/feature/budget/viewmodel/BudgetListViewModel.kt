package com.mmr.feature.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.domain.repository.BudgetRepository
import com.mmr.feature.budget.model.BudgetListItemUi
import com.mmr.feature.budget.model.BudgetListUiEvent
import com.mmr.feature.budget.model.BudgetListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetListViewModel
    @Inject
    constructor(
        private val budgetRepository: BudgetRepository,
    ) : ViewModel() {
        private val loadErrorState = MutableStateFlow<String?>(null)

        val uiState: StateFlow<BudgetListUiState> =
            combine(
                budgetRepository.observeActiveBudgetProgress().catch { e ->
                    loadErrorState.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                    emit(emptyList())
                },
                loadErrorState,
            ) { snapshots, loadError ->
                val items =
                    snapshots.map { snap ->
                        BudgetListItemUi(
                            budgetId = snap.budgetId,
                            targetLabel = snap.targetLabel,
                            periodLabel = snap.periodLabel,
                            periodType = snap.periodType,
                            limitFormatted = MmrMoneyFormat.formatInr(snap.limitAmount),
                            spentFormatted = MmrMoneyFormat.formatInr(snap.spentAmount),
                            percentUsed = snap.percentUsed,
                            progressFraction = snap.progressFraction,
                            isOverBudget = snap.isOverBudget,
                            alertOnExceeded = snap.alertOnExceeded,
                        )
                    }
                BudgetListUiState(
                    isLoading = false,
                    budgets = items,
                    isEmpty = loadError == null && items.isEmpty(),
                    errorMessage = loadError,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = BudgetListUiState(isLoading = true),
            )

        fun onEvent(event: BudgetListUiEvent) {
            when (event) {
                BudgetListUiEvent.OnRetryClick -> loadErrorState.value = null
                is BudgetListUiEvent.OnDeleteBudget -> {
                    viewModelScope.launch {
                        try {
                            budgetRepository.delete(event.budgetId)
                        } catch (e: Exception) {
                            loadErrorState.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                        }
                    }
                }
            }
        }
    }
