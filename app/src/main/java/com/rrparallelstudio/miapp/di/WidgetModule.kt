package com.rrparallelstudio.miapp.di

import com.rrparallelstudio.miapp.core.common.widget.HomeScreenWidgetRefreshTrigger
import com.rrparallelstudio.miapp.widget.HomeScreenWidgetRefreshTriggerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    @Singleton
    abstract fun bindHomeScreenWidgetRefreshTrigger(
        impl: HomeScreenWidgetRefreshTriggerImpl,
    ): HomeScreenWidgetRefreshTrigger
}
