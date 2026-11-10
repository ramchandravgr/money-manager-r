package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat
import com.rrparallelstudio.miapp.core.lansync.model.MmrLanSyncPayload
import org.json.JSONArray
import org.json.JSONObject

/** One local row the peer's newer copy replaced. Shown highlighted in sync history. */
data class LanSyncOverrideLine(
    val kind: String,
    val title: String,
    val yours: String,
    val theirs: String,
)

class LanSyncInboundJournal {
    var added: Int = 0
        private set
    var unchanged: Int = 0
        private set
    private val _overrides = mutableListOf<LanSyncOverrideLine>()
    val overrides: List<LanSyncOverrideLine> get() = _overrides

    fun added() {
        added++
    }

    fun unchanged() {
        unchanged++
    }

    fun replaced(
        kind: String,
        title: String,
        yours: String,
        theirs: String,
    ) {
        _overrides +=
            LanSyncOverrideLine(
                kind = kind,
                title = title.ifBlank { kind },
                yours = yours,
                theirs = theirs,
            )
    }
}

object LanSyncOverrideCopy {
    const val DELETED_ON_OTHER_PHONE: String = "Deleted on the other phone"

    fun money(amount: Double): String = MmrMoneyFormat.formatInr(amount)

    fun transaction(
        amount: Double,
        type: String,
        note: String?,
    ): String =
        buildString {
            append(money(amount))
            append(" · ")
            append(type)
            if (!note.isNullOrBlank()) {
                append(" · ")
                append(note.trim())
            }
        }

    fun loan(
        outstanding: Double,
        rate: Double,
        status: String,
    ): String = "Outstanding ${money(outstanding)} · $rate% · $status"

    fun recurring(
        amount: Double,
        enabled: Boolean,
        note: String?,
    ): String =
        buildString {
            append(money(amount))
            append(" · ")
            append(if (enabled) "On" else "Off")
            if (!note.isNullOrBlank()) {
                append(" · ")
                append(note.trim())
            }
        }

    fun account(
        balance: Double,
        active: Boolean,
    ): String = "Balance ${money(balance)}" + if (active) "" else " · Inactive"

    fun budget(
        limit: Double,
        active: Boolean,
    ): String = "Limit ${money(limit)}" + if (active) "" else " · Inactive"

    fun asset(
        value: Double,
        status: String,
    ): String = "Value ${money(value)} · $status"
}

object LanSyncOverrideJson {
    fun encode(lines: List<LanSyncOverrideLine>): String {
        val array = JSONArray()
        lines.take(MAX_STORED).forEach { line ->
            array.put(
                JSONObject()
                    .put("kind", line.kind)
                    .put("title", line.title)
                    .put("yours", line.yours)
                    .put("theirs", line.theirs),
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<LanSyncOverrideLine> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    add(
                        LanSyncOverrideLine(
                            kind = obj.optString("kind"),
                            title = obj.optString("title"),
                            yours = obj.optString("yours"),
                            theirs = obj.optString("theirs"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private const val MAX_STORED = 80
}

/** Appearance (theme, colors, start screen) is per phone. Never apply a peer copy. */
fun MmrLanSyncPayload.withoutDeviceAppearance(): MmrLanSyncPayload = copy(displaySettings = null)
