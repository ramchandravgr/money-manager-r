package com.mmr.core.data.repository

import android.content.Context
import androidx.room.Room
import com.mmr.core.categorization.CategorizationRuleEngine
import com.mmr.core.database.MmrDatabase
import com.mmr.core.loan.EmiAutoPostCoordinator
import com.mmr.core.loan.LoanCategoryResolver
import com.mmr.core.loan.LoanLedgerCoordinator
import com.mmr.core.loan.LoanTransactionCoordinator
import com.mmr.core.parser.account.SmsAccountResolver
import com.mmr.core.parser.engine.SmsParserEngine
import com.mmr.core.parser.template.SmsTemplateRegistry
import com.mmr.core.transaction.TransactionMergeCoordinator

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

    private val loanTransactionCoordinator =
        LoanTransactionCoordinator(
            database = database,
            loanPaymentDao = database.loanPaymentDao(),
            loanDao = database.loanDao(),
            loanScheduleDao = database.loanScheduleDao(),
            transactionDao = database.transactionDao(),
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
        )

    val transactionRepository: TransactionRepositoryImpl =
        TransactionRepositoryImpl(
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao(),
            loanTransactionCoordinator = loanTransactionCoordinator,
            transactionMergeCoordinator = transactionMergeCoordinator,
            smsMessageRecordDao = database.smsMessageRecordDao(),
        )

    private val loanCategoryResolver = LoanCategoryResolver(categoryDao = database.categoryDao())

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
        )

    val approvalRepository: ApprovalRepositoryImpl =
        ApprovalRepositoryImpl(
            transactionApprovalDao = database.transactionApprovalDao(),
            approvalLedgerDao = database.approvalLedgerDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            smsAccountResolver = SmsAccountResolver(database.accountDao()),
            smsParserEngine = SmsParserEngine(SmsTemplateRegistry(context)),
            loanTransactionCoordinator = loanTransactionCoordinator,
            categorizationRuleEngine = CategorizationRuleEngine(database.categorizationRuleDao()),
        )

    fun close() {
        database.close()
    }
}
