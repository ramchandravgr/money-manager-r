package com.rrparallelstudio.miapp.core.asset

import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.model.enums.AssetPurchasePaymentMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetPurchasePaymentTest {
    @Test
    fun splitSumWithinTolerance() {
        assertTrue(purchaseSplitMatchesTotal(1_000_000.0, 200_000.0, 800_000.0))
        assertFalse(purchaseSplitMatchesTotal(1_000_000.0, 200_000.0, 750_000.0))
    }

    @Test
    fun legacyModeWithLinkedLoanIsFullyFinanced() {
        val entity = AssetEntity(name = "Car", linkedLoanId = 5L, purchasePrice = 500_000.0)
        assertEquals(AssetPurchasePaymentMode.FULLY_FINANCED, entity.resolvedPurchasePaymentMode())
    }
}
