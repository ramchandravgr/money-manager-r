package com.mmr.app.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object TransactionSearch : Screen("transaction_search")
    object Stats : Screen("stats")
    object AddTransaction : Screen("add_transaction") {
        fun createRoute(approvalId: Long): String = "add_transaction?approvalId=$approvalId"
    }
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long): String = "edit_transaction/$transactionId"
    }
    object ApprovalCenter : Screen("approval_center")
    object Accounts : Screen("accounts")
    object Loans : Screen("loans")
    object AddLoan : Screen("add_loan")
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long): String = "loan_detail/$loanId"
    }
    object Recurring : Screen("recurring")
    object AddRecurringRule : Screen("add_recurring")
    object EditRecurringRule : Screen("edit_recurring/{ruleId}") {
        fun createRoute(ruleId: Long): String = "edit_recurring/$ruleId"
    }
    object RecurringDetail : Screen("recurring_detail/{ruleId}") {
        fun createRoute(ruleId: Long): String = "recurring_detail/$ruleId"
    }
    object Settings : Screen("settings")
    object Backup : Screen("backup")
    object Categories : Screen("categories")
    object AddCategory : Screen("add_category")
    object EditCategory : Screen("edit_category/{categoryId}") {
        fun createRoute(categoryId: Long): String = "edit_category/$categoryId"
    }

    object AddSubcategory : Screen("add_subcategory/{categoryId}") {
        fun createRoute(categoryId: Long): String = "add_subcategory/$categoryId"
    }

    object EditSubcategory : Screen("edit_subcategory/{subcategoryId}") {
        fun createRoute(subcategoryId: Long): String = "edit_subcategory/$subcategoryId"
    }
}
