package com.mmr.core.database.converter

import androidx.room.TypeConverter
import com.mmr.core.model.enums.*

class EnumConverters {
    @TypeConverter fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter fun toTransactionType(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }

    @TypeConverter fun fromTransactionSourceType(value: TransactionSourceType?): String? = value?.name

    @TypeConverter fun toTransactionSourceType(value: String?): TransactionSourceType? =
        value?.let {
            TransactionSourceType.valueOf(it)
        }

    @TypeConverter fun fromRecordStatus(value: RecordStatus?): String? = value?.name

    @TypeConverter fun toRecordStatus(value: String?): RecordStatus? = value?.let { RecordStatus.valueOf(it) }

    @TypeConverter fun fromSmsApprovalState(value: SmsApprovalState?): String? = value?.name

    @TypeConverter fun toSmsApprovalState(value: String?): SmsApprovalState? =
        value?.let {
            SmsApprovalState.valueOf(
                it,
            )
        }

    @TypeConverter fun fromSmsParsedStatus(value: SmsParsedStatus?): String? = value?.name

    @TypeConverter fun toSmsParsedStatus(value: String?): SmsParsedStatus? = value?.let { SmsParsedStatus.valueOf(it) }

    @TypeConverter fun fromCategoryType(value: CategoryType?): String? = value?.name

    @TypeConverter fun toCategoryType(value: String?): CategoryType? = value?.let { CategoryType.valueOf(it) }

    @TypeConverter fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter fun toAccountType(value: String?): AccountType? = value?.let { AccountType.valueOf(it) }

    @TypeConverter fun fromLoanStatus(value: LoanStatus?): String? = value?.name

    @TypeConverter fun toLoanStatus(value: String?): LoanStatus? = value?.let { LoanStatus.valueOf(it) }

    @TypeConverter fun fromLoanType(value: LoanType?): String? = value?.name

    @TypeConverter fun toLoanType(value: String?): LoanType? = value?.let { LoanType.valueOf(it) }

    @TypeConverter fun fromLoanPaymentType(value: LoanPaymentType?): String? = value?.name

    @TypeConverter fun toLoanPaymentType(value: String?): LoanPaymentType? = value?.let { LoanPaymentType.valueOf(it) }

    @TypeConverter fun fromRecurrenceInterval(value: RecurrenceInterval?): String? = value?.name

    @TypeConverter fun toRecurrenceInterval(value: String?): RecurrenceInterval? =
        value?.let {
            RecurrenceInterval.valueOf(it)
        }

    @TypeConverter fun fromNotificationRuleType(value: NotificationRuleType?): String? = value?.name

    @TypeConverter fun toNotificationRuleType(value: String?): NotificationRuleType? =
        value?.let {
            NotificationRuleType.valueOf(it)
        }

    @TypeConverter fun fromLoanScheduleStatus(value: LoanScheduleStatus?): String? = value?.name

    @TypeConverter fun toLoanScheduleStatus(value: String?): LoanScheduleStatus? =
        value?.let {
            LoanScheduleStatus
                .valueOf(
                    it,
                )
        }

    @TypeConverter fun fromCategorizationMatchType(value: CategorizationMatchType?): String? = value?.name

    @TypeConverter fun toCategorizationMatchType(value: String?): CategorizationMatchType? =
        value?.let { CategorizationMatchType.valueOf(it) }

    @TypeConverter fun fromCardNetwork(value: CardNetwork?): String? = value?.name

    @TypeConverter fun toCardNetwork(value: String?): CardNetwork? = value?.let { CardNetwork.valueOf(it) }
}
