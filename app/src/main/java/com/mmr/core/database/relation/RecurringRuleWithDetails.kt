package com.mmr.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.entity.SubcategoryEntity

data class RecurringRuleWithDetails(
    @Embedded
    val rule: RecurringRuleEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id",
    )
    val category: CategoryEntity?,
    @Relation(
        parentColumn = "subcategoryId",
        entityColumn = "id",
    )
    val subcategory: SubcategoryEntity?,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id",
    )
    val account: AccountEntity?,
)
