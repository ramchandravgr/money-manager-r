package com.mmr.core.categorization

import com.mmr.core.database.dao.CategorizationRuleDao
import com.mmr.core.database.entity.CategorizationRuleEntity
import com.mmr.core.model.enums.CategorizationMatchType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizationRuleEngineTest {
    @Test
    fun resolvesMerchantPatternBeforeGenericHint() =
        runTest {
            val dao =
                FakeCategorizationRuleDao(
                    listOf(
                        CategorizationRuleEntity(
                            id = 1,
                            priority = 100,
                            matchType = CategorizationMatchType.MERCHANT_OR_NOTE_CONTAINS,
                            pattern = "zepto",
                            categoryName = "Groceries",
                        ),
                    ),
                )
            val engine = CategorizationRuleEngine(dao)
            assertEquals(
                "Groceries",
                engine.resolveCategoryName(
                    merchant = "Zepto",
                    note = "Zepto",
                    sender = "VM-HDFCBK",
                    smsBody = null,
                ),
            )
        }

    private class FakeCategorizationRuleDao(
        private val rules: List<CategorizationRuleEntity>,
    ) : CategorizationRuleDao {
        override suspend fun count(): Int = rules.size

        override suspend fun insertAll(rules: List<CategorizationRuleEntity>) = Unit

        override suspend fun getEnabledOrdered(): List<CategorizationRuleEntity> = rules

        override suspend fun getAll(): List<CategorizationRuleEntity> = rules

        override suspend fun getBySyncId(syncId: String): CategorizationRuleEntity? = rules.find { it.syncId == syncId }

        override suspend fun upsert(rule: CategorizationRuleEntity) = Unit

        override suspend fun deleteBySyncId(syncId: String) = Unit

        override suspend fun delete(rule: CategorizationRuleEntity) = Unit
    }
}
