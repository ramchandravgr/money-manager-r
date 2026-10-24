package com.mmr.core.parser.template

import android.content.Context
import com.mmr.core.common.dispatcher.IoDispatcher
import com.mmr.core.parser.model.SmsTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SmsTemplateOverlayStatus(
    val overlayCount: Int = 0,
    val mergedCount: Int = 0,
    val overrideBuiltInCount: Int = 0,
)

sealed class SmsTemplateOverlayImportResult {
    data class Success(
        val overlayCount: Int,
        val overrideBuiltInCount: Int,
    ) : SmsTemplateOverlayImportResult()

    data class ValidationFailed(
        val errors: List<SmsTemplateJsonCodec.TemplateParseError>,
    ) : SmsTemplateOverlayImportResult()
}

@Singleton
class SmsTemplateOverlayRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val overlayStore: SmsTemplateOverlayStore,
        private val templateRegistry: SmsTemplateRegistry,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val builtInIds: Set<String> by lazy {
            SmsTemplateRegistry.loadBuiltInFromAssets(context).map { it.id }.toSet()
        }

        private val _status = MutableStateFlow(SmsTemplateOverlayStatus())
        val status: StateFlow<SmsTemplateOverlayStatus> = _status.asStateFlow()

        init {
            refreshStatus()
        }

        fun loadMergedTemplates(): List<SmsTemplate> {
            val builtIn = SmsTemplateRegistry.loadBuiltInFromAssets(context)
            val overlay = readOverlayTemplates()
            return SmsTemplateJsonCodec.mergeBuiltInWithOverlay(builtIn, overlay)
        }

        fun readOverlayTemplates(): List<SmsTemplate> {
            val json = overlayStore.readJsonOrNull() ?: return emptyList()
            return runCatching {
                SmsTemplateJsonCodec.parseOverlayJson(json).templates
            }.getOrDefault(emptyList())
        }

        suspend fun importOverlayJson(json: String): SmsTemplateOverlayImportResult =
            withContext(ioDispatcher) {
                val parsed = SmsTemplateJsonCodec.parseOverlayJson(json)
                if (!parsed.ok) {
                    return@withContext SmsTemplateOverlayImportResult.ValidationFailed(parsed.errors)
                }
                val normalized = SmsTemplateJsonCodec.overlayJsonFromTemplates(parsed.templates)
                overlayStore.writeJson(normalized)
                templateRegistry.reload()
                refreshStatus()
                val overrides = parsed.templates.count { builtInIds.contains(it.id) }
                SmsTemplateOverlayImportResult.Success(
                    overlayCount = parsed.templates.size,
                    overrideBuiltInCount = overrides,
                )
            }

        suspend fun clearOverlay(): Unit =
            withContext(ioDispatcher) {
                overlayStore.clear()
                templateRegistry.reload()
                refreshStatus()
            }

        private fun refreshStatus() {
            val builtIn = SmsTemplateRegistry.loadBuiltInFromAssets(context)
            val overlay = readOverlayTemplates()
            val merged = SmsTemplateJsonCodec.mergeBuiltInWithOverlay(builtIn, overlay)
            _status.value =
                SmsTemplateOverlayStatus(
                    overlayCount = overlay.size,
                    mergedCount = merged.size,
                    overrideBuiltInCount = overlay.count { builtInIds.contains(it.id) },
                )
        }
    }
