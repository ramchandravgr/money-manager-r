package com.rrparallelstudio.miapp.ui.copy

import org.junit.Assert.assertEquals
import org.junit.Test

class MmrStaticUiCopyTest {
    @Test
    fun titleCasesWordsAfterSpacesAndSlashes() {
        assertEquals("Lender / Bank Name", "Lender / bank name".mmrStaticLabelCase())
        assertEquals("New Budget", "New budget".mmrStaticLabelCase())
        assertEquals("New Loan", "New loan".mmrStaticLabelCase())
    }

    @Test
    fun leavesAlreadyCorrectLabelsUnchanged() {
        assertEquals("Transactions", "Transactions".mmrStaticLabelCase())
        assertEquals("Fixed EMI", "Fixed EMI".mmrStaticLabelCase())
    }
}
