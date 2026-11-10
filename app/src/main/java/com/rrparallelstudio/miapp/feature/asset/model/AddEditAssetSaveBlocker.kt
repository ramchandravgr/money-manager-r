package com.rrparallelstudio.miapp.feature.asset.model

import com.rrparallelstudio.miapp.core.asset.purchaseSplitMatchesTotal
import com.rrparallelstudio.miapp.core.common.validation.MmrValidation
import com.rrparallelstudio.miapp.core.model.enums.AssetPurchasePaymentMode
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus

/** User-visible reason Save stays off, when the form looks mostly complete. */
fun addEditAssetSaveBlocker(state: AddEditAssetUiState): String? {
    if (state.name.trim().isEmpty()) return null
    if (!state.isInherited && state.purchaseDateMillis == null) {
        return "Select a purchase date."
    }
    val total = MmrValidation.parsePositiveAmount(state.purchasePrice) ?: return null
    if (state.status == AssetStatus.SOLD) {
        if (state.saleDateMillis == null) return "Select a sale date."
        if (MmrValidation.parseNonNegativeAmount(state.salePrice) == null) return "Enter a valid sale price."
    }
    if (state.isInherited) return null
    if (state.purchasePaymentMode == AssetPurchasePaymentMode.DOWN_PAYMENT_AND_EMI) {
        val down = MmrValidation.parseNonNegativeAmount(state.purchaseDownPayment)
        val financed = MmrValidation.parsePositiveAmount(state.purchaseFinancedAmount)
        when {
            down == null -> return "Enter a valid down payment."
            financed == null -> return "Enter the amount financed (EMI)."
            !purchaseSplitMatchesTotal(total, down, financed) ->
                return "Down payment and financed amount must add up to the total purchase price."
        }
    }
    return null
}
