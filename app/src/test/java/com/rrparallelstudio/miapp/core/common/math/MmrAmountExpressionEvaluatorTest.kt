package com.rrparallelstudio.miapp.core.common.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MmrAmountExpressionEvaluatorTest {
    private val invalid = "Invalid"

    @Test
    fun preview_addition_matchesAmountMath() {
        val preview =
            MmrAmountExpressionEvaluator.preview("13428+16255.49", invalid) as
                MmrAmountExpressionEvaluator.Preview.Valid
        assertEquals(29683.49, preview.value, 0.001)
    }

    @Test
    fun apply_roundsToTwoDecimals() {
        val result =
            MmrAmountExpressionEvaluator.apply("10/3", invalid) as
                MmrAmountExpressionEvaluator.ApplyResult.Ok
        assertEquals("3.33", result.amountPlain)
    }

    @Test
    fun preview_invalid_unclosedParen() {
        val preview = MmrAmountExpressionEvaluator.preview("(100+", invalid)
        assertTrue(preview is MmrAmountExpressionEvaluator.Preview.Invalid)
    }

    @Test
    fun preview_invalid_badCharacter() {
        val preview = MmrAmountExpressionEvaluator.preview("1&2", invalid)
        assertTrue(preview is MmrAmountExpressionEvaluator.Preview.Invalid)
    }

    @Test
    fun apply_tooLarge() {
        val result = MmrAmountExpressionEvaluator.apply("999999999999*999", invalid)
        assertEquals(MmrAmountExpressionEvaluator.ApplyResult.TooLarge, result)
    }

    @Test
    fun power_and_modulo() {
        val preview =
            MmrAmountExpressionEvaluator.preview("2^10+5%3", invalid) as
                MmrAmountExpressionEvaluator.Preview.Valid
        assertEquals(1024.0 + 2.0, preview.value, 0.0)
    }

    @Test
    fun unaryMinus() {
        val preview =
            MmrAmountExpressionEvaluator.preview("-(3+2)", invalid) as
                MmrAmountExpressionEvaluator.Preview.Valid
        assertEquals(-5.0, preview.value, 0.0)
    }

    @Test
    fun divideByZero_invalid() {
        val preview = MmrAmountExpressionEvaluator.preview("1/0", invalid)
        assertTrue(preview is MmrAmountExpressionEvaluator.Preview.Invalid)
    }

    @Test
    fun seedExpressionFromAmountField_stripsGrouping() {
        assertEquals("1234.5", MmrAmountExpressionEvaluator.seedExpressionFromAmountField("1,234.5"))
    }
}
