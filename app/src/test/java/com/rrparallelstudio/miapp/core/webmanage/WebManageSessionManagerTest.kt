package com.rrparallelstudio.miapp.core.webmanage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebManageSessionManagerTest {
    private val manager = WebManageSessionManager()

    @Test
    fun isValid_acceptsTokenForSameClientAddress() {
        val token = manager.createSession("192.168.1.50")
        assertTrue(manager.isValid(token, "192.168.1.50"))
    }

    @Test
    fun isValid_rejectsTokenFromDifferentClientAddress() {
        val token = manager.createSession("192.168.1.50")
        assertFalse(manager.isValid(token, "192.168.1.99"))
        assertFalse(manager.isValid(token, "192.168.1.50"))
    }

    @Test
    fun isValid_rejectsBlankToken() {
        assertFalse(manager.isValid(null, "192.168.1.1"))
        assertFalse(manager.isValid("", "192.168.1.1"))
    }
}
