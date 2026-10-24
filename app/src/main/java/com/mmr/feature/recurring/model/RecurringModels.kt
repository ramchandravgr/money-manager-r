package com.mmr.feature.recurring.model

import com.mmr.core.model.enums.RecurrenceInterval
import com.mmr.core.model.enums.TransactionType

enum class RecurringFilterType {
    ALL,
    ACTIVE,
    PAUSED,
}

data class RecurringRuleItemUi(
    val id: Long,
    val name: String,
    val amount: Double,
    val amountFormatted: String,
    val type: TransactionType,
    val isExpense: Boolean,
    val interval: RecurrenceInterval,
    val intervalLabel: String,
    val categoryName: String,
    val accountName: String,
    val nextRunDate: Long,
    val nextRunDateFormatted: String,
    val isEnabled: Boolean,
    val reminderMinutesBefore: Int,
    val reminderLabel: String,
    val noteTemplate: String?,
)

data class UpcomingReminderUi(
    val notificationId: Long,
    val ruleId: Long?,
    val title: String,
    val message: String,
    val scheduleTime: Long,
    val formattedScheduleDate: String,
    val amountFormatted: String?,
)

data class RecurringListUiState(
    val isLoading: Boolean = true,
    val rules: List<RecurringRuleItemUi> = emptyList(),
    val reminders: List<UpcomingReminderUi> = emptyList(),
    val totalRulesCount: Int = 0,
    val activeRulesCount: Int = 0,
    val pausedRulesCount: Int = 0,
    val selectedFilter: RecurringFilterType = RecurringFilterType.ALL,
    val isEmpty: Boolean = false,
    val isFilteredEmpty: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

sealed interface RecurringListUiEvent {
    data class OnFilterSelected(
        val filter: RecurringFilterType,
    ) : RecurringListUiEvent

    data class OnToggleRuleEnabled(
        val ruleId: Long,
        val isEnabled: Boolean,
    ) : RecurringListUiEvent

    data class OnDeleteRule(
        val ruleId: Long,
    ) : RecurringListUiEvent

    data class OnAdvanceNextRunDate(
        val ruleId: Long,
    ) : RecurringListUiEvent

    object OnRetryClick : RecurringListUiEvent

    object OnDismissStatus : RecurringListUiEvent

    object OnResetFilters : RecurringListUiEvent
}

data class AddEditRecurringRuleUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val ruleId: Long? = null,
    val name: String = "",
    val amount: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categories: List<com.mmr.feature.transaction.model.CategoryUi> = emptyList(),
    val accounts: List<com.mmr.feature.transaction.model.AccountUi> = emptyList(),
    val loans: List<com.mmr.feature.transaction.model.LoanPickerUi> = emptyList(),
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val accountId: Long? = null,
    val relatedAccountId: Long? = null,
    val transferLoanId: Long? = null,
    val interval: RecurrenceInterval = RecurrenceInterval.MONTHLY,
    val dayOfMonth: Int? = null,
    val startDate: Long = System.currentTimeMillis(),
    val reminderMinutesBefore: Int = 1440,
    val notes: String = "",
    val errorMessage: String? = null,
    val canSave: Boolean = false,
    val categoryPickerVisible: Boolean = false,
    val pickerHighlightedCategoryId: Long? = null,
    val accountPickerVisible: Boolean = false,
    val accountPickerTarget: com.mmr.feature.transaction.model.AccountPickerTarget =
        com.mmr.feature.transaction.model.AccountPickerTarget.SINGLE_ACCOUNT,
    val transferToPickerVisible: Boolean = false,
    val reminderPickerVisible: Boolean = false,
)

sealed interface AddEditRecurringRuleUiEvent {
    data class OnNameChanged(
        val name: String,
    ) : AddEditRecurringRuleUiEvent

    data class OnAmountChanged(
        val amount: String,
    ) : AddEditRecurringRuleUiEvent

    data class OnTypeSelected(
        val type: TransactionType,
    ) : AddEditRecurringRuleUiEvent

    data class OnCategorySelected(
        val categoryId: Long?,
        val subcategoryId: Long?,
    ) : AddEditRecurringRuleUiEvent

    data class OnPickerParentSelected(
        val categoryId: Long,
    ) : AddEditRecurringRuleUiEvent

    data class OnPickerSubcategorySelected(
        val subcategoryId: Long,
    ) : AddEditRecurringRuleUiEvent

    object OnPickerConfirmParentOnly : AddEditRecurringRuleUiEvent

    data class OnAccountSelected(
        val accountId: Long,
    ) : AddEditRecurringRuleUiEvent

    data class OnTransferToAccountSelected(
        val accountId: Long,
    ) : AddEditRecurringRuleUiEvent

    data class OnTransferToLoanSelected(
        val loanId: Long,
    ) : AddEditRecurringRuleUiEvent

    data class OnIntervalSelected(
        val interval: RecurrenceInterval,
    ) : AddEditRecurringRuleUiEvent

    data class OnDayOfMonthChanged(
        val day: Int?,
    ) : AddEditRecurringRuleUiEvent

    data class OnStartDateChanged(
        val date: Long,
    ) : AddEditRecurringRuleUiEvent

    data class OnReminderMinutesChanged(
        val minutes: Int,
    ) : AddEditRecurringRuleUiEvent

    data class OnNotesChanged(
        val notes: String,
    ) : AddEditRecurringRuleUiEvent

    object OnCategoryRowClick : AddEditRecurringRuleUiEvent

    object OnDismissCategoryPicker : AddEditRecurringRuleUiEvent

    object OnAccountRowClick : AddEditRecurringRuleUiEvent

    object OnDismissAccountPicker : AddEditRecurringRuleUiEvent

    object OnTransferFromRowClick : AddEditRecurringRuleUiEvent

    object OnTransferToRowClick : AddEditRecurringRuleUiEvent

    object OnDismissTransferToPicker : AddEditRecurringRuleUiEvent

    object OnReminderRowClick : AddEditRecurringRuleUiEvent

    object OnDismissReminderPicker : AddEditRecurringRuleUiEvent

    object OnSaveClick : AddEditRecurringRuleUiEvent
}
