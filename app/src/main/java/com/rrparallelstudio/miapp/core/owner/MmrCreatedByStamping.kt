package com.rrparallelstudio.miapp.core.owner

import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.database.entity.RecurringRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity

fun TransactionEntity.withLocalCreatedBy(stamp: MmrCreatedByStamp): TransactionEntity =
    if (createdByProfileId != null) {
        this
    } else {
        copy(
            createdByProfileId = stamp.profileId,
            createdByDisplayName = stamp.displayName,
        )
    }

fun LoanEntity.withLocalCreatedBy(stamp: MmrCreatedByStamp): LoanEntity =
    if (createdByProfileId != null) {
        this
    } else {
        copy(
            createdByProfileId = stamp.profileId,
            createdByDisplayName = stamp.displayName,
        )
    }

fun RecurringRuleEntity.withLocalCreatedBy(stamp: MmrCreatedByStamp): RecurringRuleEntity =
    if (createdByProfileId != null) {
        this
    } else {
        copy(
            createdByProfileId = stamp.profileId,
            createdByDisplayName = stamp.displayName,
        )
    }

fun BudgetEntity.withLocalCreatedBy(stamp: MmrCreatedByStamp): BudgetEntity =
    if (createdByProfileId != null) {
        this
    } else {
        copy(
            createdByProfileId = stamp.profileId,
            createdByDisplayName = stamp.displayName,
        )
    }

fun AssetEntity.withLocalCreatedBy(stamp: MmrCreatedByStamp): AssetEntity =
    if (createdByProfileId != null) {
        this
    } else {
        copy(
            createdByProfileId = stamp.profileId,
            createdByDisplayName = stamp.displayName,
        )
    }
