package com.mmr.core.parser.duplicate

import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.ParsedSmsCandidate
import com.mmr.core.parser.model.ParserConfidence
import com.mmr.core.parser.model.SmsInstrument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsCrossChannelDedupTest {

    @Test
    fun deduplicate_keepsCardAndDropsHpPayForSameAmount() {
        val card = sample(
            amount = 850.0,
            time = 1_700_000_000_000L,
            body = "INR 850 is spent on your bobcard ending 4321 at HP Pay Direct on 01-01-2026.",
            instrument = SmsInstrument.CREDIT_CARD,
            last4 = "4321"
        )
        val hp = sample(
            amount = 850.0,
            time = 1_700_000_030_000L,
            body = "Transaction of Rs.850 for purchase of petrol is successful. HP Pay",
            instrument = SmsInstrument.FUEL_WALLET,
            sender = "VM-HPPAYS"
        )
        val result = SmsCrossChannelDedup.deduplicate(listOf(hp, card))
        assertEquals(1, result.kept.size)
        assertEquals(1, result.suppressedCount)
        assertTrue(result.kept.first().rawBody.contains("bobcard"))
    }

    private fun sample(
        amount: Double,
        time: Long,
        body: String,
        instrument: SmsInstrument,
        last4: String? = null,
        sender: String = "AD-BOBCRD-S"
    ) = ParsedSmsCandidate(
        sender = sender,
        amount = amount,
        type = TransactionType.EXPENSE,
        timestamp = time,
        accountLastDigits = last4,
        rawMessageId = "1",
        messageHash = body.hashCode().toString(),
        confidenceScore = 80,
        confidenceBand = ParserConfidence.HIGH,
        rawBody = body,
        instrument = instrument
    )
}
