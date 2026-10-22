package com.mmr.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoanRepositoryInstrumentedTest {

    private lateinit var database: MmrDatabase
    private lateinit var repository: LoanRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MmrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LoanRepositoryImpl(
            database = database,
            loanDao = database.loanDao(),
            loanPaymentDao = database.loanPaymentDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun recordLoanPayment_updatesOutstandingAndClosesWhenFullyPaid() = runBlocking {
        val loanId = repository.createLoan(
            LoanEntity(
                name = "Personal",
                lenderName = "Bank",
                principalAmount = 1000.0,
                interestRate = 0.0,
                outstandingAmount = 1000.0,
                openingDate = System.currentTimeMillis(),
                loanType = LoanType.BORROWED,
                status = LoanStatus.ACTIVE
            )
        )

        repository.recordLoanPayment(
            loanId = loanId,
            paymentAmount = 400.0,
            paymentType = LoanPaymentType.PART_PAYMENT
        )
        var loan = repository.getLoanById(loanId)!!
        assertEquals(600.0, loan.outstandingAmount, 0.001)
        assertEquals(LoanStatus.ACTIVE, loan.status)

        repository.recordLoanPayment(
            loanId = loanId,
            paymentAmount = 600.0,
            paymentType = LoanPaymentType.CLOSING_PAYMENT
        )
        loan = repository.getLoanById(loanId)!!
        assertEquals(0.0, loan.outstandingAmount, 0.001)
        assertEquals(LoanStatus.CLOSED, loan.status)
        assertEquals(2, database.loanPaymentDao().getPaymentsForLoan(loanId).first().size)
    }

    @Test
    fun recordLoanPayment_rejectsInvalidAmount() {
        runBlocking {
            val loanId = repository.createLoan(
                LoanEntity(
                    name = "Personal",
                    lenderName = "Bank",
                    principalAmount = 500.0,
                    interestRate = 0.0,
                    outstandingAmount = 500.0,
                    openingDate = System.currentTimeMillis(),
                    loanType = LoanType.BORROWED
                )
            )

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.recordLoanPayment(loanId, 0.0, LoanPaymentType.PART_PAYMENT)
                }
            }
        }
    }
}
