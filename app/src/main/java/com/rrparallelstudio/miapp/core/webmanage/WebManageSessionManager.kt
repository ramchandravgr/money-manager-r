package com.rrparallelstudio.miapp.core.webmanage

import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebManageSessionManager
    @Inject
    constructor() {
        private val sessions = ConcurrentHashMap<String, SessionRecord>()
        private val random = SecureRandom()

        fun createSession(clientAddress: String): String {
            val tokenBytes = ByteArray(24)
            random.nextBytes(tokenBytes)
            val token = tokenBytes.joinToString("") { "%02x".format(it) }
            sessions[token] =
                SessionRecord(
                    expiresAtMs = System.currentTimeMillis() + SESSION_TTL_MS,
                    clientAddress = normalizeClientAddress(clientAddress),
                )
            pruneExpired()
            return token
        }

        fun isValid(
            token: String?,
            clientAddress: String?,
        ): Boolean {
            if (token.isNullOrBlank()) return false
            val record = sessions[token] ?: return false
            if (System.currentTimeMillis() > record.expiresAtMs) {
                sessions.remove(token)
                return false
            }
            if (record.clientAddress != normalizeClientAddress(clientAddress)) {
                sessions.remove(token)
                return false
            }
            return true
        }

        fun invalidate(token: String?) {
            if (!token.isNullOrBlank()) {
                sessions.remove(token)
            }
        }

        fun invalidateAll() {
            sessions.clear()
        }

        private fun pruneExpired() {
            val now = System.currentTimeMillis()
            sessions.entries.removeIf { it.value.expiresAtMs < now }
        }

        private fun normalizeClientAddress(raw: String?): String =
            raw?.trim()?.takeIf { it.isNotEmpty() } ?: UNKNOWN_CLIENT

        private data class SessionRecord(
            val expiresAtMs: Long,
            val clientAddress: String,
        )

        companion object {
            const val SESSION_TTL_MS = 2L * 60L * 60L * 1000L

            private const val UNKNOWN_CLIENT = "unknown"
        }
    }
