package com.mmr.core.loan

import com.mmr.core.database.dao.LoanScheduleDao
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanInterestRevisionEntity
import com.mmr.core.model.enums.LoanPartPaymentAdjustment
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanTopUpAdjustment
import java.util.Calendar
import java.util.TimeZone

object LoanSchedulePlanner {
    fun nextEmiDueMillis(
        emiDayOfMonth: Int?,
        fromMillis: Long,
    ): Long {
        val day = emiDayOfMonth?.coerceIn(1, 28) ?: 1
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = fromMillis }
        cal.set(Calendar.DAY_OF_MONTH, day)
        if (cal.timeInMillis <= fromMillis) {
            cal.add(Calendar.MONTH, 1)
        }
        setEmiDayOnCalendar(cal, day)
        return cal.timeInMillis
    }

    /** Next EMI date: same day-of-month, one calendar month later (not +30 days). */
    fun advanceEmiDueOneMonth(
        dueMillis: Long,
        emiDayOfMonth: Int,
    ): Long {
        val day = emiDayOfMonth.coerceIn(1, 28)
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = dueMillis }
        cal.add(Calendar.MONTH, 1)
        setEmiDayOnCalendar(cal, day)
        return cal.timeInMillis
    }

    private fun setEmiDayOnCalendar(
        cal: Calendar,
        emiDayOfMonth: Int,
    ) {
        val day = emiDayOfMonth.coerceIn(1, 28)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, minOf(day, maxDay))
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    suspend fun regenerateFixedEmiSchedule(
        loan: LoanEntity,
        loanScheduleDao: LoanScheduleDao,
        scheduleAnchorMillis: Long = System.currentTimeMillis(),
        firstPeriodPrincipalStep: PrincipalStepAccrual? = null,
    ) {
        if (loan.repaymentModeEnum() != LoanRepaymentMode.FIXED_EMI) return
        val emi = loan.fixedEmiAmount ?: return
        val tenure = loan.remainingTenureMonths ?: return
        if (loan.outstandingAmount <= 0.0 || tenure <= 0) return

        loanScheduleDao.deletePlannedFrom(loan.id, scheduleAnchorMillis)
        val startSeq = loanScheduleDao.maxSequenceNumber(loan.id) + 1
        val emiDay = (loan.emiDayOfMonth ?: 1).coerceIn(1, 28)
        val firstDue = nextEmiDueMillis(emiDay, scheduleAnchorMillis)
        val periodStart = firstPeriodPrincipalStep?.periodStartMillis ?: scheduleAnchorMillis
        val entries =
            LoanAmortizationEngine.buildPlannedSchedule(
                loanId = loan.id,
                principalOutstanding = loan.outstandingAmount,
                annualRatePercent = loan.interestRate,
                emiAmount = emi,
                remainingTenureMonths = tenure,
                firstDueMillis = firstDue,
                emiDayOfMonth = emiDay,
                periodStartMillis = periodStart,
                startSequence = startSeq,
                firstPeriodPrincipalStep = firstPeriodPrincipalStep,
            )
        if (entries.isNotEmpty()) {
            loanScheduleDao.insertAll(entries)
        }
    }

    fun applyPartPaymentAdjustment(
        loan: LoanEntity,
        adjustment: LoanPartPaymentAdjustment?,
    ): LoanEntity {
        if (loan.repaymentModeEnum() != LoanRepaymentMode.FIXED_EMI) return loan
        val emi = loan.fixedEmiAmount ?: return loan
        val tenure = loan.remainingTenureMonths ?: return loan
        return when (adjustment) {
            LoanPartPaymentAdjustment.REDUCE_TENURE -> {
                val newTenure =
                    LoanAmortizationEngine.computeTenureMonths(
                        loan.outstandingAmount,
                        loan.interestRate,
                        emi,
                    )
                loan.copy(remainingTenureMonths = newTenure)
            }
            LoanPartPaymentAdjustment.REDUCE_EMI -> {
                val months = tenure.coerceAtLeast(1)
                val newEmi =
                    LoanAmortizationEngine.computeEmi(
                        loan.outstandingAmount,
                        loan.interestRate,
                        months,
                    )
                loan.copy(fixedEmiAmount = newEmi)
            }
            null -> loan
        }
    }

    fun applyTopUpAdjustment(
        loan: LoanEntity,
        adjustment: LoanTopUpAdjustment,
    ): LoanEntity {
        if (loan.repaymentModeEnum() != LoanRepaymentMode.FIXED_EMI) return loan
        val emi = loan.fixedEmiAmount ?: return loan
        val tenure = loan.remainingTenureMonths ?: return loan
        return when (adjustment) {
            LoanTopUpAdjustment.EXTEND_TENURE -> {
                val newTenure =
                    LoanAmortizationEngine.computeTenureMonths(
                        loan.outstandingAmount,
                        loan.interestRate,
                        emi,
                    )
                if (newTenure <= 0) return loan
                loan.copy(
                    remainingTenureMonths = newTenure,
                    tenureMonths = (loan.tenureMonths ?: newTenure).coerceAtLeast(newTenure),
                )
            }
            LoanTopUpAdjustment.INCREASE_EMI -> {
                val months = tenure.coerceAtLeast(1)
                val newEmi =
                    LoanAmortizationEngine.computeEmi(
                        loan.outstandingAmount,
                        loan.interestRate,
                        months,
                    )
                loan.copy(fixedEmiAmount = newEmi)
            }
        }
    }

    fun initialRevision(
        loanId: Long,
        rate: Double,
        effectiveFrom: Long,
    ): LoanInterestRevisionEntity =
        LoanInterestRevisionEntity(
            loanId = loanId,
            effectiveFromMillis = effectiveFrom,
            annualRatePercent = rate,
            note = "Opening rate",
        )
}
