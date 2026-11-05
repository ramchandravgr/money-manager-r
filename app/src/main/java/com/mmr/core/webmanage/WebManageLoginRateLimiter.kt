package com.mmr.core.webmanage

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object WebManageLoginRateLimiter {
    private const val MAX_FAILURES = 5
    private const val LOCKOUT_MS = 15 * 60 * 1000L
    private const val WINDOW_MS = 15 * 60 * 1000L

    private val failuresByClient = ConcurrentHashMap<String, ClientFailures>()

    fun isLoginAllowed(clientKey: String): Boolean {
        pruneExpired(clientKey)
        val entry = failuresByClient[clientKey] ?: return true
        val now = System.currentTimeMillis()
        if (entry.lockoutUntilMs > now) return false
        return entry.failureCount.get() < MAX_FAILURES
    }

    fun recordFailure(clientKey: String) {
        val now = System.currentTimeMillis()
        val entry =
            failuresByClient.compute(clientKey) { _, existing ->
                when {
                    existing == null || now - existing.windowStartMs > WINDOW_MS ->
                        ClientFailures(windowStartMs = now, failureCount = AtomicInteger(0), lockoutUntilMs = 0L)
                    else -> existing
                }
            } ?: return
        val failures = entry.failureCount.incrementAndGet()
        if (failures >= MAX_FAILURES) {
            entry.lockoutUntilMs = now + LOCKOUT_MS
        }
    }

    fun recordSuccess(clientKey: String) {
        failuresByClient.remove(clientKey)
    }

    fun clientKeyFromHeaders(headers: Map<String, String>): String =
        headers["x-mmr-client-address"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: "unknown"

    private fun pruneExpired(clientKey: String) {
        val entry = failuresByClient[clientKey] ?: return
        val now = System.currentTimeMillis()
        if (now - entry.windowStartMs > WINDOW_MS && entry.lockoutUntilMs <= now) {
            failuresByClient.remove(clientKey)
        }
    }

    private class ClientFailures(
        val windowStartMs: Long,
        val failureCount: AtomicInteger,
        @Volatile var lockoutUntilMs: Long,
    )
}
