package com.rrparallelstudio.miapp.feature.asset.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrparallelstudio.miapp.core.asset.AssetValuationFormat
import com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat
import com.rrparallelstudio.miapp.core.database.relation.AssetWithValuations
import com.rrparallelstudio.miapp.core.domain.repository.AssetRepository
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import com.rrparallelstudio.miapp.feature.asset.model.AssetListItemUi
import com.rrparallelstudio.miapp.feature.asset.model.AssetListUiEvent
import com.rrparallelstudio.miapp.feature.asset.model.AssetListUiState
import com.rrparallelstudio.miapp.feature.asset.model.AssetStatusFilter
import com.rrparallelstudio.miapp.feature.asset.model.assetTypeDisplayLabel
import com.rrparallelstudio.miapp.feature.asset.model.displayLabel
import com.rrparallelstudio.miapp.ui.state.MmrUiMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AssetListViewModel
    @Inject
    constructor(
        private val assetRepository: AssetRepository,
    ) : ViewModel() {
        private val statusFilterState = MutableStateFlow(AssetStatusFilter.ACTIVE)
        private val loadErrorState = MutableStateFlow<String?>(null)

        val uiState: StateFlow<AssetListUiState> =
            combine(
                assetRepository.getAssetsWithValuations().catch { e ->
                    loadErrorState.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                    emit(emptyList())
                },
                statusFilterState,
                loadErrorState,
            ) { rows, filter, loadError ->
                val all = rows.map { it.asset }
                val activeCount = all.count { it.status == AssetStatus.ACTIVE }
                val soldCount = all.count { it.status == AssetStatus.SOLD }
                val filtered =
                    rows.filter { row ->
                        when (filter) {
                            AssetStatusFilter.ACTIVE -> row.asset.status == AssetStatus.ACTIVE
                            AssetStatusFilter.SOLD -> row.asset.status == AssetStatus.SOLD
                            AssetStatusFilter.ALL -> true
                        }
                    }
                val items = filtered.map { it.toListItemUi() }
                val totalActive =
                    all
                        .filter { it.status == AssetStatus.ACTIVE }
                        .sumOf { it.currentValue }
                AssetListUiState(
                    isLoading = false,
                    assets = items,
                    selectedStatusFilter = filter,
                    totalActiveValueFormatted = MmrMoneyFormat.formatInr(totalActive),
                    activeCount = activeCount,
                    soldCount = soldCount,
                    isEmpty = loadError == null && all.isEmpty(),
                    isFilteredEmpty = loadError == null && all.isNotEmpty() && items.isEmpty(),
                    errorMessage = loadError,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AssetListUiState(isLoading = true),
            )

        fun onEvent(event: AssetListUiEvent) {
            when (event) {
                is AssetListUiEvent.OnStatusFilterSelected -> statusFilterState.value = event.filter
                AssetListUiEvent.OnRetryClick -> loadErrorState.value = null
            }
        }
    }

private fun AssetWithValuations.toListItemUi(): AssetListItemUi {
    val sorted = valuations.sortedByDescending { it.recordedAt }
    val latest = sorted.firstOrNull()?.amount
    val previous = sorted.drop(1).firstOrNull()?.amount
    val change =
        AssetValuationFormat.formatPercentChange(
            AssetValuationFormat.percentChangeFromPrevious(previous, latest ?: asset.currentValue),
        )
    return AssetListItemUi(
        id = asset.id,
        name = asset.name,
        typeLabel = asset.assetType.assetTypeDisplayLabel().ifBlank { "—" },
        status = asset.status,
        statusLabel = asset.status.displayLabel(),
        currentValueFormatted = MmrMoneyFormat.formatInr(asset.currentValue),
        changePercentLabel = change,
        ownedByName = asset.ownedByName?.takeIf { it.isNotBlank() },
    )
}
