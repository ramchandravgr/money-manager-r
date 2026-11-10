package com.rrparallelstudio.miapp.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanPaymentEntity

data class LoanWithPayments(
    @Embedded
    val loan: LoanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "loanId",
    )
    val payments: List<LoanPaymentEntity>,
)
