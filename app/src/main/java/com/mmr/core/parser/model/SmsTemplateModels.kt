package com.mmr.core.parser.model

import com.mmr.core.model.enums.TransactionType
import java.util.regex.Pattern

enum class SmsPaymentChannel {
    UPI,
    NEFT,
    CARD,
    NACH,
    WALLET,
    NET_BANKING,
    UNKNOWN,
}

enum class SmsInstrument {
    SAVINGS,
    CREDIT_CARD,
    MEAL_WALLET,
    FUEL_WALLET,
    UNKNOWN,
}

enum class SmsParseIntent {
    SETTLED,
    MANDATE,
    STATEMENT,
    INVESTMENT,

    /** Credit card bill payment (already counted when spent on the card). */
    CARD_PAYMENT,

    /** Merchant refund / reversal credited to the card — apply to an existing card spend. */
    CARD_REFUND,
}

data class SmsTemplate(
    val id: String,
    val senderSuffixes: List<String>,
    val priority: Int,
    val bodyPattern: Pattern,
    val transactionType: TransactionType,
    val paymentChannel: SmsPaymentChannel?,
    val instrument: SmsInstrument?,
    val intent: SmsParseIntent,
    val amountGroup: String = "amount",
    val merchantGroup: String? = "merchant",
    val last4Group: String? = "last4",
    val refGroup: String? = "ref",
    val balanceGroup: String? = "balance",
)

data class SmsTemplateMatch(
    val template: SmsTemplate,
    val amount: Double,
    val merchant: String?,
    val accountLastDigits: String?,
    val referenceNumber: String?,
    val balanceAfter: Double?,
)
