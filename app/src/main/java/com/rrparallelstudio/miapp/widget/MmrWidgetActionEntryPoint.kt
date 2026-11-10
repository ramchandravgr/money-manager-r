package com.rrparallelstudio.miapp.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MmrWidgetActionEntryPoint {
    fun widgetActionExecutor(): MmrWidgetActionExecutor
}
