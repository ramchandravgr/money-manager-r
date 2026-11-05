package com.mmr.core.lansync.transport

import com.mmr.core.webmanage.WebManageHttpConstants

/** Request body size limits for the shared LAN HTTP server (LAN sync + Web Manage). */
object MmrHttpBodyLimits {
    const val MAX_BODY_BYTES = 1_048_576

    /** Login / pair payloads should stay tiny (credentials + pairing code). */
    const val MAX_LOGIN_BODY_BYTES = 16_384

    fun maxBodyBytesForPath(path: String): Int =
        when (path) {
            WebManageHttpConstants.PATH_AUTH_LOGIN,
            LanSyncHttpConstants.PATH_PAIR,
            -> MAX_LOGIN_BODY_BYTES
            else -> MAX_BODY_BYTES
        }
}
