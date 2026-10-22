package com.mmr.app.di

import com.mmr.core.data.repository.AccountRepositoryImpl
import com.mmr.core.data.repository.CategoryRepositoryImpl
import com.mmr.core.data.repository.TransactionRepositoryImpl
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.BackupRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.TransactionRepository
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
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindApprovalRepository(
        impl: com.mmr.core.data.repository.ApprovalRepositoryImpl
    ): com.mmr.core.domain.repository.ApprovalRepository

    @Binds
    @Singleton
    abstract fun bindLoanRepository(
        impl: com.mmr.core.data.repository.LoanRepositoryImpl
    ): com.mmr.core.domain.repository.LoanRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRuleRepository(
        impl: com.mmr.core.data.repository.RecurringRuleRepositoryImpl
    ): com.mmr.core.domain.repository.RecurringRuleRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: com.mmr.core.data.repository.BackupRepositoryImpl
    ): BackupRepository
}
