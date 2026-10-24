package com.mmr.core.webmanage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebManagePasswordHasherTest {
    @Test
    fun verify_matchesHashForSamePassword() {
        val salt = WebManagePasswordHasher.newSalt()
        val hash = WebManagePasswordHasher.hash("secret-pass", salt)
        assertTrue(WebManagePasswordHasher.verify("secret-pass", salt, hash))
        assertFalse(WebManagePasswordHasher.verify("wrong", salt, hash))
    }
}
