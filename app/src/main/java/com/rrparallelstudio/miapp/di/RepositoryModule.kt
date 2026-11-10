package com.rrparallelstudio.miapp.di

import com.rrparallelstudio.miapp.core.data.repository.AccountRepositoryImpl
import com.rrparallelstudio.miapp.core.data.repository.BudgetRepositoryImpl
import com.rrparallelstudio.miapp.core.data.repository.CategoryRepositoryImpl
import com.rrparallelstudio.miapp.core.data.repository.TransactionAttachmentRepositoryImpl
import com.rrparallelstudio.miapp.core.data.repository.TransactionRepositoryImpl
import com.rrparallelstudio.miapp.core.domain.repository.AccountRepository
import com.rrparallelstudio.miapp.core.domain.repository.BackupRepository
import com.rrparallelstudio.miapp.core.domain.repository.BudgetRepository
import com.rrparallelstudio.miapp.core.domain.repository.CategoryRepository
import com.rrparallelstudio.miapp.core.domain.repository.TransactionAttachmentRepository
import com.rrparallelstudio.miapp.core.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindTransactionAttachmentRepository(
        impl: TransactionAttachmentRepositoryImpl,
    ): TransactionAttachmentRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindApprovalRepository(
        impl: com.rrparallelstudio.miapp.core.data.repository.ApprovalRepositoryImpl,
    ): com.rrparallelstudio.miapp.core.domain.repository.ApprovalRepository

    @Binds
    @Singleton
    abstract fun bindLoanRepository(
        impl: com.rrparallelstudio.miapp.core.data.repository.LoanRepositoryImpl,
    ): com.rrparallelstudio.miapp.core.domain.repository.LoanRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRuleRepository(
        impl: com.rrparallelstudio.miapp.core.data.repository.RecurringRuleRepositoryImpl,
    ): com.rrparallelstudio.miapp.core.domain.repository.RecurringRuleRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: com.rrparallelstudio.miapp.core.data.repository.BackupRepositoryImpl,
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindGoogleDriveBackupRepository(
        impl: com.rrparallelstudio.miapp.core.data.repository.GoogleDriveBackupRepositoryImpl,
    ): com.rrparallelstudio.miapp.core.domain.repository.GoogleDriveBackupRepository

    @Binds
    @Singleton
    abstract fun bindAssetRepository(
        impl: com.rrparallelstudio.miapp.core.data.repository.AssetRepositoryImpl,
    ): com.rrparallelstudio.miapp.core.domain.repository.AssetRepository

    @Binds
    @Singleton
    abstract fun bindAssetAttachmentRepository(
        impl: com.rrparallelstudio.miapp.core.data.repository.AssetAttachmentRepositoryImpl,
    ): com.rrparallelstudio.miapp.core.domain.repository.AssetAttachmentRepository
}
