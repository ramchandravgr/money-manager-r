package com.mmr.core.parser.catalog

import android.content.Context
import org.json.JSONObject
import java.util.Locale

/**
 * Maps UPI VPAs and PSP handles to friendly merchant labels (offline JSON).
 */
object UpiDisplayNameTable {
    private var exactVpa: Map<String, String> = emptyMap()
    private var pspHandles: Map<String, String> = emptyMap()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val text =
            context.assets
                .open("upi_display_names.json")
                .bufferedReader()
                .use { it.readText() }
        val root = JSONObject(text)
        loadFromJsonRoot(root)
    }

    internal fun loadFromJsonRoot(root: JSONObject) {
        exactVpa = parseMap(root.optJSONObject("exactVpa"))
        pspHandles = parseMap(root.optJSONObject("pspHandles"))
        loaded = true
    }

    fun resolve(raw: String): String {
        val trimmed = raw.trim().trimEnd('.', ',')
        if (trimmed.isEmpty()) return trimmed

        val lowerFull = trimmed.lowercase(Locale.getDefault())
        exactVpa[lowerFull]?.let { return it }

        if (trimmed.contains("@")) {
            val vpaLower = lowerFull
            exactVpa[vpaLower]?.let { return it }
            val psp = vpaLower.substringAfterLast('@', "")
            pspHandles[psp]?.let { pspName ->
                val local =
                    vpaLower
                        .substringBefore('@')
                        .substringBefore('.')
                        .replace('-', ' ')
                        .trim()
                if (local.isNotEmpty() && local.length > 2) {
                    return local.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                }
                return pspName
            }
            val handle = trimmed.substringBefore("@")
            return handle
                .substringBefore('.')
                .replace('-', ' ')
                .trim()
                .replaceFirstChar { it.titlecase(Locale.getDefault()) }
        }

        return trimmed
    }

    private fun parseMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.optString(key)?.takeIf { it.isNotBlank() } ?: continue
            out[key.lowercase(Locale.getDefault())] = value
        }
        return out
    }
}
