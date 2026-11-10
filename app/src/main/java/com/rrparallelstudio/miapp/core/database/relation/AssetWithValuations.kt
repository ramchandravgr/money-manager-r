package com.rrparallelstudio.miapp.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetValuationEntity

data class AssetWithValuations(
    @Embedded val asset: AssetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "assetId",
    )
    val valuations: List<AssetValuationEntity>,
)
