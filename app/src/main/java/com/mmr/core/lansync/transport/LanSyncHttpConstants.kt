package com.mmr.core.lansync.transport

object LanSyncHttpConstants {
    const val SERVICE_TYPE = "_mmr-sync._tcp."
    const val HEADER_DEVICE_ID = "X-MMR-Device-Id"
    const val HEADER_AUTH_TOKEN = "X-MMR-Auth-Token"
    const val PATH_SYNC = "/mmr/v1/sync"
    const val PATH_PAIR = "/mmr/v1/pair"
    const val PATH_PAIRING_OFFER = "/mmr/v1/pairing-offer"
    const val PATH_ATTACHMENT_PREFIX = "/mmr/v1/attachment/"
    const val DEFAULT_PORT = 19681

    fun pathAttachment(syncId: String): String = PATH_ATTACHMENT_PREFIX + syncId
}
