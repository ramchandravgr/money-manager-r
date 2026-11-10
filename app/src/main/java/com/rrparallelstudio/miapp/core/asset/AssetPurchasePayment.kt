package com.rrparallelstudio.miapp.core.asset

import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.model.enums.AssetPurchasePaymentMode
import kotlin.math.abs

fun AssetEntity.purchasePaymentModeEnum(): AssetPurchasePaymentMode? =
    purchasePaymentMode?.let { raw ->
        runCatching { AssetPurchasePaymentMode.valueOf(raw) }.getOrNull()
    }

fun AssetPurchasePaymentMode.displayLabel(): String =
    when (this) {
        AssetPurchasePaymentMode.PAID_IN_FULL -> "Paid in full"
        AssetPurchasePaymentMode.FULLY_FINANCED -> "Fully financed"
        AssetPurchasePaymentMode.DOWN_PAYMENT_AND_EMI -> "Down payment + EMI loan"
    }

/** Non-inherited add/edit shows optional linked-loan row (not required to save). */
fun AssetPurchasePaymentMode.showsOptionalLinkedLoanField(): Boolean =
    this == AssetPurchasePaymentMode.FULLY_FINANCED ||
        this == AssetPurchasePaymentMode.DOWN_PAYMENT_AND_EMI

/** Rupees tolerance when validating down + financed vs total purchase price. */
const val ASSET_PURCHASE_SPLIT_TOLERANCE = 0.05

fun purchaseSplitMatchesTotal(
    total: Double,
    downPayment: Double,
    financedAmount: Double,
    tolerance: Double = ASSET_PURCHASE_SPLIT_TOLERANCE,
): Boolean = abs((downPayment + financedAmount) - total) <= tolerance

/** Legacy rows without [AssetEntity.purchasePaymentMode] stored. */
fun AssetEntity.resolvedPurchasePaymentMode(): AssetPurchasePaymentMode {
    if (isInherited) return AssetPurchasePaymentMode.PAID_IN_FULL
    purchasePaymentModeEnum()?.let { return it }
    return if (linkedLoanId != null) {
        AssetPurchasePaymentMode.FULLY_FINANCED
    } else {
        AssetPurchasePaymentMode.PAID_IN_FULL
    }
}
