package com.mmr.app.testing

import com.mmr.app.navigation.Screen
import com.mmr.feature.home.model.HomeViewTab

/**
 * Stable Compose UI test selectors. Use dotted ids: screen.control.role.
 */
object MmrTestTags {
    fun navTab(route: String): String = "nav.tab.$route"

    val navHome = navTab(Screen.Home.route)
    val navStats = navTab(Screen.Stats.route)
    val navLoans = navTab(Screen.Loans.route)
    val navRecurring = navTab(Screen.Recurring.route)
    val navSettings = navTab(Screen.Settings.route)

    fun homeViewTab(tab: HomeViewTab): String = "home.tab.${tab.name.lowercase()}"

    const val homeFabAdd = "home.fab.add"
    const val homeTopBarSearch = "home.top_bar.search"
    const val homeTopBarApprovals = "home.top_bar.approvals"
    const val homeExportExcel = "home.total.export_excel"
    const val homeCalendarWeekdayHeader = "home.calendar.weekday_header"
    const val homeSelectionMerge = "home.selection.merge"
    const val homeSelectionDelete = "home.selection.delete"

    fun transactionRow(id: Long): String = "transaction.row.$id"

    const val transactionDetailEdit = "transaction.detail.edit"
    const val transactionDetailDuplicate = "transaction.detail.duplicate"
    const val transactionDetailDelete = "transaction.detail.delete"
    const val transactionDuplicateToday = "transaction.duplicate.today"
    const val transactionDuplicateSameDate = "transaction.duplicate.same_date"

    const val backupExport = "backup.action.export"
    const val googleDriveBackupLink = "backup.action.google_drive"
    const val googleDriveBackupScreen = "backup.screen.google_drive"
    const val googleDriveBackupTurnOn = "backup.action.google_drive_turn_on"
    const val googleDriveBackupNow = "backup.action.google_drive_backup_now"
    const val backupImport = "backup.action.import"
    const val backupImportConfirm = "backup.import.confirm"
    const val backupImportModeReplace = "backup.import.mode.replace"
    const val backupImportModeMerge = "backup.import.mode.merge"

    fun loanRow(id: Long): String = "loan.row.$id"

    const val loanDetailFabRepayment = "loan.detail.fab.repayment"
    const val loanRepaymentAmount = "loan.repayment.field.amount"
    const val loanRepaymentConfirm = "loan.repayment.confirm"

    const val approvalScanSms = "approval.action.scan_sms"

    const val recurringFieldName = "recurring.field.name"

    fun approvalCard(approvalId: Long): String = "approval.card.$approvalId"

    fun approvalQuickApprove(approvalId: Long): String = "approval.$approvalId.approve"

    fun approvalLink(approvalId: Long): String = "approval.$approvalId.link"

    const val approvalSelectionLink = "approval.selection.link"

    const val approvalSelectionSelectAll = "approval.selection.select_all"

    const val approvalLinkPickerSearch = "approval.link_picker.search"

    fun approvalQuickReject(approvalId: Long): String = "approval.$approvalId.reject"

    const val approvalFabManualSms = "approval.fab.manual_sms"

    const val homeSummaryIncome = "home.summary.income"
    const val homeSummaryExpense = "home.summary.expense"
    const val homeSummaryTotal = "home.summary.total"

    const val searchNoteQuery = "search.field.note_query"
    const val searchPeriodChip = "search.header.period_chip"

    const val transactionAmount = "transaction.field.amount"
    const val transactionNote = "transaction.field.note"

    const val transactionTypeExpense = "transaction.type.expense"
    const val transactionTypeIncome = "transaction.type.income"
    const val transactionTypeTransfer = "transaction.type.transfer"

    const val statsTabIncome = "stats.tab.income"
    const val statsTabExpense = "stats.tab.expense"

    const val settingsNavAccounts = "settings.nav.accounts"
    const val settingsNavCategories = "settings.nav.categories"
    const val settingsNavBackup = "settings.nav.backup"
    const val settingsNavLanSync = "settings.nav.lan_sync"
    const val settingsNavWebManage = "settings.nav.web_manage"
    const val settingsListAddAccounts = "settings.list.accounts.add"
    const val settingsListAddCategories = "settings.list.categories.add"
    const val accountFieldName = "account.field.name"
    const val categoryFieldName = "category.field.name"
    const val webManagePasswordField = "web_manage.password.field"
    const val webManagePasswordSave = "web_manage.password.save"
    const val webManageEnabledSwitch = "web_manage.switch.enabled"
    const val lanSyncEnabledSwitch = "lan_sync.switch.enabled"

    const val loanFabAdd = "loan.fab.add"
    const val loanFieldName = "loan.field.name"
    const val loanFieldLender = "loan.field.lender"
    const val loanFieldPrincipal = "loan.field.principal"

    const val recurringFabAdd = "recurring.fab.add"

    const val appBarSave = "app_bar.save"
}
