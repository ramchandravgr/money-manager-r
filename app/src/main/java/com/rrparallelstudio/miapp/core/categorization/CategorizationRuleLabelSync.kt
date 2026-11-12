package com.rrparallelstudio.miapp.core.categorization

import com.rrparallelstudio.miapp.core.database.entity.CategorizationRuleEntity

/**
 * Rewrites SMS categorization rules when a category or subcategory label changes.
 * Rules store those labels as text, so the stored text has to move with the category row.
 */
internal object CategorizationRuleLabelSync {
    fun categoryRenamed(
        rules: List<CategorizationRuleEntity>,
        oldName: String,
        newName: String,
        now: Long,
    ): List<CategorizationRuleEntity> =
        rules.mapNotNull { rule ->
            if (!rule.categoryName.equals(oldName, ignoreCase = true)) return@mapNotNull null
            if (rule.categoryName == newName) return@mapNotNull null
            rule.copy(categoryName = newName, updatedAt = now)
        }

    fun subcategoryRenamed(
        rules: List<CategorizationRuleEntity>,
        categoryName: String,
        oldName: String,
        newName: String,
        now: Long,
    ): List<CategorizationRuleEntity> =
        rules.mapNotNull { rule ->
            if (!sameCategory(rule, categoryName)) return@mapNotNull null
            val sub = rule.subcategoryName ?: return@mapNotNull null
            if (!sub.equals(oldName, ignoreCase = true)) return@mapNotNull null
            if (sub == newName) return@mapNotNull null
            rule.copy(subcategoryName = newName, updatedAt = now)
        }

    fun subcategoryRemoved(
        rules: List<CategorizationRuleEntity>,
        categoryName: String,
        subcategoryName: String,
        now: Long,
    ): List<CategorizationRuleEntity> =
        rules.mapNotNull { rule ->
            if (!sameCategory(rule, categoryName)) return@mapNotNull null
            val sub = rule.subcategoryName ?: return@mapNotNull null
            if (!sub.equals(subcategoryName, ignoreCase = true)) return@mapNotNull null
            rule.copy(subcategoryName = null, updatedAt = now)
        }

    fun categoryMigrated(
        rules: List<CategorizationRuleEntity>,
        oldName: String,
        newName: String,
        now: Long,
    ): List<CategorizationRuleEntity> =
        rules.mapNotNull { rule ->
            if (!rule.categoryName.equals(oldName, ignoreCase = true)) return@mapNotNull null
            if (rule.categoryName == newName && rule.subcategoryName == null) return@mapNotNull null
            rule.copy(categoryName = newName, subcategoryName = null, updatedAt = now)
        }

    fun categoryRemoved(
        rules: List<CategorizationRuleEntity>,
        categoryName: String,
        now: Long,
    ): List<CategorizationRuleEntity> =
        rules.mapNotNull { rule ->
            if (!rule.categoryName.equals(categoryName, ignoreCase = true)) return@mapNotNull null
            if (!rule.isEnabled) return@mapNotNull null
            rule.copy(isEnabled = false, updatedAt = now)
        }

    private fun sameCategory(
        rule: CategorizationRuleEntity,
        categoryName: String,
    ): Boolean = rule.categoryName.equals(categoryName, ignoreCase = true)
}
