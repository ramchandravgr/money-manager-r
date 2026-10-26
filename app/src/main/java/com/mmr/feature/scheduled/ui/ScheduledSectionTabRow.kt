package com.mmr.feature.scheduled.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrTabRow
import com.mmr.app.ui.components.MmrTabRowSurface
import com.mmr.feature.scheduled.model.ScheduledSection

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
