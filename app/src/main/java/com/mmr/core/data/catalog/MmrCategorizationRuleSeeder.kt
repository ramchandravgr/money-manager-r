package com.mmr.core.data.catalog

import android.content.Context
import com.mmr.core.database.dao.CategorizationRuleDao
import com.mmr.core.database.entity.CategorizationRuleEntity
import com.mmr.core.model.enums.CategorizationMatchType
import org.json.JSONObject

object MmrCategorizationRuleSeeder {
    suspend fun seedIfMissing(
        context: Context,
        dao: CategorizationRuleDao,
    ) {
        if (dao.count() > 0) return
        val text =
            context.assets
                .open("categorization_rules.json")
                .bufferedReader()
                .use { it.readText() }
        val array = JSONObject(text).getJSONArray("rules")
        val entities = mutableListOf<CategorizationRuleEntity>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            entities.add(
                CategorizationRuleEntity(
                    priority = item.optInt("priority", 50),
                    matchType = CategorizationMatchType.valueOf(item.getString("matchType")),
                    pattern = item.getString("pattern"),
                    categoryName = item.getString("categoryName"),
                    subcategoryName = item.optString("subcategoryName").takeIf { it.isNotBlank() },
                    isEnabled = true,
                ),
            )
        }
        dao.insertAll(entities)
    }
}
