package com.mmr.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private val PickerHeaderBlue = Color(0xFF263248)
private val PickerSelectedMonth = Color(0xFFE86A4F)

@Composable
fun HomeMonthPickerDialog(
    visible: Boolean,
    pickerYear: Int,
    selectedYear: Int,
    selectedMonth: Int,
    onDismiss: () -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthSelected: (Int) -> Unit,
    onThisMonth: () -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PickerHeaderBlue)
                    .padding(start = 20.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onThisMonth) {
                    Text(
                        text = "THIS MONTH",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Text("✕", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onPreviousYear) {
                    Text("‹", style = MaterialTheme.typography.headlineSmall)
                }
                Text(
                    text = pickerYear.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                TextButton(onClick = onNextYear) {
                    Text("›", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Month.entries.chunked(3).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowMonths.forEach { month ->
                            val monthValue = month.value
                            val isSelected = pickerYear == selectedYear && monthValue == selectedMonth
                            val label = month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                .uppercase(Locale.getDefault())

                            Text(
                                text = label,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onMonthSelected(monthValue) }
                                    .padding(vertical = 14.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PickerSelectedMonth else Color.Black
                            )
                        }
                        if (rowMonths.size < 3) {
                            repeat(3 - rowMonths.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
