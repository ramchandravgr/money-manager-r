package com.mmr.core.database.mapper

import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.relation.CategoryWithSubcategories
import com.mmr.core.model.category.Category
import com.mmr.core.model.category.CategoryColor
import com.mmr.core.model.category.CategoryId
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.category.Subcategory
import com.mmr.core.model.category.SubcategoryId

object CategoryPersistenceMapper {

    fun CategoryEntity.toCategory(): Category = Category(
        id = CategoryId(id),
        name = name,
        iconName = iconName,
        color = CategoryColor(colorHex),
        type = type,
        isDefault = isDefault,
        isActive = isActive,
        sortOrder = orderIndex
    )

    fun SubcategoryEntity.toSubcategory(): Subcategory = Subcategory(
        id = SubcategoryId(id),
        categoryId = CategoryId(categoryId),
        name = name,
        isActive = isActive,
        sortOrder = orderIndex
    )

    fun CategoryWithSubcategories.toModel(): CategoryWithChildren {
        val activeSorted = subcategories
            .filter { it.isActive }
            .sortedWith(compareBy({ it.orderIndex }, { it.name }))
        return CategoryWithChildren(
            category = category.toCategory(),
            subcategories = activeSorted.map { it.toSubcategory() }
        )
    }

    fun Category.toEntity(): CategoryEntity = CategoryEntity(
        id = id.value,
        name = name,
        iconName = iconName,
        colorHex = color.hex,
        type = type,
        isDefault = isDefault,
        isActive = isActive,
        orderIndex = sortOrder
    )

    fun Subcategory.toEntity(): SubcategoryEntity = SubcategoryEntity(
        id = id.value,
        categoryId = categoryId.value,
        name = name,
        isActive = isActive,
        orderIndex = sortOrder
    )
}
