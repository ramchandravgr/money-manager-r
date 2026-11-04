package com.mmr.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mmr.feature.account.ui.AccountListScreen
import com.mmr.feature.account.ui.AddEditAccountScreen
import com.mmr.feature.approval.ui.ApprovalCenterScreen
import com.mmr.feature.asset.ui.AddEditAssetScreen
import com.mmr.feature.asset.ui.AssetDetailScreen
import com.mmr.feature.asset.ui.AssetListScreen
import com.mmr.feature.backup.ui.BackupScreen
import com.mmr.feature.budget.ui.AddEditBudgetScreen
import com.mmr.feature.budget.ui.BudgetListScreen
import com.mmr.feature.category.ui.AddEditCategoryScreen
import com.mmr.feature.category.ui.AddEditSubcategoryScreen
import com.mmr.feature.category.ui.CategoryListScreen
import com.mmr.feature.drivebackup.ui.GoogleDriveBackupScreen
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.home.HomeScreen
import com.mmr.feature.lansync.ui.LanSyncScreen
import com.mmr.feature.loan.ui.AddEditLoanScreen
import com.mmr.feature.loan.ui.LoanDetailScreen
import com.mmr.feature.recurring.ui.AddEditRecurringRuleScreen
import com.mmr.feature.recurring.ui.RecurringDetailScreen
import com.mmr.feature.scheduled.model.ScheduledSection
import com.mmr.feature.scheduled.ui.ScheduledScreen
import com.mmr.feature.search.ui.TransactionSearchScreen
import com.mmr.feature.settings.ui.ConfigurationScreen
import com.mmr.feature.settings.ui.SettingsScreen
import com.mmr.feature.settings.ui.SmsTemplateOverlayScreen
import com.mmr.feature.stats.model.StatsCategoryDrillDown
import com.mmr.feature.stats.ui.StatsCategoryDetailScreen
import com.mmr.feature.stats.ui.StatsScreen
import com.mmr.feature.transaction.ui.AddEditTransactionRoute
import com.mmr.feature.transaction.ui.TransactionDetailScreen
import com.mmr.feature.webmanage.ui.WebManageScreen

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
                    navController.navigate(Screen.ApprovalCenter.createRoute())
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
                onNavigateToWebManage = {
                    navController.navigate(Screen.WebManage.route)
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route)
                },
                onNavigateToSmsTemplateOverlay = {
                    navController.navigate(Screen.SmsTemplateOverlay.route)
                },
            )
        }

        composable(Screen.SmsTemplateOverlay.route) {
            SmsTemplateOverlayScreen(onNavigateBack = { navController.popBackStack() })
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
                onNavigateToGoogleDrive = { navController.navigate(Screen.GoogleDriveBackup.route) },
            )
        }

        composable(Screen.GoogleDriveBackup.route) {
            GoogleDriveBackupScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.LanSync.route) {
            LanSyncScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHomeAfterRestore = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Screen.WebManage.route) {
            WebManageScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Scheduled.routeWithArgs,
            arguments =
                listOf(
                    navArgument(Screen.Scheduled.ARG_SECTION) {
                        type = NavType.StringType
                        defaultValue = "loans"
                    },
                ),
        ) { entry ->
            val section =
                ScheduledSection.fromRouteArg(
                    entry.arguments?.getString(Screen.Scheduled.ARG_SECTION),
                )
            scheduledScreen(
                navController = navController,
                initialSection = section,
            )
        }

        composable(Screen.Loans.route) {
            scheduledScreen(
                navController = navController,
                initialSection = ScheduledSection.LOANS,
            )
        }

        composable(Screen.Recurring.route) {
            scheduledScreen(
                navController = navController,
                initialSection = ScheduledSection.RECURRING,
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

        composable(Screen.Budgets.route) {
            BudgetListScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
                onNavigateToAddBudget = { navController.navigate(Screen.AddBudget.route) },
                onNavigateToEditBudget = { id ->
                    navController.navigate(Screen.EditBudget.createRoute(id))
                },
            )
        }

        composable(Screen.Assets.route) {
            AssetListScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
                onNavigateToAddAsset = { navController.navigate(Screen.AddAsset.route) },
                onNavigateToAssetDetail = { id ->
                    navController.navigate(Screen.AssetDetail.createRoute(id))
                },
            )
        }

        composable(Screen.AddAsset.route) {
            AddEditAssetScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.EditAsset.route,
            arguments =
                listOf(
                    navArgument("assetId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditAssetScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.AssetDetail.route,
            arguments =
                listOf(
                    navArgument("assetId") {
                        type = NavType.LongType
                    },
                ),
        ) {
            AssetDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { assetId ->
                    navController.navigate(Screen.EditAsset.createRoute(assetId))
                },
            )
        }

        composable(Screen.AddBudget.route) {
            AddEditBudgetScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.EditBudget.route,
            arguments =
                listOf(
                    navArgument("budgetId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditBudgetScreen(onNavigateBack = { navController.popBackStack() })
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

        composable(
            route = Screen.ApprovalCenter.routeWithArgs,
            arguments =
                listOf(
                    navArgument(Screen.ApprovalCenter.ARG_APPROVAL_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument(Screen.ApprovalCenter.ARG_ACTION) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
        ) { entry ->
            val launchApprovalId =
                entry.arguments?.getLong(Screen.ApprovalCenter.ARG_APPROVAL_ID) ?: 0L
            val launchAction =
                entry.arguments?.getString(Screen.ApprovalCenter.ARG_ACTION).orEmpty()
            ApprovalCenterScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = true,
                launchApprovalId = launchApprovalId,
                launchAction = launchAction,
                onNavigateToEditPending = { approvalId ->
                    navController.navigate(Screen.AddTransaction.createRoute(approvalId))
                },
            )
        }
    }
}

@Composable
private fun scheduledScreen(
    navController: NavController,
    initialSection: ScheduledSection,
) {
    ScheduledScreen(
        initialSection = initialSection,
        onNavigateToLoanDetail = { loanId ->
            navController.navigate(Screen.LoanDetail.createRoute(loanId))
        },
        onNavigateToAddLoan = {
            navController.navigate(Screen.AddLoan.route)
        },
        onNavigateToEditLoan = { loanId ->
            navController.navigate(Screen.EditLoan.createRoute(loanId))
        },
        onNavigateToRecurringDetail = { ruleId ->
            navController.navigate(Screen.RecurringDetail.createRoute(ruleId))
        },
        onNavigateToAddRecurringRule = {
            navController.navigate(Screen.AddRecurringRule.route)
        },
    )
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
