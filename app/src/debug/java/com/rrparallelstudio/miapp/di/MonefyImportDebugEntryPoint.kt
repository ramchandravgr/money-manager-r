package com.rrparallelstudio.miapp.di

import com.rrparallelstudio.miapp.core.monefy.MonefyLedgerImporter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MonefyImportDebugEntryPoint {
    fun monefyLedgerImporter(): MonefyLedgerImporter
}
