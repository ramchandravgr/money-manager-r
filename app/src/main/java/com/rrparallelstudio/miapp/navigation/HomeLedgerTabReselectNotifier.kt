package com.rrparallelstudio.miapp.navigation

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main nav calls [notifyLedgerTabReselected] when the user taps **Ledger** while that tab is already selected.
 * [HomeScreen] forwards to [com.rrparallelstudio.miapp.feature.home.model.HomeUiEvent.OnLedgerTabReselected].
 */
@Singleton
class HomeLedgerTabReselectNotifier
    @Inject
    constructor() {
        private val _signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val signals: SharedFlow<Unit> = _signals.asSharedFlow()

        fun notifyLedgerTabReselected() {
            _signals.tryEmit(Unit)
        }
    }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeLedgerTabReselectNotifierEntryPoint {
    fun homeLedgerTabReselectNotifier(): HomeLedgerTabReselectNotifier
}

fun homeLedgerTabReselectNotifierFrom(applicationContext: Context): HomeLedgerTabReselectNotifier =
    EntryPointAccessors
        .fromApplication(
            applicationContext,
            HomeLedgerTabReselectNotifierEntryPoint::class.java,
        ).homeLedgerTabReselectNotifier()
