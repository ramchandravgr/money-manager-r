package com.rrparallelstudio.miapp.ui.ftue

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme
import com.rrparallelstudio.miapp.navigation.Screen
import com.rrparallelstudio.miapp.navigation.mmrDestinationIconPair
import com.rrparallelstudio.miapp.ui.components.MmrCarryOverCalendarArrow
import com.rrparallelstudio.miapp.ui.components.MmrTotalReceiptSum
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

data class FtueBulletIconVisual(
    val imageVector: ImageVector,
    val tint: Color,
    val chipBackground: Color,
)

@Composable
fun ftueBulletIconVisual(icon: FtueBulletIcon): FtueBulletIconVisual {
    val scheme = MmrAmountColorScheme.SET_B
    val income = MmrBrandColors.incomeColor(scheme)
    val expense = MmrBrandColors.expenseColor(scheme)
    val onSurface = MmrBrandColors.dialogTitleText()
    val mutedChip = MmrBrandColors.dialogMessageText().copy(alpha = 0.12f)
    return when (icon) {
        FtueBulletIcon.SummaryIncome ->
            FtueBulletIconVisual(
                Icons.Filled.ArrowDownward,
                income,
                income.copy(alpha = 0.12f),
            )
        FtueBulletIcon.SummaryExpense ->
            FtueBulletIconVisual(
                Icons.Filled.ArrowUpward,
                expense,
                expense.copy(alpha = 0.1f),
            )
        FtueBulletIcon.SummaryCarryOver ->
            FtueBulletIconVisual(
                MmrCarryOverCalendarArrow,
                onSurface,
                Color(0xFF7C6A9A).copy(alpha = 0.14f),
            )
        FtueBulletIcon.SummaryTotal -> {
            val net = MmrBrandColors.summaryNetColor()
            FtueBulletIconVisual(
                MmrTotalReceiptSum,
                net,
                net.copy(alpha = if (MmrBrandColors.canvasIsDark()) 0.22f else 0.14f),
            )
        }
        FtueBulletIcon.AddTransaction ->
            FtueBulletIconVisual(Icons.Filled.Add, MmrBrandColors.Active, MmrBrandColors.Active.copy(alpha = 0.14f))
        FtueBulletIcon.ViewMode ->
            FtueBulletIconVisual(
                Icons.Filled.ArrowDropDown,
                onSurface,
                mutedChip,
            )
        FtueBulletIcon.Search ->
            FtueBulletIconVisual(Icons.Filled.Search, onSurface, mutedChip)
        FtueBulletIcon.ApprovalsInbox ->
            FtueBulletIconVisual(Icons.Outlined.Email, onSurface, mutedChip)
        FtueBulletIcon.SwipeMonth ->
            FtueBulletIconVisual(
                Icons.AutoMirrored.Filled.ArrowForward,
                onSurface,
                mutedChip,
            )
        FtueBulletIcon.MainStats ->
            navTabVisual(Screen.Stats.route, onSurface, mutedChip)
        FtueBulletIcon.MainBudgets ->
            navTabVisual(Screen.Budgets.route, onSurface, mutedChip)
        FtueBulletIcon.MainAssets ->
            navTabVisual(Screen.Assets.route, onSurface, mutedChip)
        FtueBulletIcon.MainScheduled ->
            navTabVisual(Screen.Scheduled.route, onSurface, mutedChip)
        FtueBulletIcon.MainSettings ->
            navTabVisual(Screen.Settings.route, onSurface, mutedChip)
        FtueBulletIcon.Backup ->
            FtueBulletIconVisual(Icons.Filled.Backup, onSurface, mutedChip)
        FtueBulletIcon.DriveBackup ->
            FtueBulletIconVisual(Icons.Filled.CloudUpload, onSurface, mutedChip)
        FtueBulletIcon.LanSync ->
            FtueBulletIconVisual(Icons.Filled.Sync, onSurface, mutedChip)
        FtueBulletIcon.WebManage ->
            FtueBulletIconVisual(Icons.Filled.Computer, onSurface, mutedChip)
        FtueBulletIcon.Configuration ->
            FtueBulletIconVisual(Icons.Filled.Palette, onSurface, mutedChip)
        FtueBulletIcon.Accounts ->
            FtueBulletIconVisual(Icons.Filled.AccountBalance, onSurface, mutedChip)
        FtueBulletIcon.Categories ->
            FtueBulletIconVisual(Icons.Filled.Category, onSurface, mutedChip)
    }
}

private fun navTabVisual(
    route: String,
    onSurface: Color,
    mutedChip: Color,
): FtueBulletIconVisual {
    val pair = mmrDestinationIconPair(route)
    return FtueBulletIconVisual(pair.filled, onSurface, mutedChip)
}
