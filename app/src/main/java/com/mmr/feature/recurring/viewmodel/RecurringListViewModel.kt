package com.mmr.feature.recurring.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.database.relation.RecurringRuleWithDetails
import com.mmr.core.domain.repository.RecurringRuleRepository
import com.mmr.core.model.enums.RecurrenceInterval
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.recurring.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val recurringRuleRepository: RecurringRuleRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(RecurringFilterType.ALL)
    private val _loadErrorMessage = MutableStateFlow<String?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)

    private val recurringListData = combine(
        recurringRuleRepository.getAllRulesWithDetails()
            .catch { e ->
                _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                emit(emptyList())
            },
        recurringRuleRepository.getUpcomingReminders()
            .catch { emit(emptyList()) },
        recurringRuleRepository.getEnabledRulesCount()
            .catch { emit(0) },
        _selectedFilter
    ) { allRules, upcomingReminders, enabledCount, filter ->
        RecurringListData(allRules, upcomingReminders, enabledCount, filter)
    }

    val uiState: StateFlow<RecurringListUiState> = combine(
        recurringListData,
        _loadErrorMessage,
        _statusMessage
    ) { data, loadError, status ->
        val allRules = data.allRules
        val upcomingReminders = data.upcomingReminders
        val enabledCount = data.enabledCount
        val filter = data.filter
        val filtered = allRules.filter { withDetails ->
            when (filter) {
                RecurringFilterType.ALL -> true
                RecurringFilterType.ACTIVE -> withDetails.rule.isEnabled
                RecurringFilterType.PAUSED -> !withDetails.rule.isEnabled
            }
        }

        val ruleUis = filtered.map { it.toItemUi() }
        val reminderUis = upcomingReminders.map {
            UpcomingReminderUi(
                notificationId = it.notificationId,
                ruleId = it.ruleId,
                title = it.title,
                message = it.message,
                scheduleTime = it.scheduleTime,
                formattedScheduleDate = it.formattedScheduleDate,
                amountFormatted = it.amountFormatted
            )
        }

        RecurringListUiState(
            isLoading = false,
            rules = ruleUis,
            reminders = reminderUis,
            activeRulesCount = enabledCount,
            selectedFilter = filter,
            isEmpty = loadError == null && ruleUis.isEmpty(),
            errorMessage = loadError,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringListUiState(isLoading = true)
    )

    fun onEvent(event: RecurringListUiEvent) {
        when (event) {
            is RecurringListUiEvent.OnFilterSelected -> {
                _selectedFilter.value = event.filter
            }

            is RecurringListUiEvent.OnToggleRuleEnabled -> {
                viewModelScope.launch {
                    try {
                        recurringRuleRepository.setRuleEnabled(event.ruleId, event.isEnabled)
                    } catch (e: Exception) {
                        _statusMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                    }
                }
            }

            is RecurringListUiEvent.OnDeleteRule -> {
                viewModelScope.launch {
                    try {
                        val rule = recurringRuleRepository.getRuleById(event.ruleId)
                        if (rule != null) {
                            recurringRuleRepository.deleteRecurringRule(rule)
                        } else {
                            _statusMessage.value = "Recurring rule not found."
                        }
                    } catch (e: Exception) {
                        _statusMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                    }
                }
            }

            is RecurringListUiEvent.OnAdvanceNextRunDate -> {
                viewModelScope.launch {
                    try {
                        recurringRuleRepository.advanceNextRunDate(event.ruleId)
                    } catch (e: Exception) {
                        _statusMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                    }
                }
            }

            RecurringListUiEvent.OnRetryClick -> {
                _loadErrorMessage.value = null
                _statusMessage.value = null
            }

            RecurringListUiEvent.OnDismissStatus -> {
                _statusMessage.value = null
            }
        }
    }
}

private data class RecurringListData(
    val allRules: List<RecurringRuleWithDetails>,
    val upcomingReminders: List<com.mmr.core.database.model.UpcomingReminder>,
    val enabledCount: Int,
    val filter: RecurringFilterType
)

internal fun RecurringRuleWithDetails.toItemUi(): RecurringRuleItemUi {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val isExpense = rule.transactionTemplateType == TransactionType.EXPENSE
    val sign = if (isExpense) "-" else "+"

    val intervalLabel = when (rule.recurrenceInterval) {
        RecurrenceInterval.DAILY -> "Daily"
        RecurrenceInterval.WEEKLY -> "Weekly"
        RecurrenceInterval.MONTHLY -> rule.dayOfMonth?.let { "Monthly (on day $it)" } ?: "Monthly"
        RecurrenceInterval.YEARLY -> "Yearly"
        RecurrenceInterval.CUSTOM -> "Custom"
    }

    val reminderLabel = when (rule.reminderMinutesBefore) {
        0 -> "No reminder"
        60 -> "1 hour before"
        1440 -> "1 day before"
        2880 -> "2 days before"
        else -> "${rule.reminderMinutesBefore / 60} hours before"
    }

    return RecurringRuleItemUi(
        id = rule.id,
        name = rule.name,
        amount = rule.amount,
        amountFormatted = "$sign₹${"%.2f".format(rule.amount)}",
        type = rule.transactionTemplateType,
        isExpense = isExpense,
        interval = rule.recurrenceInterval,
        intervalLabel = intervalLabel,
        categoryName = category?.name ?: "Uncategorized",
        accountName = account?.name ?: "Account",
        nextRunDate = rule.nextRunDate,
        nextRunDateFormatted = dateFormat.format(Date(rule.nextRunDate)),
        isEnabled = rule.isEnabled,
        reminderMinutesBefore = rule.reminderMinutesBefore,
        reminderLabel = reminderLabel,
        noteTemplate = rule.noteTemplate
    )
}
