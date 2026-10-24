package com.mmr.app.di

import com.mmr.app.widget.HomeScreenWidgetRefreshTriggerImpl
import com.mmr.core.common.widget.HomeScreenWidgetRefreshTrigger
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
