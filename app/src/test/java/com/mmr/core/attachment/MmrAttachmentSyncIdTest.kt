package com.mmr.core.attachment

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MmrAttachmentSyncIdTest {
    @Test
    fun requireSafeSyncId_acceptsUuidStyle() {
        val id = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        assertTrue(MmrAttachmentSyncId.requireSafeSyncId(id) == id)
    }

    @Test
    fun requireSafeSyncId_rejectsPathTraversal() {
        assertThrows(IllegalArgumentException::class.java) {
            MmrAttachmentSyncId.requireSafeSyncId("../prefs.xml")
        }
    }

    @Test
    fun fileInRoot_staysUnderRoot() {
        val root =
            File.createTempFile("mmr-root", "").apply {
                delete()
                mkdirs()
            }
        try {
            val file = MmrAttachmentSyncId.fileInRoot(root, "photo-1.jpg")
            assertTrue(file.canonicalPath.startsWith(root.canonicalPath))
        } finally {
            root.deleteRecursively()
        }
    }
}
