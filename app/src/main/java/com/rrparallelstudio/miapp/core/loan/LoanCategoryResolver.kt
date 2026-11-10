package com.rrparallelstudio.miapp.core.loan

import com.rrparallelstudio.miapp.core.database.dao.CategoryDao
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.core.model.enums.LoanType
import javax.inject.Inject
import javax.inject.Singleton

data class LoanCategoryIds(
    val categoryId: Long,
    val subcategoryId: Long,
)

@Singleton
class LoanCategoryResolver
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
    ) {
        suspend fun resolveForLoan(
            loanName: String,
            loanType: LoanType,
        ): LoanCategoryIds {
            val parentType =
                when (loanType) {
                    LoanType.BORROWED -> CategoryType.EXPENSE
                    LoanType.LENT -> CategoryType.INCOME
                }
            val parent =
                categoryDao
                    .getActiveCategoriesOnce()
                    .firstOrNull { it.name.equals(LOANS_CATEGORY_NAME, ignoreCase = true) && it.type == parentType }
                    ?: categoryDao
                        .insert(
                            CategoryEntity(
                                name = LOANS_CATEGORY_NAME,
                                iconName = "account_balance",
                                colorHex = "#6366F1",
                                type = parentType,
                                isDefault = false,
                                isActive = true,
                                orderIndex = 900,
                            ),
                        ).let { id -> categoryDao.getCategoryById(id)!! }

            val subName = loanName.trim().ifEmpty { "Loan" }
            val existingSub =
                categoryDao
                    .getSubcategoriesForCategoryOnce(parent.id)
                    .firstOrNull { it.name.equals(subName, ignoreCase = true) && it.isActive }
            val subId =
                existingSub?.id ?: categoryDao.insertSubcategory(
                    SubcategoryEntity(
                        categoryId = parent.id,
                        name = subName,
                        isActive = true,
                        orderIndex = 0,
                    ),
                )
            return LoanCategoryIds(parent.id, subId)
        }

        private companion object {
            const val LOANS_CATEGORY_NAME = "Loans"
        }
    }
