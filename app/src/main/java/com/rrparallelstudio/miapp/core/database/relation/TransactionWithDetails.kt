package com.rrparallelstudio.miapp.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity

data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "subcategoryId", entityColumn = "id")
    val subcategory: SubcategoryEntity?,
    @Relation(parentColumn = "accountId", entityColumn = "id")
    val account: AccountEntity?,
    @Relation(parentColumn = "relatedAccountId", entityColumn = "id")
    val relatedAccount: AccountEntity?,
    @Relation(parentColumn = "loanId", entityColumn = "id")
    val loan: LoanEntity?,
)
