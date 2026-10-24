package com.mmr.core.parser

import com.mmr.core.model.enums.SmsApprovalKind
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.engine.SmsParserEngine
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsParseResult
import com.mmr.core.parser.template.SmsTemplateRegistry
import com.mmr.core.parser.template.SmsTestAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Regression tests from Mezo AAAA1 vs MMR gap report (tools/mezo_mmr_gap_report.txt). */
class MezoGapParserFixesTest {
    private lateinit var engine: SmsParserEngine

    @Before
    fun setUp() {
        engine = SmsParserEngine(SmsTemplateRegistry.fromJson(SmsTestAssets.loadTemplatesJson()))
    }

    @Test
    fun unionMobBkCredit_notIgnored_despiteOtpFooter() {
        val result =
            engine.parseResult(
                RawSmsMessage(
                    id = "1",
                    sender = "JD-UNIONB-S",
                    body =
                        "A/c *9007 Credited for Rs:15000.00 on 30-04-2026 23:28:22 by Mob Bk ref no 205142682469 " +
                            "Avl Bal Rs:18261.55.Never Share OTP/PIN/CVV-Union Bank",
                    timestamp = 1L,
                ),
            )
        assertTrue(result is SmsParseResult.Transaction)
        val txn = result as SmsParseResult.Transaction
        assertEquals(15000.0, txn.candidate.amount, 0.01)
        assertEquals(TransactionType.INCOME, txn.candidate.type)
    }

    @Test
    fun hdfcOnlineBankingTransfer_parsesAsExpense() {
        val result =
            engine.parseResult(
                RawSmsMessage(
                    id = "2",
                    sender = "VM-HDFCBK-S",
                    body =
                        "Amt Deducted! Rs.78472 from your HDFC Bank A/c XX0561 for Money Transfer " +
                            "via HDFC Bank Online Banking. Not you?Call 18002586161/SMS BLOCK OB",
                    timestamp = 1L,
                ),
            )
        assertTrue(result is SmsParseResult.Transaction)
        assertEquals(78472.0, (result as SmsParseResult.Transaction).candidate.amount, 0.01)
    }

    @Test
    fun hdfcCcReversal_parsesAsCardRefund() {
        val result =
            engine.parseResult(
                RawSmsMessage(
                    id = "3",
                    sender = "AD-HDFCBK-S",
                    body =
                        "Transaction Reversed!On HDFC Bank CREDIT Card xx4148 Amt: Rs.1470 " +
                            "By AMAZON                000 On 2026-05-24:03:18:24",
                    timestamp = 1L,
                ),
            )
        assertTrue(result is SmsParseResult.CardRefund)
        assertEquals(SmsApprovalKind.CARD_REFUND, (result as SmsParseResult.CardRefund).candidate.approvalKind)
    }

    @Test
    fun canaraInterestCredit_parsesAsIncome() {
        val result =
            engine.parseResult(
                RawSmsMessage(
                    id = "4",
                    sender = "JK-CANBNK-S",
                    body =
                        "INR 175.00 credited to A/c XXX678 on 28/09/2026 towards Interest. " +
                            "Total Avail. Bal. INR 31,191.00. -Canara Bank",
                    timestamp = 1L,
                ),
            )
        assertTrue(result is SmsParseResult.Transaction)
        assertEquals(175.0, (result as SmsParseResult.Transaction).candidate.amount, 0.01)
    }

    @Test
    fun hdfcFdSweep_isInformational() {
        val result =
            engine.parseResult(
                RawSmsMessage(
                    id = "5",
                    sender = "VM-HDFCBK-S",
                    body =
                        "FD XX9018 of INR 55,228.00 will be liquidated for INR 55,107.00 for sweep in debit " +
                            "from A/c XX0561. Add money to your HDFC Bank A/c to avoid",
                    timestamp = 1L,
                ),
            )
        assertTrue(result is SmsParseResult.Informational)
    }
}
