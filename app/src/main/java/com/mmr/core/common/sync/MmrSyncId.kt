package com.mmr.core.common.sync

import java.util.UUID

object MmrSyncId {
    fun newId(): String = UUID.randomUUID().toString()

    fun normalize(existing: String?): String = existing?.trim()?.takeIf { it.isNotEmpty() } ?: newId()
}
