package com.rrparallelstudio.miapp.core.database.entity

import androidx.room.*
import com.rrparallelstudio.miapp.core.model.enums.CardNetwork
import com.rrparallelstudio.miapp.core.model.enums.SmsApprovalKind
import com.rrparallelstudio.miapp.core.model.enums.SmsApprovalState
import com.rrparallelstudio.miapp.core.model.enums.TransactionType

@Entity(
    tableName = "transaction_approvals",
    foreignKeys = [
        ForeignKey(
            entity = SmsMessageRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["smsMessageRecordId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["extractedCategoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["extractedAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["smsMessageRecordId"], unique = true),
        Index(value = ["extractedCategoryId"]),
        Index(value = ["extractedAccountId"]),
        Index(value = ["approvalState"]),
        Index(value = ["createdAt"]),
    ],
)
data class TransactionApprovalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val smsMessageRecordId: Long,
    val extractedAmount: Double,
    val extractedType: TransactionType = TransactionType.EXPENSE,
    val approvalKind: SmsApprovalKind = SmsApprovalKind.TRANSACTION,
    val extractedDateTime: Long,
    val extractedSender: String,
    val extractedMerchant: String? = null,
    val extractedCategoryId: Long? = null,
    val extractedAccountId: Long? = null,
    val extractedNote: String? = null,
    val confidenceScore: Int = 0,
    val userEdited: Boolean = false,
    val approvalState: SmsApprovalState = SmsApprovalState.PENDING,
    val rawBodySnippet: String = "",
    val extractedTemplateId: String? = null,
    val extractedPaymentChannel: String? = null,
    val extractedInstrument: String? = null,
    val extractedCardLastFour: String? = null,
    val extractedCardNetwork: CardNetwork? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
