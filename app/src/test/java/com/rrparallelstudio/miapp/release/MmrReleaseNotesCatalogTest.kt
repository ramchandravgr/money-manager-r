package com.rrparallelstudio.miapp.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MmrReleaseNotesCatalogTest {
    @Test
    fun notesForVersion_returnsEntryForKnownVersion() {
        val notes = MmrReleaseNotesCatalog.notesForVersion("0.9.0")
        requireNotNull(notes)
        assertEquals("0.9.0", notes.version)
        assertTrue(notes.changesNewestFirst.isNotEmpty())
    }

    @Test
    fun priorMajorMinorHistory_excludesCurrentVersion() {
        val history = MmrReleaseNotesCatalog.priorMajorMinorHistory("0.9.0")
        assertTrue(history.none { it.version == "0.9.0" })
    }

    @Test
    fun priorMajorMinorHistory_capsAtFivePastVersions() {
        val history = MmrReleaseNotesCatalog.priorMajorMinorHistory("99.99.99")
        assertEquals(
            MmrReleaseNotesCatalog.MAX_PAST_VERSIONS_IN_WHATS_NEW,
            history.size,
        )
    }

    @Test
    fun notesForVersion_unknownReturnsNull() {
        assertNull(MmrReleaseNotesCatalog.notesForVersion("99.0.0"))
    }
}
