package com.rrparallelstudio.miapp.di

import com.rrparallelstudio.miapp.core.data.repository.WebManageRepositoryImpl
import com.rrparallelstudio.miapp.core.domain.repository.WebManageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WebManageRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWebManageRepository(impl: WebManageRepositoryImpl): WebManageRepository
}
