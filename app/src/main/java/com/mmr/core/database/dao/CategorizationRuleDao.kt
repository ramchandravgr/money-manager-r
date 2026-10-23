package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mmr.core.database.entity.CategorizationRuleEntity

@Dao
interface CategorizationRuleDao {
    @Query("SELECT COUNT(*) FROM categorization_rules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<CategorizationRuleEntity>)

    @Query(
        """
        SELECT * FROM categorization_rules
        WHERE isEnabled = 1
        ORDER BY priority DESC, id ASC
        """,
    )
    suspend fun getEnabledOrdered(): List<CategorizationRuleEntity>
}
