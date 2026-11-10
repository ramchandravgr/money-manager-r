package com.rrparallelstudio.miapp.di

import com.rrparallelstudio.miapp.core.data.repository.LanSyncRepositoryImpl
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncRepository
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
