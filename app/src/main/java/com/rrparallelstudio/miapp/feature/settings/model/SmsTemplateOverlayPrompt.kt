package com.rrparallelstudio.miapp.feature.settings.model

/**
 * Instructions a person pastes into any AI, together with their own SMS export,
 * to produce an overlay JSON file Money Inbox can import.
 */
object SmsTemplateOverlayPrompt {
    const val CLIPBOARD_LABEL = "Money Inbox SMS template prompt"

    val text: String =
        """
        You are writing a JSON file of SMS parsing rules for the Android app Money Inbox. I will attach or paste an SMS export after this prompt (SMS Backup & Restore XML, or copied bank messages). Use only those messages. Follow this schema exactly so the app can import the file.

        What to build
        - Find messages that are real money movement: spends, UPI, credits, card spends, ATM, transfers, merchant refunds.
        - Group repeated shapes. Write one template per shape, not one per message.
        - Prefer fewer broader patterns. At most 80 templates. The app rejects a file with more than 120.
        - Skip OTP, login codes, promos, and statement-only notices.

        How the app matches
        - The message is lowercased. The rupee symbol and a standalone Rs. or Rs: become "inr " (the word "reversed" is left alone). Line breaks become single spaces. Write bodyRegex for that text.
        - Regex is Java (named groups like (?<amount>…)), case-insensitive. Do not use Python (?P<amount>…).
        - Sender: the app uppercases the address and drops everything before the first hyphen. VM-HDFCBK-S becomes HDFCBK-S. A senderSuffix matches when it equals that key, or the key ends with it, or the key contains it. HDFCBK matches HDFCBK-S.
        - A template is used only when the amount group is a number greater than 0.

        JSON shape
        {
          "version": 1,
          "templates": []
        }

        Each template:
        - id (required): unique snake_case, for example hdfc_upi_sent_custom. The same id as a built-in rule replaces that built-in rule. Duplicate ids in this file are rejected.
        - senderSuffixes (required): uppercase strings, for example ["HDFCBK","HDFCBK-S"]. An empty array matches every sender; avoid that.
        - priority (optional, default 50): higher runs first. Use 70 to 110 for one bank.
        - bodyRegex (required): max 4000 characters. In the JSON file a regex backslash is written twice, so whitespace is \\s and a dot is \\.
        - transactionType (required): EXPENSE, INCOME, or TRANSFER.
        - paymentChannel (optional): UPI, NEFT, CARD, NACH, WALLET, NET_BANKING, UNKNOWN.
        - instrument (optional): SAVINGS, CREDIT_CARD, MEAL_WALLET, FUEL_WALLET, UNKNOWN.
        - intent (optional, default SETTLED): SETTLED offers a transaction to approve. CARD_PAYMENT is paying a credit-card bill (not a new spend). CARD_REFUND is a merchant refund onto the card. MANDATE, STATEMENT, and INVESTMENT are notices, not new spends.
        - amountGroup (optional, default "amount"): the regex must contain that named group.
        - merchantGroup (optional, default "merchant"): set to "" when there is no payee.
        - last4Group (optional, default "last4"): account or card last 4. Set to "" when there is none.
        - refGroup (optional, default "ref"): set to "" when there is none.
        - balanceGroup (optional, default "balance"): set to "" when there is none.

        Reuse these pieces inside bodyRegex:
        - Amount: (?<amount>[0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)
        - Last 4: (?<last4>[0-9]{4})

        Do not put full account numbers or the original message text in the JSON. Invalid regex rejects the whole file.

        Example of one template (structure only):
        {
          "id": "union_mob_bk_debit",
          "senderSuffixes": ["UNIONB"],
          "priority": 88,
          "bodyRegex": "debited for inr\\s+(?<amount>[0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)",
          "transactionType": "EXPENSE",
          "paymentChannel": "UPI",
          "instrument": "SAVINGS",
          "intent": "SETTLED"
        }

        Reply format
        1. At most 12 short bullets: which banks you covered and what you skipped.
        2. A line that is exactly ---JSON---
        3. Only the JSON object after that line, pretty-printed, starting with { and ending with }.

        Then wait for my SMS export if it is not already attached. Build the templates from that export only.
        """.trimIndent()
}
