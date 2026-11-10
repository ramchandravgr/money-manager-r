package com.rrparallelstudio.miapp.di

import com.rrparallelstudio.miapp.core.webmanage.WebManageChartThemeSource
import com.rrparallelstudio.miapp.webmanage.WebManageChartThemeSourceImpl
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
