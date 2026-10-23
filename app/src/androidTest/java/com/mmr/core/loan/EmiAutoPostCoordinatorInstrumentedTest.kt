package com.mmr.core.loan

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.data.repository.MmrInstrumentedRepositoryGraph
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EmiAutoPostCoordinatorInstrumentedTest {
    private lateinit var graph: MmrInstrumentedRepositoryGraph

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        graph = MmrInstrumentedRepositoryGraph(context)
    }

    @After
    fun tearDown() {
        graph.close()
    }

    @Test
    fun postDueEmiLedgerEntries_createsTransferForPlannedEmi() =
        runBlocking {
            graph.database.accountDao().insert(
                AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, isDefault = true),
            )
            val principal = 12_000.0
            val tenure = 12
            val rate = 0.0
            val emi = LoanAmortizationEngine.computeEmi(principal, rate, tenure)
            val openingDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(45)

            val loanId =
                graph.loanRepository.createLoan(
                    LoanEntity(
                        name = "Auto EMI loan",
                        lenderName = "Bank",
                        principalAmount = principal,
                        interestRate = rate,
                        outstandingAmount = principal,
                        openingDate = openingDate,
                        loanType = LoanType.BORROWED,
                        status = LoanStatus.ACTIVE,
                        repaymentMode = LoanRepaymentMode.FIXED_EMI.name,
                        tenureMonths = tenure,
                        remainingTenureMonths = tenure,
                        fixedEmiAmount = emi,
                        emiDayOfMonth = 1,
                    ),
                )
            assertTrue(loanId > 0L)

            val created = graph.emiAutoPostCoordinator.postDueEmiLedgerEntries()
            assertTrue(created >= 1)
        }
}
