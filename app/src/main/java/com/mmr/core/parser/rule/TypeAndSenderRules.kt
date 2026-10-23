package com.mmr.core.parser.rule

import com.mmr.core.model.enums.TransactionType

object TransactionTypeRule {
    private val TRANSFER_KEYWORDS =
        listOf(
            "transferred to",
            "sent to account",
            "sent via upi",
            "bank transfer",
            "transfer to",
            "neft to",
            "rtgs to",
            "imps to",
        )

    private val LOAN_KEYWORDS =
        listOf(
            "emi debited",
            "loan installment",
            "emi payment",
            "repayment debited",
            "towards nach",
            "nach-",
        )

    private val DEBIT_KEYWORDS =
        listOf(
            "debited",
            "spent",
            "paid",
            "withdrawn",
            "purchase",
            "pos txn",
            "txn of inr",
            "charged",
            "transaction of inr",
            "is spent on your",
        )

    private val CREDIT_KEYWORDS =
        listOf(
            "credited",
            "salary",
            "refund",
            "cashback",
            "deposited",
            "received",
        )

    fun inferType(normalizedBody: String): TransactionType? {
        if (LOAN_KEYWORDS.any { normalizedBody.contains(it) }) return TransactionType.LOAN_PAYMENT
        if (TRANSFER_KEYWORDS.any { normalizedBody.contains(it) }) return TransactionType.TRANSFER
        if (DEBIT_KEYWORDS.any { normalizedBody.contains(it) }) return TransactionType.EXPENSE
        if (CREDIT_KEYWORDS.any { normalizedBody.contains(it) }) return TransactionType.INCOME
        return null
    }
}

object SenderRule {
    private val KNOWN_BANKS_AND_APPS =
        listOf(
            "HDFC",
            "SBI",
            "ICICI",
            "AXIS",
            "KOTAK",
            "PNB",
            "BOB",
            "CANARA",
            "YESBNK",
            "BOIIND",
            "HSBC",
            "FEDBNK",
            "IDFC",
            "AIRBNK",
            "BOBCRD",
            "SBICRD",
            "PAYTM",
            "IPAYTM",
            "JIOPAY",
            "GPAY",
            "PHONEPE",
            "CRED",
            "AMAZON",
            "SLICE",
            "JUPITER",
            "PLUXEE",
            "HPPAY",
            "NAVAMC",
            "HDFCMF",
            "PAYTMB",
        )

    fun isKnownFinancialSender(sender: String): Boolean {
        val upper = sender.uppercase()
        return KNOWN_BANKS_AND_APPS.any { upper.contains(it) }
    }
}
