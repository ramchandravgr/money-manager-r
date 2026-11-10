package com.rrparallelstudio.miapp.feature.lansync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncRepository
import com.rrparallelstudio.miapp.core.lansync.LanSyncOverrideJson
import com.rrparallelstudio.miapp.core.lansync.LanSyncOverrideLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class LanSyncHistorySession(
    val peerName: String,
    val whenLabel: String,
    val success: Boolean,
    val unchangedCount: Int,
    val addedCount: Int,
    val overrides: List<LanSyncOverrideLine>,
    val counts: String,
)

@HiltViewModel
class LanSyncHistoryViewModel
    @Inject
    constructor(
        private val lanSyncRepository: LanSyncRepository,
    ) : ViewModel() {
        val sessions =
            lanSyncRepository.sessionState
                .map { state ->
                    state.syncLog.map { entry ->
                        val overrides = LanSyncOverrideJson.decode(entry.overridesJson)
                        val session =
                            LanSyncHistorySession(
                                peerName = entry.peerName,
                                whenLabel = formatWhen(entry.completedAtMillis),
                                success = entry.success,
                                unchangedCount = entry.unchangedCount,
                                addedCount = entry.addedCount,
                                overrides = overrides,
                                counts = "",
                            )
                        session.copy(
                            counts =
                                "${overrides.size} replaced · ${entry.unchangedCount} already in sync · ${entry.addedCount} added",
                        )
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            viewModelScope.launch { lanSyncRepository.refreshSessionState() }
        }

        private fun formatWhen(completedAtMillis: Long): String {
            val formatter = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())
            return Instant.ofEpochMilli(completedAtMillis).atZone(ZoneId.systemDefault()).format(formatter)
        }
    }
