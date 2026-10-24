package com.mmr.core.webmanage

import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebManageSessionManager
    @Inject
    constructor() {
        private val sessions = ConcurrentHashMap<String, Long>()
        private val random = SecureRandom()

        fun createSession(): String {
            val tokenBytes = ByteArray(24)
            random.nextBytes(tokenBytes)
            val token = tokenBytes.joinToString("") { "%02x".format(it) }
            sessions[token] = System.currentTimeMillis() + SESSION_TTL_MS
            pruneExpired()
            return token
        }

        fun isValid(token: String?): Boolean {
            if (token.isNullOrBlank()) return false
            val expiresAt = sessions[token] ?: return false
            if (System.currentTimeMillis() > expiresAt) {
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
            sessions.entries.removeIf { it.value < now }
        }

        private companion object {
            const val SESSION_TTL_MS = 12L * 60L * 60L * 1000L
        }
    }
