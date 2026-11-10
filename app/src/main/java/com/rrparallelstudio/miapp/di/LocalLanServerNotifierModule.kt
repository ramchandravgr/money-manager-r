package com.rrparallelstudio.miapp.di

import com.rrparallelstudio.miapp.core.lansync.LocalLanServerNotifier
import com.rrparallelstudio.miapp.lansync.LocalLanServerNotifierImpl
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
