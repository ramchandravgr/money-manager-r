package com.mmr.feature.recurring.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.domain.repository.RecurringRuleRepository
import com.mmr.core.domain.util.RecurrenceCalculator
import com.mmr.feature.recurring.model.RecurringRuleItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RecurringDetailUiState(
    val isLoading: Boolean = true,
    val rule: RecurringRuleItemUi? = null,
    val upcomingScheduleDates: List<String> = emptyList(),
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

sealed interface RecurringDetailUiEvent {
    data class OnToggleEnabled(
        val isEnabled: Boolean,
    ) : RecurringDetailUiEvent

    object OnAdvanceSchedule : RecurringDetailUiEvent

    object OnDeleteRule : RecurringDetailUiEvent

    object OnRetryClick : RecurringDetailUiEvent

    object OnDismissStatus : RecurringDetailUiEvent
}

@HiltViewModel
class RecurringDetailViewModel
    @Inject
    constructor(
        private val recurringRuleRepository: RecurringRuleRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val ruleId: Long = checkNotNull(savedStateHandle["ruleId"])

        private val _uiState = MutableStateFlow(RecurringDetailUiState(isLoading = true))
        val uiState: StateFlow<RecurringDetailUiState> = _uiState.asStateFlow()

        init {
            loadRuleDetails()
        }

        private fun loadRuleDetails() {
            viewModelScope.launch {
                recurringRuleRepository
                    .getRuleWithDetailsById(ruleId)
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD),
                            )
                        }
                    }.collect { withDetails ->
                        if (withDetails == null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Recurring rule not found.",
                                )
                            }
                        } else {
                            val itemUi = withDetails.toItemUi()
                            val nextDates =
                                computeUpcomingOccurrences(
                                    startFrom = withDetails.rule.nextRunDate,
                                    interval = withDetails.rule.recurrenceInterval,
                                    dayOfMonth = withDetails.rule.dayOfMonth,
                                    count = 5,
                                )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    rule = itemUi,
                                    upcomingScheduleDates = nextDates,
                                    errorMessage = null,
                                )
                            }
                        }
                    }
            }
        }

        fun onEvent(event: RecurringDetailUiEvent) {
            when (event) {
                is RecurringDetailUiEvent.OnToggleEnabled -> {
                    viewModelScope.launch {
                        try {
                            recurringRuleRepository.setRuleEnabled(ruleId, event.isEnabled)
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(statusMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                            }
                        }
                    }
                }

                RecurringDetailUiEvent.OnAdvanceSchedule -> {
                    viewModelScope.launch {
                        try {
                            recurringRuleRepository.advanceNextRunDate(ruleId)
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(statusMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                            }
                        }
                    }
                }

                RecurringDetailUiEvent.OnDeleteRule -> {
                    viewModelScope.launch {
                        try {
                            val rule = recurringRuleRepository.getRuleById(ruleId)
                            if (rule != null) {
                                recurringRuleRepository.deleteRecurringRule(rule)
                            } else {
                                _uiState.update { it.copy(statusMessage = "Recurring rule not found.") }
                            }
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(statusMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                            }
                        }
                    }
                }

                RecurringDetailUiEvent.OnRetryClick -> {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null, statusMessage = null) }
                    loadRuleDetails()
                }

                RecurringDetailUiEvent.OnDismissStatus -> {
                    _uiState.update { it.copy(statusMessage = null) }
                }
            }
        }

        private fun computeUpcomingOccurrences(
            startFrom: Long,
            interval: com.mmr.core.model.enums.RecurrenceInterval,
            dayOfMonth: Int?,
            count: Int,
        ): List<String> {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val occurrences = mutableListOf<String>()
            var current = startFrom
            for (i in 0 until count) {
                occurrences.add(dateFormat.format(Date(current)))
                current = RecurrenceCalculator.calculateNextRunDate(current, interval, dayOfMonth)
            }
            return occurrences
        }
    }
