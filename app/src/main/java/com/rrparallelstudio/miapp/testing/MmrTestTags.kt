package com.rrparallelstudio.miapp.testing

import com.rrparallelstudio.miapp.feature.home.model.HomeViewTab
import com.rrparallelstudio.miapp.feature.scheduled.model.ScheduledSection
import com.rrparallelstudio.miapp.navigation.Screen

/**
 * Stable Compose UI test selectors. Use dotted ids: screen.control.role.
 */
object MmrTestTags {
    fun navTab(route: String): String = "nav.tab.$route"

    val navHome = navTab(Screen.Home.route)
    val navStats = navTab(Screen.Stats.route)
    val navBudgets = navTab(Screen.Budgets.route)
    val navScheduled = navTab(Screen.Scheduled.route)
    val navLoans = navScheduled
    val navRecurring = navScheduled

    const val scheduledScreen = "scheduled.screen"

    fun scheduledSectionTab(section: ScheduledSection): String = "scheduled.tab.${section.name.lowercase()}"

    val navSettings = navTab(Screen.Settings.route)

    const val homeViewModeTrigger = "home.view_mode.trigger"

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
    const val backupMigrationImportRequest = "backup.migration.import_request"

    fun loanRow(id: Long): String = "loan.row.$id"

    const val loanDetailFabRepayment = "loan.detail.fab.repayment"
    const val loanRepaymentAmount = "loan.repayment.field.amount"
    const val loanRepaymentConfirm = "loan.repayment.confirm"

    const val approvalOpenSmsSetup = "approval.open_sms_setup"
    const val approvalScanSms = "approval.action.scan_sms"

    const val releaseNotesClose = "release_notes.action.close"

    const val ftueScreen = "ftue.screen"
    const val ftueSkip = "ftue.action.skip"
    const val ftueNext = "ftue.action.next"
    const val ftueDone = "ftue.action.done"

    const val settingsReplayIntroduction = "settings.nav.replay_introduction"

    const val adHomeDailyBanner = "ad.home.daily.banner"
    const val adHomeDailyMrec = "ad.home.daily.mrec"
    const val adStatsNative = "ad.stats.native"

    const val recurringFieldName = "recurring.field.name"

    fun approvalCard(approvalId: Long): String = "approval.card.$approvalId"

    fun approvalQuickApprove(approvalId: Long): String = "approval.$approvalId.approve"

    fun approvalLink(approvalId: Long): String = "approval.$approvalId.link"

    const val approvalSelectionLink = "approval.selection.link"

    const val approvalSelectionApprove = "approval.selection.approve"

    const val approvalSelectionReject = "approval.selection.reject"

    const val approvalSelectionSelectAll = "approval.selection.select_all"

    const val approvalLinkPickerSearch = "approval.link_picker.search"

    fun approvalQuickReject(approvalId: Long): String = "approval.$approvalId.reject"

    const val approvalFabManualSms = "approval.fab.manual_sms"

    const val homeSummaryIncome = "home.summary.income"
    const val homeSummaryExpense = "home.summary.expense"
    const val homeSummaryTotal = "home.summary.total"
    const val homeSummaryCarryOver = "home.summary.carry_over"
    const val homeCarryOverRow = "home.carry_over.row"

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
    const val settingsNavTransactionScan = "settings.nav.transaction_scan"
    const val settingsTransactionScanMonthsSlider = "settings.transaction_scan.months_slider"
    const val settingsTransactionScanBackupFolder = "settings.transaction_scan.backup_folder"
    const val settingsTransactionScanSbrGuide = "settings.transaction_scan.sbr_guide"
    const val settingsTransactionScanSbrGuideSheet = "settings.transaction_scan.sbr_guide.sheet"
    const val settingsTransactionScanSbrGuidePlayStore = "settings.transaction_scan.sbr_guide.play_store"
    const val settingsTransactionScanSbrGuideChooseFolder = "settings.transaction_scan.sbr_guide.choose_folder"
    const val settingsListAddAccounts = "settings.list.accounts.add"
    const val settingsListAddCategories = "settings.list.categories.add"
    const val accountFieldName = "account.field.name"
    const val categoryFieldName = "category.field.name"
    const val webManagePasswordField = "web_manage.password.field"
    const val webManagePasswordSave = "web_manage.password.save"
    const val webManageEnabledSwitch = "web_manage.switch.enabled"
    const val lanSyncEnabledSwitch = "lan_sync.switch.enabled"
    const val lanSyncPairingOfferValue = "lan_sync.pairing_offer.value"
    const val lanSyncPairingInput = "lan_sync.pairing.input"
    const val lanSyncTrustAdd = "lan_sync.action.trust_add"
    const val lanSyncSyncNow = "lan_sync.action.sync_now"
    const val lanSyncApprovePairPrefix = "lan_sync.approve."

    const val loanFabAdd = "loan.fab.add"
    const val loanFieldName = "loan.field.name"
    const val loanFieldLender = "loan.field.lender"
    const val loanFieldPrincipal = "loan.field.principal"

    const val recurringFabAdd = "recurring.fab.add"

    const val budgetListScreen = "budget.screen.list"
    const val budgetFabAdd = "budget.fab.add"

    fun budgetRow(id: Long): String = "budget.row.$id"

    const val appBarSave = "app_bar.save"
}
