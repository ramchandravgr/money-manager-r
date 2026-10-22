package com.mmr.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mmr.feature.approval.ui.ApprovalCenterScreen
import com.mmr.feature.backup.ui.BackupScreen
import com.mmr.feature.category.ui.AddEditCategoryScreen
import com.mmr.feature.category.ui.AddEditSubcategoryScreen
import com.mmr.feature.category.ui.CategoryListScreen
import com.mmr.feature.home.HomeScreen
import com.mmr.feature.loan.ui.AddEditLoanScreen
import com.mmr.feature.loan.ui.LoanDetailScreen
import com.mmr.feature.loan.ui.LoanListScreen
import com.mmr.feature.recurring.ui.AddEditRecurringRuleScreen
import com.mmr.feature.recurring.ui.RecurringDetailScreen
import com.mmr.feature.recurring.ui.RecurringListScreen
import com.mmr.feature.search.ui.TransactionSearchScreen
import com.mmr.feature.settings.ui.SettingsScreen
import com.mmr.feature.stats.ui.StatsScreen
import com.mmr.feature.transaction.ui.AddEditTransactionRoute

@Composable
fun MmrNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                },
                onNavigateToApprovals = {
                    navController.navigate(Screen.ApprovalCenter.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.TransactionSearch.route)
                }
            )
        }

        composable(Screen.TransactionSearch.route) {
            TransactionSearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
                onNavigateToBackup = {
                    navController.navigate(Screen.Backup.route)
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                }
            )
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
                }
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
                }
            )
        }

        composable(
            route = Screen.EditCategory.route,
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            AddEditCategoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddSubcategory = { categoryId ->
                    navController.navigate(Screen.AddSubcategory.createRoute(categoryId))
                },
                onNavigateToEditSubcategory = { subcategoryId ->
                    navController.navigate(Screen.EditSubcategory.createRoute(subcategoryId))
                }
            )
        }

        composable(
            route = Screen.AddSubcategory.route,
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            AddEditSubcategoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditSubcategory.route,
            arguments = listOf(
                navArgument("subcategoryId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            AddEditSubcategoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
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
                }
            )
        }

        composable(Screen.AddRecurringRule.route) {
            AddEditRecurringRuleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditRecurringRule.route,
            arguments = listOf(
                navArgument("ruleId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            AddEditRecurringRuleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RecurringDetail.route,
            arguments = listOf(
                navArgument("ruleId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            RecurringDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { ruleId ->
                    navController.navigate(Screen.EditRecurringRule.createRoute(ruleId))
                }
            )
        }

        composable(Screen.Loans.route) {
            LoanListScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
                onNavigateToLoanDetail = { loanId ->
                    navController.navigate(Screen.LoanDetail.createRoute(loanId))
                },
                onNavigateToAddLoan = {
                    navController.navigate(Screen.AddLoan.route)
                }
            )
        }

        composable(Screen.AddLoan.route) {
            AddEditLoanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LoanDetail.route,
            arguments = listOf(
                navArgument("loanId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            LoanDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false
            )
        }

        composable(
            route = "add_transaction?approvalId={approvalId}",
            arguments = listOf(
                navArgument("approvalId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            AddEditTransactionRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            AddEditTransactionRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ApprovalCenter.route) {
            ApprovalCenterScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = true,
                onNavigateToEditPending = { approvalId ->
                    navController.navigate(Screen.AddTransaction.createRoute(approvalId))
                }
            )
        }
    }
}
