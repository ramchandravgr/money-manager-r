package com.rrparallelstudio.miapp.feature.scheduled.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rrparallelstudio.miapp.feature.scheduled.model.ScheduledSection
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrTabRow
import com.rrparallelstudio.miapp.ui.components.MmrTabRowSurface

@Composable
fun ScheduledSectionTabRow(
    selectedSection: ScheduledSection,
    onSectionSelected: (ScheduledSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = ScheduledSection.entries
    MmrTabRow(
        tabs = sections.map { it.title },
        selectedIndex = sections.indexOf(selectedSection).coerceAtLeast(0),
        onTabSelected = { index -> onSectionSelected(sections[index]) },
        modifier = modifier,
        testTagForIndex = { index -> MmrTestTags.scheduledSectionTab(sections[index]) },
        surface = MmrTabRowSurface.Ledger,
    )
}
