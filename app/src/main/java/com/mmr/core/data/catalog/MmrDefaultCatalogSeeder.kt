package com.mmr.core.data.catalog

import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.model.enums.CategoryType

/**
 * Inserts default categories and subcategories when missing (by unique name + type / name per parent).
 * Safe to call on every app start.
 */
object MmrDefaultCatalogSeeder {
    private data class DefaultCategory(
        val name: String,
        val iconName: String,
        val colorHex: String,
        val type: CategoryType,
        val subcategories: List<String>,
    )

    private val catalog =
        listOf(
            DefaultCategory(
                name = "Food & Dining",
                iconName = "restaurant",
                colorHex = "#EF4444",
                type = CategoryType.EXPENSE,
                subcategories = listOf("Restaurants", "Delivery", "Snacks"),
            ),
            DefaultCategory(
                name = "Groceries",
                iconName = "shopping_cart",
                colorHex = "#F59E0B",
                type = CategoryType.EXPENSE,
                subcategories = listOf("Supermarket", "Local market"),
            ),
            DefaultCategory(
                name = "Transport & Fuel",
                iconName = "directions_car",
                colorHex = "#3B82F6",
                type = CategoryType.EXPENSE,
                subcategories = listOf("Fuel", "Ride / taxi", "Public transit"),
            ),
            DefaultCategory(
                name = "Shopping",
                iconName = "shopping_bag",
                colorHex = "#EC4899",
                type = CategoryType.EXPENSE,
                subcategories = listOf("Clothing", "Electronics", "Online"),
            ),
            DefaultCategory(
                name = "Bills & Utilities",
                iconName = "receipt_long",
                colorHex = "#8B5CF6",
                type = CategoryType.EXPENSE,
                subcategories = listOf("Electricity", "Mobile / internet", "Rent"),
            ),
            DefaultCategory(
                name = "Health",
                iconName = "medical_services",
                colorHex = "#14B8A6",
                type = CategoryType.EXPENSE,
                subcategories = listOf("Pharmacy", "Doctor / clinic"),
            ),
            DefaultCategory(
                name = "Entertainment",
                iconName = "movie",
                colorHex = "#F97316",
                type = CategoryType.EXPENSE,
                subcategories = listOf("Streaming", "Events", "Games"),
            ),
            DefaultCategory(
                name = "Salary",
                iconName = "payments",
                colorHex = "#10B981",
                type = CategoryType.INCOME,
                subcategories = listOf("Monthly", "Bonus"),
            ),
            DefaultCategory(
                name = "Investment / Return",
                iconName = "trending_up",
                colorHex = "#06B6D4",
                type = CategoryType.INCOME,
                subcategories = listOf("Dividends", "Interest", "Capital gains"),
            ),
        )

    suspend fun seedIfMissing(database: MmrDatabase) {
        val categoryDao = database.categoryDao()
        val existing = categoryDao.getActiveCategoriesOnce()
        val existingByKey = existing.associateBy { "${it.name}|${it.type}" }

        val toInsertCategories =
            catalog
                .filter { def -> "${def.name}|${def.type}" !in existingByKey }
                .mapIndexed { index, def ->
                    CategoryEntity(
                        name = def.name,
                        iconName = def.iconName,
                        colorHex = def.colorHex,
                        type = def.type,
                        isDefault = true,
                        orderIndex = index,
                    )
                }
        if (toInsertCategories.isNotEmpty()) {
            categoryDao.insertCategories(toInsertCategories)
        }

        val allCategories = categoryDao.getActiveCategoriesOnce()
        val idByKey = allCategories.associateBy { "${it.name}|${it.type}" }

        val subsToInsert = mutableListOf<SubcategoryEntity>()
        catalog.forEach { def ->
            val parentId = idByKey["${def.name}|${def.type}"]?.id ?: return@forEach
            val existingSubs =
                categoryDao
                    .getSubcategoriesForCategoryOnce(parentId)
                    .map { it.name }
                    .toSet()
            def.subcategories.forEachIndexed { index, subName ->
                if (subName !in existingSubs) {
                    subsToInsert +=
                        SubcategoryEntity(
                            categoryId = parentId,
                            name = subName,
                            orderIndex = index,
                        )
                }
            }
        }
        if (subsToInsert.isNotEmpty()) {
            categoryDao.insertSubcategories(subsToInsert)
        }
    }
}
