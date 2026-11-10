package com.rrparallelstudio.miapp.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.RecurringRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity

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
