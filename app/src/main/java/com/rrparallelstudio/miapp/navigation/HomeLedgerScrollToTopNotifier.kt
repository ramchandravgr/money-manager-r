package com.rrparallelstudio.miapp.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Emitted after a manual new transaction is saved so the ledger list scrolls to the top. */
@Singleton
class HomeLedgerScrollToTopNotifier
    @Inject
    constructor() {
        private val _signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val signals: SharedFlow<Unit> = _signals.asSharedFlow()

        fun notifyScrollToTop() {
            _signals.tryEmit(Unit)
        }
    }
