package com.mmr.app.di

import com.mmr.app.lansync.LocalLanServerNotifierImpl
import com.mmr.core.lansync.LocalLanServerNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalLanServerNotifierModule {
    @Binds
    @Singleton
    abstract fun bindLocalLanServerNotifier(impl: LocalLanServerNotifierImpl): LocalLanServerNotifier
}
