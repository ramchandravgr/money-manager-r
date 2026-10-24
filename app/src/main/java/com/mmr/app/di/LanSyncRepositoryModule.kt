package com.mmr.app.di

import com.mmr.core.data.repository.LanSyncRepositoryImpl
import com.mmr.core.domain.repository.LanSyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LanSyncRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindLanSyncRepository(impl: LanSyncRepositoryImpl): LanSyncRepository
}
