package com.mmr.core.lansync

import org.json.JSONObject

data class LanSyncPairingOffer(
    val deviceId: String,
    val displayName: String,
    val inboundAuthToken: String,
)

object LanSyncPairingCodec {
    private const val PREFIX = "mmr-lan-sync:"

    fun encode(offer: LanSyncPairingOffer): String =
        PREFIX +
            JSONObject()
                .apply {
                    put("deviceId", offer.deviceId)
                    put("displayName", offer.displayName)
                    put("inboundAuthToken", offer.inboundAuthToken)
                }.toString()

    fun decode(raw: String): LanSyncPairingOffer {
        val jsonText = raw.trim().removePrefix(PREFIX)
        val json = JSONObject(jsonText)
        return LanSyncPairingOffer(
            deviceId = json.getString("deviceId"),
            displayName = json.getString("displayName"),
            inboundAuthToken = json.getString("inboundAuthToken"),
        )
    }
}
