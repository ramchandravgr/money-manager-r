package com.mmr.app.di

import com.mmr.app.webmanage.WebManageChartThemeSourceImpl
import com.mmr.core.webmanage.WebManageChartThemeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WebManageChartThemeModule {
    @Binds
    @Singleton
    abstract fun bindWebManageChartThemeSource(impl: WebManageChartThemeSourceImpl): WebManageChartThemeSource
}
