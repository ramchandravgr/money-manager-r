package com.mmr.core.data.repository

import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.common.dispatcher.IoDispatcher
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.preferences.LanSyncIdentityStore
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.database.dao.LanSyncDao
import com.mmr.core.database.entity.PendingLanPairRequestEntity
import com.mmr.core.database.entity.TrustedLanDeviceEntity
import com.mmr.core.domain.repository.LanSyncRepository
import com.mmr.core.domain.repository.LanSyncSessionState
import com.mmr.core.lansync.LanSyncExporter
import com.mmr.core.lansync.LanSyncJsonCodec
import com.mmr.core.lansync.LanSyncMergeSummary
import com.mmr.core.lansync.LanSyncMerger
import com.mmr.core.lansync.LanSyncNetworkUtils
import com.mmr.core.lansync.LanSyncPairingCodec
import com.mmr.core.lansync.LanSyncPairingOffer
import com.mmr.core.lansync.transport.LanSyncHttpClient
import com.mmr.core.lansync.transport.LanSyncHttpConstants
import com.mmr.core.lansync.transport.LanSyncHttpServer
import com.mmr.core.lansync.transport.LanSyncNsdHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncRepositoryImpl
    @Inject
    constructor(
        private val identityStore: LanSyncIdentityStore,
        private val lanSyncDao: LanSyncDao,
        private val exporter: LanSyncExporter,
        private val merger: LanSyncMerger,
        private val smsScanSettingsStore: SmsScanSettingsStore,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val networkUtils: LanSyncNetworkUtils,
        private val nsdHelper: LanSyncNsdHelper,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : LanSyncRepository {
        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private var httpServer: LanSyncHttpServer? = null

        private val _sessionState = MutableStateFlow(LanSyncSessionState())
        override val sessionState: StateFlow<LanSyncSessionState> = _sessionState.asStateFlow()

        override suspend fun refreshSessionState() {
            withContext(ioDispatcher) {
                publishState()
            }
        }

        override fun setLanSyncEnabled(enabled: Boolean) {
            identityStore.setLanSyncEnabled(enabled)
            if (enabled) {
                startInfrastructure()
            } else {
                stopInfrastructure()
            }
            scope.launch { publishState() }
        }

        override fun setDisplayName(name: String) {
            identityStore.displayName = name
            scope.launch { publishState() }
        }

        override fun pairingOffer(): LanSyncPairingOffer =
            LanSyncPairingOffer(
                deviceId = identityStore.deviceId,
                displayName = identityStore.displayName,
                inboundAuthToken = identityStore.inboundAuthToken,
            )

        override suspend fun trustDeviceFromPairingOffer(offer: LanSyncPairingOffer) {
            withContext(ioDispatcher) {
                if (offer.deviceId == identityStore.deviceId) {
                    throw IllegalArgumentException("Cannot trust this device.")
                }
                lanSyncDao.upsertTrustedDevice(
                    TrustedLanDeviceEntity(
                        remoteDeviceId = offer.deviceId,
                        displayName = offer.displayName,
                        theirAuthToken = offer.inboundAuthToken,
                    ),
                )
                sendPairRequestToPeer(offer)
                publishState()
            }
        }

        override suspend fun approvePairRequest(deviceId: String) {
            withContext(ioDispatcher) {
                val request =
                    lanSyncDao.getPendingPairRequests().firstOrNull { it.remoteDeviceId == deviceId }
                        ?: throw IllegalArgumentException("Pair request not found.")
                lanSyncDao.approvePairRequest(request)
                publishState()
            }
        }

        override suspend fun rejectPairRequest(deviceId: String) {
            withContext(ioDispatcher) {
                lanSyncDao.deletePendingPairRequest(deviceId)
                publishState()
            }
        }

        override suspend fun removeTrustedDevice(deviceId: String) {
            withContext(ioDispatcher) {
                lanSyncDao.deleteTrustedDevice(deviceId)
                publishState()
            }
        }

        override suspend fun syncNow(): LanSyncMergeSummary =
            withContext(ioDispatcher) {
                if (!identityStore.isLanSyncEnabled()) {
                    throw IllegalStateException("Turn on Wi‑Fi sync in settings first.")
                }
                if (!networkUtils.isOnWifi()) {
                    throw IllegalStateException("Connect to Wi‑Fi to sync.")
                }
                val trusted = lanSyncDao.getTrustedDevices()
                if (trusted.isEmpty()) {
                    throw IllegalStateException("Add at least one trusted device.")
                }
                var total =
                    LanSyncMergeSummary(
                        categoriesInserted = 0,
                        categoriesUpdated = 0,
                        subcategoriesInserted = 0,
                        subcategoriesUpdated = 0,
                        accountsInserted = 0,
                        accountsUpdated = 0,
                        loansInserted = 0,
                        loansUpdated = 0,
                        transactionsInserted = 0,
                        transactionsUpdated = 0,
                    )
                for (device in trusted) {
                    val peer =
                        nsdHelper.findPeer(device.remoteDeviceId)
                            ?: throw IllegalStateException(
                                "Trusted device ${device.displayName} is not on this Wi‑Fi network.",
                            )
                    val localPayloadJson = exportPayloadJson()
                    val response =
                        LanSyncHttpClient.post(
                            host = peer.host,
                            port = peer.port,
                            path = LanSyncHttpConstants.PATH_SYNC,
                            deviceId = identityStore.deviceId,
                            authToken = device.theirAuthToken,
                            body = localPayloadJson,
                        )
                    if (response.statusCode != 200) {
                        throw IllegalStateException("Sync failed with ${device.displayName} (${response.statusCode}).")
                    }
                    val remotePayload = LanSyncJsonCodec.decode(response.body)
                    val summary = merger.merge(remotePayload)
                    applySettings(remotePayload)
                    total = total + summary
                }
                publishState()
                total
            }

        private suspend fun publishState() {
            val offer = pairingOffer()
            _sessionState.value =
                LanSyncSessionState(
                    enabled = identityStore.isLanSyncEnabled(),
                    deviceId = offer.deviceId,
                    displayName = offer.displayName,
                    pairingOfferText = LanSyncPairingCodec.encode(offer),
                    trustedDevices = lanSyncDao.getTrustedDevices(),
                    pendingPairRequests = lanSyncDao.getPendingPairRequests(),
                    discoveredPeers = nsdHelper.discoveredPeers(),
                    onWifi = networkUtils.isOnWifi(),
                )
        }

        private fun startInfrastructure() {
            if (httpServer != null) return
            val server =
                LanSyncHttpServer(scope) { path, headers, body ->
                    handleHttpRequest(path, headers, body)
                }
            server.start()
            httpServer = server
            nsdHelper.registerService(
                deviceId = identityStore.deviceId,
                displayName = identityStore.displayName,
                port = server.port,
            )
            nsdHelper.startDiscovery { scope.launch { publishState() } }
        }

        private fun stopInfrastructure() {
            nsdHelper.stopDiscovery()
            nsdHelper.unregisterService()
            httpServer?.stop()
            httpServer = null
        }

        private fun handleHttpRequest(
            path: String,
            headers: Map<String, String>,
            body: String,
        ): Pair<Int, String> {
            if (!identityStore.isLanSyncEnabled()) {
                return 403 to """{"error":"sync_disabled"}"""
            }
            return when (path) {
                LanSyncHttpConstants.PATH_PAIR -> handlePairRequest(headers, body)
                LanSyncHttpConstants.PATH_SYNC -> handleSyncRequest(headers, body)
                else -> 404 to """{"error":"not_found"}"""
            }
        }

        private fun handlePairRequest(
            headers: Map<String, String>,
            body: String,
        ): Pair<Int, String> {
            val remoteDeviceId = headers[LanSyncHttpConstants.HEADER_DEVICE_ID.lowercase()] ?: return 401 to ""
            val remoteToken = headers[LanSyncHttpConstants.HEADER_AUTH_TOKEN.lowercase()]
            if (!remoteToken.isNullOrBlank()) {
                val trusted =
                    runCatching {
                        kotlinx.coroutines.runBlocking {
                            lanSyncDao.getTrustedDevice(
                                remoteDeviceId,
                            )
                        }
                    }.getOrNull()
                if (trusted == null || trusted.theirAuthToken != remoteToken) {
                    // New pair requests do not require prior trust.
                }
            }
            val json = runCatching { JSONObject(body) }.getOrNull() ?: return 400 to ""
            val deviceId = json.optString("deviceId")
            val displayName = json.optString("displayName")
            val inboundAuthToken = json.optString("inboundAuthToken")
            if (deviceId.isBlank() || inboundAuthToken.isBlank()) return 400 to ""
            scope.launch {
                lanSyncDao.upsertPendingPairRequest(
                    PendingLanPairRequestEntity(
                        remoteDeviceId = deviceId,
                        displayName = displayName.ifBlank { "MMR device" },
                        theirAuthToken = inboundAuthToken,
                    ),
                )
                publishState()
            }
            return 200 to """{"status":"pending"}"""
        }

        private fun handleSyncRequest(
            headers: Map<String, String>,
            body: String,
        ): Pair<Int, String> {
            val remoteDeviceId = headers[LanSyncHttpConstants.HEADER_DEVICE_ID.lowercase()] ?: return 401 to ""
            val remoteToken = headers[LanSyncHttpConstants.HEADER_AUTH_TOKEN.lowercase()] ?: return 401 to ""
            val trusted =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) {
                        lanSyncDao.getTrustedDevice(remoteDeviceId)
                    }
                }.getOrNull()
            if (trusted == null || trusted.theirAuthToken != remoteToken) {
                return 401 to """{"error":"unauthorized"}"""
            }
            val mergeResult =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) {
                        val payload = LanSyncJsonCodec.decode(body)
                        val summary = merger.merge(payload)
                        applySettings(payload)
                        summary
                    }
                }
            if (mergeResult.isFailure) {
                return 400 to """{"error":"merge_failed"}"""
            }
            val responseBody =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) { exportPayloadJson() }
                }.getOrElse { return 500 to """{"error":"export_failed"}""" }
            return 200 to responseBody
        }

        private suspend fun exportPayloadJson(): String {
            val payload =
                exporter.buildPayload(
                    sourceDeviceId = identityStore.deviceId,
                    appSettings = MmrAppSettingsBackup(smsScanMonths = smsScanSettingsStore.getScanMonths()),
                    displaySettings = displaySettingsStore.snapshot(),
                )
            return LanSyncJsonCodec.encode(payload)
        }

        private suspend fun applySettings(payload: com.mmr.core.lansync.model.MmrLanSyncPayload) {
            payload.appSettings?.let { smsScanSettingsStore.setScanMonths(it.smsScanMonths) }
            payload.displaySettings?.let { displaySettingsStore.applySnapshot(it) }
        }

        private suspend fun sendPairRequestToPeer(offer: LanSyncPairingOffer) {
            val peer = nsdHelper.findPeer(offer.deviceId) ?: return
            val body =
                JSONObject()
                    .apply {
                        put("deviceId", identityStore.deviceId)
                        put("displayName", identityStore.displayName)
                        put("inboundAuthToken", identityStore.inboundAuthToken)
                    }.toString()
            LanSyncHttpClient.post(
                host = peer.host,
                port = peer.port,
                path = LanSyncHttpConstants.PATH_PAIR,
                deviceId = identityStore.deviceId,
                authToken = offer.inboundAuthToken,
                body = body,
            )
        }

        private operator fun LanSyncMergeSummary.plus(other: LanSyncMergeSummary): LanSyncMergeSummary =
            LanSyncMergeSummary(
                categoriesInserted = categoriesInserted + other.categoriesInserted,
                categoriesUpdated = categoriesUpdated + other.categoriesUpdated,
                subcategoriesInserted = subcategoriesInserted + other.subcategoriesInserted,
                subcategoriesUpdated = subcategoriesUpdated + other.subcategoriesUpdated,
                accountsInserted = accountsInserted + other.accountsInserted,
                accountsUpdated = accountsUpdated + other.accountsUpdated,
                loansInserted = loansInserted + other.loansInserted,
                loansUpdated = loansUpdated + other.loansUpdated,
                transactionsInserted = transactionsInserted + other.transactionsInserted,
                transactionsUpdated = transactionsUpdated + other.transactionsUpdated,
            )

        init {
            if (identityStore.isLanSyncEnabled()) {
                startInfrastructure()
            }
            scope.launch { publishState() }
        }
    }
