package com.mmr.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mmr.feature.account.ui.AccountListScreen
import com.mmr.feature.account.ui.AddEditAccountScreen
import com.mmr.feature.approval.ui.ApprovalCenterScreen
import com.mmr.feature.backup.ui.BackupScreen
import com.mmr.feature.category.ui.AddEditCategoryScreen
import com.mmr.feature.category.ui.AddEditSubcategoryScreen
import com.mmr.feature.category.ui.CategoryListScreen
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.home.HomeScreen
import com.mmr.feature.lansync.ui.LanSyncScreen
import com.mmr.feature.loan.ui.AddEditLoanScreen
import com.mmr.feature.loan.ui.LoanDetailScreen
import com.mmr.feature.loan.ui.LoanListScreen
import com.mmr.feature.recurring.ui.AddEditRecurringRuleScreen
import com.mmr.feature.recurring.ui.RecurringDetailScreen
import com.mmr.feature.recurring.ui.RecurringListScreen
import com.mmr.feature.search.ui.TransactionSearchScreen
import com.mmr.feature.settings.ui.ConfigurationScreen
import com.mmr.feature.settings.ui.SettingsScreen
import com.mmr.feature.stats.model.StatsCategoryDrillDown
import com.mmr.feature.stats.ui.StatsCategoryDetailScreen
import com.mmr.feature.stats.ui.StatsScreen
import com.mmr.feature.transaction.ui.AddEditTransactionRoute
import com.mmr.feature.transaction.ui.TransactionDetailScreen

@Composable
fun MmrNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddTransaction = { prefillDateMillis ->
                    navController.navigate(
                        Screen.AddTransaction.createRoute(
                            approvalId = 0L,
                            dateMillis = prefillDateMillis ?: -1L,
                        ),
                    )
                },
                onNavigateToTransactionDetail = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                },
                onNavigateToApprovals = {
                    navController.navigate(Screen.ApprovalCenter.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.TransactionSearch.createRoute())
                },
            )
        }

        composable(
            route = Screen.TransactionSearch.routeWithArgs,
            arguments =
                listOf(
                    navArgument(Screen.TransactionSearch.ARG_CATEGORY_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Screen.TransactionSearch.ARG_START_MILLIS) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Screen.TransactionSearch.ARG_END_MILLIS) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Screen.TransactionSearch.ARG_TRANSACTION_TYPE) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(Screen.TransactionSearch.ARG_SUBCATEGORY_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
        ) {
            TransactionSearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                },
            )
        }

        composable(
            route = Screen.TransactionDetail.route,
            arguments =
                listOf(
                    navArgument("transactionId") {
                        type = NavType.LongType
                    },
                ),
        ) {
            TransactionDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                },
                onNavigateToDuplicate = { sourceId, dateMillis ->
                    navController.navigate(
                        Screen.AddTransaction.createDuplicateRoute(sourceId, dateMillis),
                    ) {
                        popUpTo(Screen.TransactionDetail.createRoute(sourceId)) {
                            inclusive = true
                        }
                    }
                },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
                onNavigateToConfiguration = {
                    navController.navigate(Screen.Configuration.route)
                },
                onNavigateToBackup = {
                    navController.navigate(Screen.Backup.route)
                },
                onNavigateToLanSync = {
                    navController.navigate(Screen.LanSync.route)
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route)
                },
            )
        }

        composable(Screen.Configuration.route) {
            ConfigurationScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Accounts.route) {
            AccountListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddAccount = { navController.navigate(Screen.AddAccount.route) },
                onNavigateToEditAccount = { id ->
                    navController.navigate(Screen.EditAccount.createRoute(id))
                },
            )
        }

        composable(Screen.AddAccount.route) {
            AddEditAccountScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.EditAccount.route,
            arguments =
                listOf(
                    navArgument("accountId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditAccountScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Categories.route) {
            CategoryListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddCategory = { navController.navigate(Screen.AddCategory.route) },
                onNavigateToEditCategory = { categoryId ->
                    navController.navigate(Screen.EditCategory.createRoute(categoryId))
                },
                onNavigateToAddSubcategory = { categoryId ->
                    navController.navigate(Screen.AddSubcategory.createRoute(categoryId))
                },
                onNavigateToEditSubcategory = { subcategoryId ->
                    navController.navigate(Screen.EditSubcategory.createRoute(subcategoryId))
                },
            )
        }

        composable(Screen.AddCategory.route) {
            AddEditCategoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddSubcategory = { categoryId ->
                    navController.navigate(Screen.AddSubcategory.createRoute(categoryId))
                },
                onNavigateToEditSubcategory = { subcategoryId ->
                    navController.navigate(Screen.EditSubcategory.createRoute(subcategoryId))
                },
            )
        }

        composable(
            route = Screen.EditCategory.route,
            arguments =
                listOf(
                    navArgument("categoryId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditCategoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddSubcategory = { categoryId ->
                    navController.navigate(Screen.AddSubcategory.createRoute(categoryId))
                },
                onNavigateToEditSubcategory = { subcategoryId ->
                    navController.navigate(Screen.EditSubcategory.createRoute(subcategoryId))
                },
            )
        }

        composable(
            route = Screen.AddSubcategory.route,
            arguments =
                listOf(
                    navArgument("categoryId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditSubcategoryScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.EditSubcategory.route,
            arguments =
                listOf(
                    navArgument("subcategoryId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditSubcategoryScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.LanSync.route) {
            LanSyncScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Recurring.route) {
            RecurringListScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
                onNavigateToDetail = { ruleId ->
                    navController.navigate(Screen.RecurringDetail.createRoute(ruleId))
                },
                onNavigateToAddRule = {
                    navController.navigate(Screen.AddRecurringRule.route)
                },
            )
        }

        composable(Screen.AddRecurringRule.route) {
            AddEditRecurringRuleScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.EditRecurringRule.route,
            arguments =
                listOf(
                    navArgument("ruleId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditRecurringRuleScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.RecurringDetail.route,
            arguments =
                listOf(
                    navArgument("ruleId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            RecurringDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { ruleId ->
                    navController.navigate(Screen.EditRecurringRule.createRoute(ruleId))
                },
            )
        }

        composable(Screen.Loans.route) {
            LoanListScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
                onNavigateToLoanDetail = { loanId ->
                    navController.navigate(Screen.LoanDetail.createRoute(loanId))
                },
                onNavigateToEditLoan = { loanId ->
                    navController.navigate(Screen.EditLoan.createRoute(loanId))
                },
                onNavigateToAddLoan = {
                    navController.navigate(Screen.AddLoan.route)
                },
            )
        }

        composable(Screen.AddLoan.route) {
            AddEditLoanScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.EditLoan.route,
            arguments =
                listOf(
                    navArgument("loanId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditLoanScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.LoanDetail.route,
            arguments =
                listOf(
                    navArgument("loanId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            LoanDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditLoan = { loanId ->
                    navController.navigate(Screen.EditLoan.createRoute(loanId))
                },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                },
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
                onNavigateToCategoryDetail = { drill ->
                    navController.navigate(statsCategoryDetailRoute(drill))
                },
            )
        }

        composable(
            route = Screen.StatsCategoryDetail.routeWithArgs,
            arguments =
                listOf(
                    navArgument(Screen.StatsCategoryDetail.ARG_CATEGORY_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument(Screen.StatsCategoryDetail.ARG_TRANSACTION_TYPE) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(Screen.StatsCategoryDetail.ARG_PERIOD_MODE) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(Screen.StatsCategoryDetail.ARG_ANCHOR_MILLIS) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Screen.StatsCategoryDetail.ARG_CUSTOM_START_MILLIS) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Screen.StatsCategoryDetail.ARG_CUSTOM_END_MILLIS) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
        ) {
            StatsCategoryDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTransaction = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                },
                onSwitchCategory = { newCategoryId ->
                    val entry = navController.currentBackStackEntry
                    val type = entry?.arguments?.getString(Screen.StatsCategoryDetail.ARG_TRANSACTION_TYPE) ?: ""
                    val periodMode = entry?.arguments?.getString(Screen.StatsCategoryDetail.ARG_PERIOD_MODE) ?: ""
                    val anchorMillis = entry?.arguments?.getLong(Screen.StatsCategoryDetail.ARG_ANCHOR_MILLIS) ?: -1L
                    val customStart =
                        entry?.arguments?.getLong(Screen.StatsCategoryDetail.ARG_CUSTOM_START_MILLIS) ?: -1L
                    val customEnd = entry?.arguments?.getLong(Screen.StatsCategoryDetail.ARG_CUSTOM_END_MILLIS) ?: -1L
                    navController.navigate(
                        Screen.StatsCategoryDetail.createRoute(
                            categoryId = newCategoryId,
                            transactionType = type,
                            periodMode = periodMode,
                            anchorMillis = anchorMillis,
                            customStartMillis = customStart,
                            customEndMillis = customEnd,
                        ),
                    ) {
                        popUpTo(Screen.StatsCategoryDetail.routeWithArgs) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Screen.AddTransaction.routeWithArgs,
            arguments =
                listOf(
                    navArgument(Screen.AddTransaction.ARG_APPROVAL_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument(Screen.AddTransaction.ARG_DATE_MILLIS) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Screen.AddTransaction.ARG_DUPLICATE_FROM_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditTransactionRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments =
                listOf(
                    navArgument("transactionId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditTransactionRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.ApprovalCenter.route) {
            ApprovalCenterScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = true,
                onNavigateToEditPending = { approvalId ->
                    navController.navigate(Screen.AddTransaction.createRoute(approvalId))
                },
            )
        }
    }
}

private fun statsCategoryDetailRoute(drill: StatsCategoryDrillDown): String {
    val zoneId = java.time.ZoneId.systemDefault()
    return Screen.StatsCategoryDetail.createRoute(
        categoryId = drill.categoryId,
        transactionType = drill.transactionType.name,
        periodMode = drill.periodMode.name,
        anchorMillis =
            drill.anchorDate
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli(),
        customStartMillis = drill.customStartDate?.let { HomePeriodUtils.dayStartMillis(it, zoneId) } ?: -1L,
        customEndMillis = drill.customEndDate?.let { HomePeriodUtils.dayEndMillis(it, zoneId) } ?: -1L,
    )
}
