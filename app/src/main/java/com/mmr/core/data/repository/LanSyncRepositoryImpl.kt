package com.mmr.core.data.repository

import com.mmr.core.attachment.TransactionAttachmentStore
import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.common.dispatcher.IoDispatcher
import com.mmr.core.data.preferences.LanSyncIdentityStore
import com.mmr.core.data.preferences.LanSyncPeerMode
import com.mmr.core.data.preferences.LanSyncPreferencesStore
import com.mmr.core.data.preferences.LanSyncWifiTrustStore
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.database.dao.LanSyncDao
import com.mmr.core.database.dao.TransactionApprovalDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.LanSyncLogEntryEntity
import com.mmr.core.database.entity.TrustedLanDeviceEntity
import com.mmr.core.domain.repository.LanSyncNowResult
import com.mmr.core.domain.repository.LanSyncRepository
import com.mmr.core.domain.repository.LanSyncSessionState
import com.mmr.core.domain.repository.WebManageRepository
import com.mmr.core.lansync.LanSyncAttachmentPuller
import com.mmr.core.lansync.LanSyncConflictDetector
import com.mmr.core.lansync.LanSyncExporter
import com.mmr.core.lansync.LanSyncJsonCodec
import com.mmr.core.lansync.LanSyncMergeOptions
import com.mmr.core.lansync.LanSyncMergeSummary
import com.mmr.core.lansync.LanSyncMerger
import com.mmr.core.lansync.LanSyncNeedsResolutionException
import com.mmr.core.lansync.LanSyncNetworkUtils
import com.mmr.core.lansync.LanSyncPairingCodec
import com.mmr.core.lansync.LanSyncPairingOffer
import com.mmr.core.lansync.LanSyncPairingPin
import com.mmr.core.lansync.LanSyncTombstoneCoordinator
import com.mmr.core.lansync.LanSyncWifiNotTrustedException
import com.mmr.core.lansync.LocalLanServerCoordinator
import com.mmr.core.lansync.LocalLanServerMode
import com.mmr.core.lansync.LocalLanServerNotifier
import com.mmr.core.lansync.model.MmrLanSyncPayload
import com.mmr.core.lansync.transport.HttpLanResponse
import com.mmr.core.lansync.transport.LanSyncHttpClient
import com.mmr.core.lansync.transport.LanSyncHttpConstants
import com.mmr.core.lansync.transport.LanSyncHttpServer
import com.mmr.core.lansync.transport.LanSyncNsdHelper
import dagger.Lazy
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
        private val wifiTrustStore: LanSyncWifiTrustStore,
        private val preferencesStore: LanSyncPreferencesStore,
        private val localLanServerCoordinator: LocalLanServerCoordinator,
        private val webManageRepository: Lazy<WebManageRepository>,
        private val lanServerNotifier: LocalLanServerNotifier,
        private val conflictDetector: LanSyncConflictDetector,
        private val transactionDao: TransactionDao,
        private val transactionApprovalDao: TransactionApprovalDao,
        private val networkUtils: LanSyncNetworkUtils,
        private val nsdHelper: LanSyncNsdHelper,
        private val attachmentPuller: LanSyncAttachmentPuller,
        private val attachmentStore: TransactionAttachmentStore,
        private val tombstoneCoordinator: LanSyncTombstoneCoordinator,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : LanSyncRepository {
        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private var httpServer: LanSyncHttpServer? = null
        private var activePairingPin: String? = null

        private val _sessionState = MutableStateFlow(LanSyncSessionState())
        override val sessionState: StateFlow<LanSyncSessionState> = _sessionState.asStateFlow()

        override suspend fun refreshSessionState() {
            withContext(ioDispatcher) {
                publishState()
            }
        }

        override fun setLanSyncEnabled(enabled: Boolean) {
            if (enabled) {
                localLanServerCoordinator.clearWebManageBecausePeerSyncStarting()
                webManageRepository.get().onPeerSyncStarting()
            }
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

        override suspend fun resolvePairingInput(raw: String): LanSyncPairingOffer =
            withContext(ioDispatcher) {
                val trimmed = raw.trim()
                if (trimmed.startsWith(LanSyncPairingCodec.LEGACY_PREFIX)) {
                    return@withContext LanSyncPairingCodec.decode(trimmed)
                }
                val pin = LanSyncPairingPin.normalize(trimmed)
                if (!LanSyncPairingPin.isValidFormat(pin)) {
                    throw IllegalArgumentException("Enter a valid pairing code from the other device.")
                }
                resolveOfferFromLanPeers(pin)
            }

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

        override suspend fun markCurrentWifiTrusted() {
            withContext(ioDispatcher) {
                val ssid = networkUtils.wifiInfo().ssid ?: return@withContext
                wifiTrustStore.markTrusted(ssid)
            }
        }

        override suspend fun clearSyncLog() {
            withContext(ioDispatcher) {
                lanSyncDao.clearSyncLog()
                publishState()
            }
        }

        override suspend fun removeTrustedDevice(deviceId: String) {
            withContext(ioDispatcher) {
                lanSyncDao.deleteTrustedDevice(deviceId)
                publishState()
            }
        }

        override fun setPeerMode(mode: LanSyncPeerMode) {
            preferencesStore.peerMode = mode
            scope.launch { publishState() }
        }

        override fun setSelectedPeerDeviceId(deviceId: String?) {
            preferencesStore.selectedPeerDeviceId = deviceId
            scope.launch { publishState() }
        }

        override fun setIncludeAttachments(enabled: Boolean) {
            preferencesStore.includeAttachments = enabled
            scope.launch { publishState() }
        }

        override suspend fun syncNow(mergeOptions: LanSyncMergeOptions): LanSyncNowResult =
            withContext(ioDispatcher) {
                ensureReadyForSync()
                val allTrusted = lanSyncDao.getTrustedDevices()
                val targets = resolveSyncTargets(allTrusted)
                ensureSyncTargetsPresent(allTrusted, targets)
                syncWithPeers(
                    devices = targets,
                    oneWayLocalPayload = false,
                    mergeOptions = mergeOptions,
                    interactive = true,
                )
            }

        override suspend fun restoreFromPeer(
            remoteDeviceId: String,
            mergeOptions: LanSyncMergeOptions,
        ): LanSyncMergeSummary =
            withContext(ioDispatcher) {
                ensureReadyForSync()
                val device =
                    lanSyncDao.getTrustedDevice(remoteDeviceId)
                        ?: throw IllegalArgumentException("Trusted device not found.")
                val result =
                    syncWithPeers(
                        devices = listOf(device),
                        oneWayLocalPayload = true,
                        mergeOptions = mergeOptions,
                        interactive = true,
                        successLogDetail = { peer, summary ->
                            "Restored from ${peer.displayName} (one-way pull). " +
                                "${summary.totalChanges} change(s) applied."
                        },
                    )
                if (result.peerFailures.isNotEmpty()) {
                    throw IllegalStateException(result.peerFailures.first())
                }
                result.summary
            }

        private fun resolveSyncTargets(trusted: List<TrustedLanDeviceEntity>): List<TrustedLanDeviceEntity> {
            if (trusted.isEmpty()) return trusted
            if (preferencesStore.peerMode != LanSyncPeerMode.SINGLE_DEVICE) return trusted
            val selectedId = preferencesStore.selectedPeerDeviceId?.trim()
            if (selectedId.isNullOrEmpty()) return emptyList()
            return trusted.filter { it.remoteDeviceId == selectedId }
        }

        private fun ensureSyncTargetsPresent(
            allTrusted: List<TrustedLanDeviceEntity>,
            targets: List<TrustedLanDeviceEntity>,
        ) {
            if (targets.isNotEmpty()) return
            if (allTrusted.isEmpty()) {
                throw IllegalStateException("Add at least one trusted device.")
            }
            if (preferencesStore.peerMode == LanSyncPeerMode.SINGLE_DEVICE) {
                val selectedId = preferencesStore.selectedPeerDeviceId?.trim()
                if (selectedId.isNullOrEmpty()) {
                    throw IllegalStateException("Select a device under Sync one device only.")
                }
                throw IllegalStateException("Selected device is not in your trusted list.")
            }
            throw IllegalStateException("Add at least one trusted device.")
        }

        private suspend fun ensureReadyForSync() {
            if (!identityStore.isLanSyncEnabled()) {
                throw IllegalStateException("Turn on Peer Device Sync in settings first.")
            }
            if (!networkUtils.isOnWifi()) {
                throw IllegalStateException("Connect to Wi‑Fi to sync.")
            }
            val ssid = networkUtils.wifiInfo().ssid
            if (ssid != null && !wifiTrustStore.isTrusted(ssid)) {
                throw LanSyncWifiNotTrustedException(ssid)
            }
        }

        private suspend fun syncWithPeers(
            devices: List<TrustedLanDeviceEntity>,
            oneWayLocalPayload: Boolean,
            mergeOptions: LanSyncMergeOptions,
            interactive: Boolean,
            successLogDetail: (TrustedLanDeviceEntity, LanSyncMergeSummary) -> String =
                { _, summary -> "${summary.totalChanges} change(s) applied" },
        ): LanSyncNowResult {
            if (devices.isEmpty()) {
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
            val successes = mutableListOf<String>()
            val failures = mutableListOf<String>()
            for (device in devices) {
                val peer =
                    nsdHelper.findPeer(device.remoteDeviceId)
                        ?: run {
                            failures += "${device.displayName} is not on this Wi‑Fi."
                            recordSyncLog(device.displayName, false, "Device not found on network.", null)
                            continue
                        }
                val outboundPayload: MmrLanSyncPayload =
                    if (oneWayLocalPayload) {
                        exporter.buildEmptyPayload(
                            sourceDeviceId = identityStore.deviceId,
                            sourceDeviceName = identityStore.displayName,
                        )
                    } else {
                        exporter.buildPayload(
                            sourceDeviceId = identityStore.deviceId,
                            sourceDeviceName = identityStore.displayName,
                            appSettings = MmrAppSettingsBackup(smsScanMonths = smsScanSettingsStore.getScanMonths()),
                            displaySettings = null,
                            includeAttachments = preferencesStore.includeAttachments,
                            peerDeviceId = device.remoteDeviceId,
                        )
                    }
                val localPayloadJson = LanSyncJsonCodec.encode(outboundPayload)
                val response =
                    runCatching {
                        LanSyncHttpClient.post(
                            host = peer.host,
                            port = peer.port,
                            path = LanSyncHttpConstants.PATH_SYNC,
                            deviceId = identityStore.deviceId,
                            authToken = device.theirAuthToken,
                            body = localPayloadJson,
                        )
                    }.getOrElse { error ->
                        failures += "${device.displayName}: ${error.message ?: "connection failed"}"
                        recordSyncLog(device.displayName, false, "Connection failed.", null)
                        continue
                    }
                if (response.statusCode != 200) {
                    failures += "${device.displayName} (${response.statusCode})"
                    recordSyncLog(device.displayName, false, "HTTP ${response.statusCode}", null)
                    continue
                }
                val remotePayload = LanSyncJsonCodec.decode(response.body)
                if (interactive) {
                    val conflicts = conflictDetector.detect(remotePayload, mergeOptions)
                    if (conflicts.needsUserInput) {
                        throw LanSyncNeedsResolutionException(conflicts)
                    }
                }
                val summaryResult =
                    runCatching {
                        merger.merge(remotePayload, mergeOptions).also {
                            applySettings(remotePayload)
                        }
                    }
                if (summaryResult.isFailure) {
                    failures += "${device.displayName}: merge failed"
                    recordSyncLog(
                        device.displayName,
                        false,
                        summaryResult.exceptionOrNull()?.message ?: "Merge failed",
                        null,
                    )
                    continue
                }
                if (preferencesStore.includeAttachments && remotePayload.attachments.isNotEmpty()) {
                    runCatching {
                        attachmentPuller.pullMissing(
                            host = peer.host,
                            port = peer.port,
                            deviceId = identityStore.deviceId,
                            authToken = device.theirAuthToken,
                            records = remotePayload.attachments,
                        )
                    }
                }
                val summary = summaryResult.getOrThrow()
                if (!oneWayLocalPayload) {
                    tombstoneCoordinator.onSuccessfulSyncToPeer(
                        device.remoteDeviceId,
                        outboundPayload.tombstones,
                    )
                }
                total = total + summary
                successes += device.displayName
                val warningText = summary.warnings.takeIf { it.isNotEmpty() }?.joinToString(" ")
                recordSyncLog(
                    device.displayName,
                    true,
                    successLogDetail(device, summary),
                    warningText,
                )
            }
            if (failures.isNotEmpty() && failures.size == devices.size) {
                throw IllegalStateException(failures.joinToString(" "))
            }
            publishState()
            return LanSyncNowResult(
                summary = total,
                peerSuccesses = successes,
                peerFailures = failures,
                warnings = total.warnings,
            )
        }

        private suspend fun recordSyncLog(
            peerName: String,
            success: Boolean,
            summaryText: String,
            warningsText: String?,
        ) {
            lanSyncDao.appendSyncLogEntry(
                LanSyncLogEntryEntity(
                    completedAtMillis = System.currentTimeMillis(),
                    peerName = peerName,
                    success = success,
                    summaryText = summaryText,
                    warningsText = warningsText,
                ),
            )
        }

        private suspend fun publishState() {
            val offer = pairingOffer()
            val port = httpServer?.port ?: LanSyncHttpConstants.DEFAULT_PORT
            val wifi = networkUtils.wifiInfo()
            _sessionState.value =
                LanSyncSessionState(
                    enabled = identityStore.isLanSyncEnabled(),
                    deviceId = offer.deviceId,
                    displayName = offer.displayName,
                    pairingOfferText =
                        if (identityStore.isLanSyncEnabled() && httpServer != null) {
                            ensureActivePairingPin()
                        } else {
                            ""
                        },
                    trustedDevices = lanSyncDao.getTrustedDevices(),
                    discoveredPeers = nsdHelper.discoveredPeers(),
                    onWifi = networkUtils.isOnWifi(),
                    wifiSsid = wifi.ssid,
                    localSyncUrl = networkUtils.localBaseUrl(port),
                    serverPort = port,
                    syncLog = lanSyncDao.getRecentSyncLog(),
                    hasLocalLedgerData = transactionDao.getAllApprovedTransactions().isNotEmpty(),
                    peerMode = preferencesStore.peerMode,
                    selectedPeerDeviceId = preferencesStore.selectedPeerDeviceId,
                    includeAttachments = preferencesStore.includeAttachments,
                    pendingApprovalCount = transactionApprovalDao.getPendingCountOnce(),
                )
        }

        private fun startInfrastructure() {
            if (httpServer != null) return
            val server =
                LanSyncHttpServer(
                    scope,
                    requestHandler = { method, path, query, headers, body ->
                        handleHttpRequest(method, path, query, headers, body)
                    },
                )
            server.start()
            httpServer = server
            nsdHelper.registerService(
                deviceId = identityStore.deviceId,
                displayName = identityStore.displayName,
                port = server.port,
            )
            nsdHelper.startDiscovery { scope.launch { publishState() } }
            lanServerNotifier.onLanServerStarted(LocalLanServerMode.PEER_SYNC)
        }

        private fun stopInfrastructure() {
            val active = httpServer ?: return
            nsdHelper.stopDiscovery()
            nsdHelper.unregisterService()
            active.stop()
            httpServer = null
            activePairingPin = null
            lanServerNotifier.onLanServerStopped()
        }

        private fun ensureActivePairingPin(): String {
            val existing = activePairingPin
            if (existing != null) return existing
            val pin = LanSyncPairingPin.generate()
            activePairingPin = pin
            return pin
        }

        private fun handleHttpRequest(
            method: String,
            path: String,
            query: String,
            headers: Map<String, String>,
            body: String,
        ): HttpLanResponse {
            if (!identityStore.isLanSyncEnabled()) {
                return HttpLanResponse.json(403, """{"error":"sync_disabled"}""")
            }
            if (method.equals("GET", ignoreCase = true) &&
                path.startsWith(LanSyncHttpConstants.PATH_ATTACHMENT_PREFIX)
            ) {
                return handleAttachmentGetRequest(path, headers)
            }
            if (method.equals("GET", ignoreCase = true) && path == LanSyncHttpConstants.PATH_PAIRING_OFFER) {
                return handlePairingOfferGet(query)
            }
            return when (path) {
                LanSyncHttpConstants.PATH_PAIR -> handlePairRequest(headers, body)
                LanSyncHttpConstants.PATH_SYNC -> handleSyncRequest(headers, body)
                else -> HttpLanResponse.json(404, """{"error":"not_found"}""")
            }
        }

        private fun handlePairingOfferGet(query: String): HttpLanResponse {
            val code =
                parseQueryParams(query)["code"]?.let(LanSyncPairingPin::normalize)
                    ?: return HttpLanResponse.json(400, """{"error":"missing_code"}""")
            val expected = activePairingPin ?: return HttpLanResponse.json(404, """{"error":"not_found"}""")
            if (!code.equals(expected, ignoreCase = true)) {
                return HttpLanResponse.json(404, """{"error":"not_found"}""")
            }
            val offer = pairingOffer()
            val body =
                JSONObject()
                    .apply {
                        put("deviceId", offer.deviceId)
                        put("displayName", offer.displayName)
                        put("inboundAuthToken", offer.inboundAuthToken)
                    }.toString()
            return HttpLanResponse.json(200, body)
        }

        private fun parseQueryParams(query: String): Map<String, String> {
            if (query.isBlank()) return emptyMap()
            return query
                .split('&')
                .mapNotNull { part ->
                    val eq = part.indexOf('=')
                    if (eq <= 0) return@mapNotNull null
                    val key = part.substring(0, eq)
                    val value = part.substring(eq + 1)
                    key to java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
                }.toMap()
        }

        private suspend fun resolveOfferFromLanPeers(pin: String): LanSyncPairingOffer {
            val peers = nsdHelper.discoveredPeers().filter { it.deviceId != identityStore.deviceId }
            if (peers.isEmpty()) {
                throw IllegalArgumentException(
                    "No devices found on this network. Turn on Peer Device Sync on both phones and use the same Wi‑Fi.",
                )
            }
            val path =
                LanSyncHttpConstants.PATH_PAIRING_OFFER +
                    "?code=${java.net.URLEncoder.encode(pin, Charsets.UTF_8.name())}"
            for (peer in peers) {
                val response =
                    runCatching {
                        LanSyncHttpClient.get(host = peer.host, port = peer.port, path = path)
                    }.getOrNull()
                if (response?.statusCode == 200) {
                    val json = runCatching { JSONObject(response.body) }.getOrNull() ?: continue
                    val deviceId = json.optString("deviceId")
                    val inboundAuthToken = json.optString("inboundAuthToken")
                    if (deviceId.isBlank() || inboundAuthToken.isBlank()) continue
                    return LanSyncPairingOffer(
                        deviceId = deviceId,
                        displayName = json.optString("displayName").ifBlank { "MMR device" },
                        inboundAuthToken = inboundAuthToken,
                    )
                }
            }
            throw IllegalArgumentException(
                "Pairing code not found. Check the code and that both devices are on the same Wi‑Fi.",
            )
        }

        private fun handleAttachmentGetRequest(
            path: String,
            headers: Map<String, String>,
        ): HttpLanResponse {
            if (!preferencesStore.includeAttachments) {
                return HttpLanResponse.json(404, """{"error":"attachments_disabled"}""")
            }
            val remoteDeviceId =
                headers[LanSyncHttpConstants.HEADER_DEVICE_ID.lowercase()]
                    ?: return HttpLanResponse.json(401, "")
            val remoteToken =
                headers[LanSyncHttpConstants.HEADER_AUTH_TOKEN.lowercase()]
                    ?: return HttpLanResponse.json(401, "")
            val trusted =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) {
                        lanSyncDao.getTrustedDevice(remoteDeviceId)
                    }
                }.getOrNull()
            if (trusted == null || trusted.theirAuthToken != remoteToken) {
                return HttpLanResponse.json(401, """{"error":"unauthorized"}""")
            }
            val syncId = path.removePrefix(LanSyncHttpConstants.PATH_ATTACHMENT_PREFIX)
            if (syncId.isBlank()) {
                return HttpLanResponse.json(404, """{"error":"not_found"}""")
            }
            val bytes =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) { attachmentStore.readBytes(syncId) }
                }.getOrNull()
            if (bytes == null) {
                return HttpLanResponse.json(404, """{"error":"not_found"}""")
            }
            return HttpLanResponse.bytes(200, bytes, "application/octet-stream")
        }

        private fun handlePairRequest(
            headers: Map<String, String>,
            body: String,
        ): HttpLanResponse {
            val remoteDeviceId =
                headers[LanSyncHttpConstants.HEADER_DEVICE_ID.lowercase()]
                    ?: return HttpLanResponse.json(401, "")
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
            val json = runCatching { JSONObject(body) }.getOrNull() ?: return HttpLanResponse.json(400, "")
            val deviceId = json.optString("deviceId")
            val displayName = json.optString("displayName")
            val inboundAuthToken = json.optString("inboundAuthToken")
            if (deviceId.isBlank() || inboundAuthToken.isBlank()) return HttpLanResponse.json(400, "")
            scope.launch {
                lanSyncDao.upsertTrustedDevice(
                    TrustedLanDeviceEntity(
                        remoteDeviceId = deviceId,
                        displayName = displayName.ifBlank { "MMR device" },
                        theirAuthToken = inboundAuthToken,
                    ),
                )
                lanSyncDao.deletePendingPairRequest(deviceId)
                publishState()
            }
            return HttpLanResponse.json(200, """{"status":"trusted"}""")
        }

        private fun handleSyncRequest(
            headers: Map<String, String>,
            body: String,
        ): HttpLanResponse {
            val remoteDeviceId =
                headers[LanSyncHttpConstants.HEADER_DEVICE_ID.lowercase()]
                    ?: return HttpLanResponse.json(401, "")
            val remoteToken =
                headers[LanSyncHttpConstants.HEADER_AUTH_TOKEN.lowercase()]
                    ?: return HttpLanResponse.json(401, "")
            val trusted =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) {
                        lanSyncDao.getTrustedDevice(remoteDeviceId)
                    }
                }.getOrNull()
            if (trusted == null || trusted.theirAuthToken != remoteToken) {
                return HttpLanResponse.json(401, """{"error":"unauthorized"}""")
            }
            val mergeResult =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) {
                        val payload = LanSyncJsonCodec.decode(body)
                        val summary = merger.merge(payload, LanSyncMergeOptions())
                        applySettings(payload)
                        summary
                    }
                }
            if (mergeResult.isFailure) {
                return HttpLanResponse.json(400, """{"error":"merge_failed"}""")
            }
            val responseBody =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) { exportPayloadJson(remoteDeviceId) }
                }.getOrElse { return HttpLanResponse.json(500, """{"error":"export_failed"}""") }
            return HttpLanResponse.json(200, responseBody)
        }

        private suspend fun exportPayloadJson(peerDeviceId: String): String {
            val includeAttachments = preferencesStore.includeAttachments
            val payload =
                exporter.buildPayload(
                    sourceDeviceId = identityStore.deviceId,
                    sourceDeviceName = identityStore.displayName,
                    appSettings = MmrAppSettingsBackup(smsScanMonths = smsScanSettingsStore.getScanMonths()),
                    displaySettings = null,
                    includeAttachments = includeAttachments,
                    peerDeviceId = peerDeviceId,
                )
            return LanSyncJsonCodec.encode(payload)
        }

        private suspend fun applySettings(payload: com.mmr.core.lansync.model.MmrLanSyncPayload) {
            val remoteMonths = payload.appSettings?.smsScanMonths ?: return
            val merged = maxOf(smsScanSettingsStore.getScanMonths(), remoteMonths)
            smsScanSettingsStore.setScanMonths(merged)
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
                tombstonesApplied = tombstonesApplied + other.tombstonesApplied,
                keptAfterRemoteDelete = keptAfterRemoteDelete + other.keptAfterRemoteDelete,
                warnings = (warnings + other.warnings).distinct(),
            )

        init {
            if (identityStore.isLanSyncEnabled()) {
                startInfrastructure()
            }
            scope.launch { publishState() }
        }
    }
