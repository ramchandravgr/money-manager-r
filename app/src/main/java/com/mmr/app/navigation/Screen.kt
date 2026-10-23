package com.mmr.app.navigation

sealed class Screen(
    val route: String,
) {
    object Home : Screen("home")

    object TransactionSearch : Screen("transaction_search") {
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_START_MILLIS = "startMillis"
        const val ARG_END_MILLIS = "endMillis"
        const val ARG_TRANSACTION_TYPE = "transactionType"
        const val ARG_SUBCATEGORY_ID = "subcategoryId"

        val routeWithArgs =
            "transaction_search?${ARG_CATEGORY_ID}={${ARG_CATEGORY_ID}}&${ARG_START_MILLIS}={${ARG_START_MILLIS}}&${ARG_END_MILLIS}={${ARG_END_MILLIS}}&${ARG_TRANSACTION_TYPE}={${ARG_TRANSACTION_TYPE}}&${ARG_SUBCATEGORY_ID}={${ARG_SUBCATEGORY_ID}}"

        fun createRoute(
            categoryId: Long = -1L,
            startMillis: Long = -1L,
            endMillis: Long = -1L,
            transactionType: String = "",
            subcategoryId: Long = -1L,
        ): String =
            "transaction_search?$ARG_CATEGORY_ID=$categoryId&$ARG_START_MILLIS=$startMillis&$ARG_END_MILLIS=$endMillis&$ARG_TRANSACTION_TYPE=$transactionType&$ARG_SUBCATEGORY_ID=$subcategoryId"
    }

    object Stats : Screen("stats")

    object StatsCategoryDetail : Screen("stats_category") {
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_TRANSACTION_TYPE = "transactionType"
        const val ARG_PERIOD_MODE = "periodMode"
        const val ARG_ANCHOR_MILLIS = "anchorMillis"
        const val ARG_CUSTOM_START_MILLIS = "customStartMillis"
        const val ARG_CUSTOM_END_MILLIS = "customEndMillis"

        val routeWithArgs =
            "stats_category?" +
                "${ARG_CATEGORY_ID}={${ARG_CATEGORY_ID}}&" +
                "${ARG_TRANSACTION_TYPE}={${ARG_TRANSACTION_TYPE}}&" +
                "${ARG_PERIOD_MODE}={${ARG_PERIOD_MODE}}&" +
                "${ARG_ANCHOR_MILLIS}={${ARG_ANCHOR_MILLIS}}&" +
                "${ARG_CUSTOM_START_MILLIS}={${ARG_CUSTOM_START_MILLIS}}&" +
                "${ARG_CUSTOM_END_MILLIS}={${ARG_CUSTOM_END_MILLIS}}"

        fun createRoute(
            categoryId: Long,
            transactionType: String,
            periodMode: String,
            anchorMillis: Long,
            customStartMillis: Long = -1L,
            customEndMillis: Long = -1L,
        ): String =
            "stats_category?" +
                "$ARG_CATEGORY_ID=$categoryId&" +
                "$ARG_TRANSACTION_TYPE=$transactionType&" +
                "$ARG_PERIOD_MODE=$periodMode&" +
                "$ARG_ANCHOR_MILLIS=$anchorMillis&" +
                "$ARG_CUSTOM_START_MILLIS=$customStartMillis&" +
                "$ARG_CUSTOM_END_MILLIS=$customEndMillis"
    }

    object AddTransaction : Screen("add_transaction") {
        const val ARG_APPROVAL_ID = "approvalId"
        const val ARG_DATE_MILLIS = "dateMillis"
        const val ARG_DUPLICATE_FROM_ID = "duplicateFromId"

        val routeWithArgs =
            "add_transaction?" +
                "$ARG_APPROVAL_ID={${ARG_APPROVAL_ID}}&" +
                "$ARG_DATE_MILLIS={${ARG_DATE_MILLIS}}&" +
                "$ARG_DUPLICATE_FROM_ID={${ARG_DUPLICATE_FROM_ID}}"

        fun createRoute(
            approvalId: Long = 0L,
            dateMillis: Long = -1L,
            duplicateFromId: Long = 0L,
        ): String =
            "add_transaction?" +
                "$ARG_APPROVAL_ID=$approvalId&" +
                "$ARG_DATE_MILLIS=$dateMillis&" +
                "$ARG_DUPLICATE_FROM_ID=$duplicateFromId"

        fun createDuplicateRoute(
            sourceTransactionId: Long,
            dateMillis: Long,
        ): String = createRoute(duplicateFromId = sourceTransactionId, dateMillis = dateMillis)
    }

    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long): String = "edit_transaction/$transactionId"
    }

    object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: Long): String = "transaction_detail/$transactionId"
    }

    object ApprovalCenter : Screen("approval_center")

    object Accounts : Screen("accounts")

    object AddAccount : Screen("add_account")

    object EditAccount : Screen("edit_account/{accountId}") {
        fun createRoute(accountId: Long): String = "edit_account/$accountId"
    }

    object Loans : Screen("loans")

    object AddLoan : Screen("add_loan")

    object EditLoan : Screen("edit_loan/{loanId}") {
        fun createRoute(loanId: Long): String = "edit_loan/$loanId"
    }

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

    object Configuration : Screen("configuration")

    object Backup : Screen("backup")

    object LanSync : Screen("lan_sync")

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
