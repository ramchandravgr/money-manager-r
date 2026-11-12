package com.rrparallelstudio.miapp.core.data.repository

import android.content.Context
import androidx.room.Room
import com.rrparallelstudio.miapp.budget.BudgetAlertNotifier
import com.rrparallelstudio.miapp.core.attachment.TransactionAttachmentStore
import com.rrparallelstudio.miapp.core.categorization.CategorizationRuleEngine
import com.rrparallelstudio.miapp.core.common.widget.NoOpHomeScreenWidgetRefreshTrigger
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncIdentityStore
import com.rrparallelstudio.miapp.core.data.repository.TransactionAttachmentRepositoryImpl
import com.rrparallelstudio.miapp.core.database.MmrDatabase
import com.rrparallelstudio.miapp.core.export.PeriodLedgerSpreadsheetImporter
import com.rrparallelstudio.miapp.core.lansync.LanSyncAccountBalanceValidator
import com.rrparallelstudio.miapp.core.lansync.LanSyncTombstoneRecorder
import com.rrparallelstudio.miapp.core.loan.EmiAutoPostCoordinator
import com.rrparallelstudio.miapp.core.loan.LoanCategoryResolver
import com.rrparallelstudio.miapp.core.loan.LoanLedgerCoordinator
import com.rrparallelstudio.miapp.core.loan.LoanTransactionCoordinator
import com.rrparallelstudio.miapp.core.monefy.MonefyLedgerImporter
import com.rrparallelstudio.miapp.core.moneymanager.MoneyManagerMmbakLedgerImporter
import com.rrparallelstudio.miapp.core.owner.MmrOwnerProfileCoordinator
import com.rrparallelstudio.miapp.core.parser.account.SmsAccountResolver
import com.rrparallelstudio.miapp.core.parser.engine.SmsParserEngine
import com.rrparallelstudio.miapp.core.parser.template.SmsTemplateRegistry
import com.rrparallelstudio.miapp.core.recurring.RecurringAutoPostCoordinator
import com.rrparallelstudio.miapp.core.transaction.TransactionMergeCoordinator

/**
 * Wires repository implementations the same way Hilt does, for instrumented tests on an in-memory DB.
 */
class MmrInstrumentedRepositoryGraph(
    context: Context,
) {
    val database: MmrDatabase =
        Room
            .inMemoryDatabaseBuilder(context, MmrDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    private val attachmentStore = TransactionAttachmentStore(context)

    private val lanSyncIdentityStore = LanSyncIdentityStore(context)

    val ownerProfileCoordinator =
        MmrOwnerProfileCoordinator(
            identityStore = lanSyncIdentityStore,
            ownerProfileDao = database.ownerProfileDao(),
        )

    private val tombstoneRecorder =
        LanSyncTombstoneRecorder(
            tombstoneDao = database.syncTombstoneDao(),
            identityStore = lanSyncIdentityStore,
        )

    private val displaySettingsStore = AppDisplaySettingsStore(context)

    private val budgetAlertNotifier = BudgetAlertNotifier(context)

    val budgetRepository: BudgetRepositoryImpl =
        BudgetRepositoryImpl(
            budgetDao = database.budgetDao(),
            categoryDao = database.categoryDao(),
            displaySettingsStore = displaySettingsStore,
            budgetAlertNotifier = budgetAlertNotifier,
            tombstoneRecorder = tombstoneRecorder,
            widgetRefreshTrigger = NoOpHomeScreenWidgetRefreshTrigger,
            ownerProfileCoordinator = ownerProfileCoordinator,
        )

    val transactionAttachmentRepository: TransactionAttachmentRepositoryImpl =
        TransactionAttachmentRepositoryImpl(
            context = context,
            database = database,
            attachmentDao = database.transactionAttachmentDao(),
            transactionDao = database.transactionDao(),
            store = attachmentStore,
            tombstoneRecorder = tombstoneRecorder,
        )

    private val loanTransactionCoordinator =
        LoanTransactionCoordinator(
            database = database,
            loanPaymentDao = database.loanPaymentDao(),
            loanDao = database.loanDao(),
            loanScheduleDao = database.loanScheduleDao(),
            transactionDao = database.transactionDao(),
            attachmentRepository = transactionAttachmentRepository,
            tombstoneRecorder = tombstoneRecorder,
        )

    val transactionMergeCoordinator =
        TransactionMergeCoordinator(
            database = database,
            transactionDao = database.transactionDao(),
            smsMessageRecordDao = database.smsMessageRecordDao(),
            loanPaymentDao = database.loanPaymentDao(),
            loanTransactionCoordinator = loanTransactionCoordinator,
        )

    val accountRepository: AccountRepositoryImpl =
        AccountRepositoryImpl(
            database = database,
            accountDao = database.accountDao(),
            transactionDao = database.transactionDao(),
            recurringRuleDao = database.recurringRuleDao(),
            loanDao = database.loanDao(),
            tombstoneRecorder = tombstoneRecorder,
        )

    val smsAccountOverrideStore =
        com.rrparallelstudio.miapp.core.parser.account.SmsAccountOverrideStore(
            overrideDao = database.smsAccountOverrideDao(),
            accountDao = database.accountDao(),
            transactionApprovalDao = database.transactionApprovalDao(),
        )

    val transactionRepository: TransactionRepositoryImpl =
        TransactionRepositoryImpl(
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao(),
            loanTransactionCoordinator = loanTransactionCoordinator,
            transactionMergeCoordinator = transactionMergeCoordinator,
            smsMessageRecordDao = database.smsMessageRecordDao(),
            widgetRefreshTrigger = NoOpHomeScreenWidgetRefreshTrigger,
            budgetRepository = budgetRepository,
            ownerProfileCoordinator = ownerProfileCoordinator,
            smsAccountOverrideStore = smsAccountOverrideStore,
        )

    val categoryRepository: CategoryRepositoryImpl =
        CategoryRepositoryImpl(
            database = database,
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            recurringRuleDao = database.recurringRuleDao(),
            categorizationRuleDao = database.categorizationRuleDao(),
            tombstoneRecorder = tombstoneRecorder,
        )

    val moneyManagerMmbakLedgerImporter: MoneyManagerMmbakLedgerImporter =
        MoneyManagerMmbakLedgerImporter(
            transactionDao = database.transactionDao(),
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
        )

    val monefyLedgerImporter: MonefyLedgerImporter =
        MonefyLedgerImporter(
            transactionDao = database.transactionDao(),
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            balanceValidator = LanSyncAccountBalanceValidator(database),
        )

    val periodLedgerSpreadsheetImporter: PeriodLedgerSpreadsheetImporter =
        PeriodLedgerSpreadsheetImporter(
            transactionDao = database.transactionDao(),
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            balanceValidator = LanSyncAccountBalanceValidator(database),
        )

    private val loanCategoryResolver =
        LoanCategoryResolver(categoryDao = database.categoryDao())

    val loanLedgerCoordinator =
        LoanLedgerCoordinator(
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            loanCategoryResolver = loanCategoryResolver,
        )

    val emiAutoPostCoordinator =
        EmiAutoPostCoordinator(
            loanScheduleDao = database.loanScheduleDao(),
            loanDao = database.loanDao(),
            transactionDao = database.transactionDao(),
            loanLedgerCoordinator = loanLedgerCoordinator,
        )

    val loanRepository: LoanRepositoryImpl =
        LoanRepositoryImpl(
            database = database,
            loanDao = database.loanDao(),
            loanPaymentDao = database.loanPaymentDao(),
            loanInterestRevisionDao = database.loanInterestRevisionDao(),
            loanScheduleDao = database.loanScheduleDao(),
            transactionDao = database.transactionDao(),
            loanLedgerCoordinator = loanLedgerCoordinator,
            accountRepository = accountRepository,
            tombstoneRecorder = tombstoneRecorder,
            ownerProfileCoordinator = ownerProfileCoordinator,
        )

    val recurringRuleRepository: RecurringRuleRepositoryImpl =
        RecurringRuleRepositoryImpl(
            database = database,
            recurringRuleDao = database.recurringRuleDao(),
            notificationRuleDao = database.notificationRuleDao(),
            tombstoneRecorder = tombstoneRecorder,
            ownerProfileCoordinator = ownerProfileCoordinator,
        )

    val recurringAutoPostCoordinator: RecurringAutoPostCoordinator =
        RecurringAutoPostCoordinator(
            recurringRuleRepository = recurringRuleRepository,
            transactionRepository = transactionRepository,
            transactionDao = database.transactionDao(),
            loanDao = database.loanDao(),
        )

    val approvalRepository: ApprovalRepositoryImpl =
        ApprovalRepositoryImpl(
            transactionApprovalDao = database.transactionApprovalDao(),
            approvalLedgerDao = database.approvalLedgerDao(),
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            smsAccountResolver = SmsAccountResolver(database.accountDao()),
            smsParserEngine =
                SmsParserEngine(
                    SmsTemplateRegistry.fromTemplates(SmsTemplateRegistry.loadBuiltInFromAssets(context)),
                ),
            loanTransactionCoordinator = loanTransactionCoordinator,
            categorizationRuleEngine = CategorizationRuleEngine(database.categorizationRuleDao()),
            widgetRefreshTrigger = NoOpHomeScreenWidgetRefreshTrigger,
            ownerProfileCoordinator = ownerProfileCoordinator,
            smsAccountOverrideStore = smsAccountOverrideStore,
        )

    fun close() {
        database.close()
    }
}
