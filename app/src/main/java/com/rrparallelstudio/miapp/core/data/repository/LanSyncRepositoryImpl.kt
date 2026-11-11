package com.rrparallelstudio.miapp.core.data.repository

import android.util.Log
import com.rrparallelstudio.miapp.core.attachment.AssetAttachmentStore
import com.rrparallelstudio.miapp.core.attachment.MmrAttachmentSyncId
import com.rrparallelstudio.miapp.core.attachment.TransactionAttachmentStore
import com.rrparallelstudio.miapp.core.backup.model.MmrAppSettingsBackup
import com.rrparallelstudio.miapp.core.common.dispatcher.IoDispatcher
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncIdentityStore
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncLastPeerAddressStore
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncPeerMode
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncPreferencesStore
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncWifiTrustStore
import com.rrparallelstudio.miapp.core.data.preferences.SmsScanSettingsStore
import com.rrparallelstudio.miapp.core.database.dao.LanSyncDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionApprovalDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionDao
import com.rrparallelstudio.miapp.core.database.entity.LanSyncLogEntryEntity
import com.rrparallelstudio.miapp.core.database.entity.PendingLanPairRequestEntity
import com.rrparallelstudio.miapp.core.database.entity.TrustedLanDeviceEntity
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncNowResult
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncRepository
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncSessionState
import com.rrparallelstudio.miapp.core.domain.repository.WebManageRepository
import com.rrparallelstudio.miapp.core.lansync.LanSyncAttachmentPuller
import com.rrparallelstudio.miapp.core.lansync.LanSyncExporter
import com.rrparallelstudio.miapp.core.lansync.LanSyncJsonCodec
import com.rrparallelstudio.miapp.core.lansync.LanSyncManualPeerAddress
import com.rrparallelstudio.miapp.core.lansync.LanSyncMergeOptions
import com.rrparallelstudio.miapp.core.lansync.LanSyncMergeSummary
import com.rrparallelstudio.miapp.core.lansync.LanSyncMerger
import com.rrparallelstudio.miapp.core.lansync.LanSyncNetworkUtils
import com.rrparallelstudio.miapp.core.lansync.LanSyncOverrideJson
import com.rrparallelstudio.miapp.core.lansync.LanSyncPairingGate
import com.rrparallelstudio.miapp.core.lansync.LanSyncPairingOffer
import com.rrparallelstudio.miapp.core.lansync.LanSyncPairingPin
import com.rrparallelstudio.miapp.core.lansync.LanSyncTombstoneCoordinator
import com.rrparallelstudio.miapp.core.lansync.LanSyncWifiNotTrustedException
import com.rrparallelstudio.miapp.core.lansync.LocalLanServerCoordinator
import com.rrparallelstudio.miapp.core.lansync.LocalLanServerMode
import com.rrparallelstudio.miapp.core.lansync.LocalLanServerNotifier
import com.rrparallelstudio.miapp.core.lansync.model.MmrLanSyncPayload
import com.rrparallelstudio.miapp.core.lansync.transport.DiscoveredLanPeer
import com.rrparallelstudio.miapp.core.lansync.transport.HttpLanResponse
import com.rrparallelstudio.miapp.core.lansync.transport.LanSyncHttpClient
import com.rrparallelstudio.miapp.core.lansync.transport.LanSyncHttpConstants
import com.rrparallelstudio.miapp.core.lansync.transport.LanSyncHttpServer
import com.rrparallelstudio.miapp.core.lansync.transport.LanSyncNsdHelper
import com.rrparallelstudio.miapp.core.lansync.withoutDeviceAppearance
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        private val lastPeerAddressStore: LanSyncLastPeerAddressStore,
        private val localLanServerCoordinator: LocalLanServerCoordinator,
        private val webManageRepository: Lazy<WebManageRepository>,
        private val lanServerNotifier: LocalLanServerNotifier,
        private val transactionDao: TransactionDao,
        private val transactionApprovalDao: TransactionApprovalDao,
        private val networkUtils: LanSyncNetworkUtils,
        private val nsdHelper: LanSyncNsdHelper,
        private val attachmentPuller: LanSyncAttachmentPuller,
        private val attachmentStore: TransactionAttachmentStore,
        private val assetAttachmentStore: AssetAttachmentStore,
        private val tombstoneCoordinator: LanSyncTombstoneCoordinator,
        private val ownerProfileCoordinator: com.rrparallelstudio.miapp.core.owner.MmrOwnerProfileCoordinator,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : LanSyncRepository {
        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private val syncMutex = Mutex()
        private var httpServer: LanSyncHttpServer? = null
        private var activePairingPin: String? = null
        private var activePairingPinIssuedAtMillis: Long = 0L
        private var resolvedPairDeviceId: String? = null
        private var resolvedPairHost: String? = null
        private var resolvedPairPort: Int = -1

        private val _sessionState = MutableStateFlow(LanSyncSessionState())
        override val sessionState: StateFlow<LanSyncSessionState> = _sessionState.asStateFlow()

        private val onLocalNetworkChanged: () -> Unit = {
            scope.launch { publishState() }
        }

        override suspend fun refreshSessionState() {
            withContext(ioDispatcher) {
                ensurePeerSyncInfrastructure()
                publishState()
            }
        }

        override fun refreshPeerDiscovery() {
            if (!identityStore.isLanSyncEnabled()) return
            ensurePeerSyncInfrastructure()
            val server = httpServer ?: return
            nsdHelper.registerService(
                deviceId = identityStore.deviceId,
                displayName = identityStore.displayName,
                port = server.port,
            )
            nsdHelper.refreshDiscovery { scope.launch { publishState() } }
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
                preferencesStore.autoSyncOnAppOpen = false
                stopInfrastructure()
            }
            scope.launch { publishState() }
        }

        override fun setDisplayName(name: String) {
            scope.launch {
                ownerProfileCoordinator.onLocalDisplayNameChanged(name)
                publishState()
            }
        }

        override fun pairingOffer(): LanSyncPairingOffer =
            LanSyncPairingOffer(
                deviceId = identityStore.deviceId,
                displayName = identityStore.displayName,
                inboundAuthToken = identityStore.inboundAuthToken,
            )

        override suspend fun resolvePairingInput(
            raw: String,
            manualPeerAddress: String?,
        ): LanSyncPairingOffer =
            withContext(ioDispatcher) {
                ensurePeerSyncInfrastructure()
                val pin = LanSyncPairingPin.normalize(raw.trim())
                if (!LanSyncPairingPin.isValidFormat(pin)) {
                    throw IllegalArgumentException("Enter a valid pairing code from the other device.")
                }
                resolveOfferFromLanPeers(pin, manualPeerAddress)
            }

        override suspend fun trustDeviceFromPairingOffer(
            offer: LanSyncPairingOffer,
            pairingCode: String?,
        ) {
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
                if (!pairingCode.isNullOrBlank()) {
                    sendPairRequestToPeer(offer, pairingCode)
                }
                publishState()
            }
        }

        override suspend fun approvePendingPairRequest(remoteDeviceId: String) {
            withContext(ioDispatcher) {
                val pending =
                    lanSyncDao.getPendingPairRequests().find { it.remoteDeviceId == remoteDeviceId }
                        ?: throw IllegalArgumentException("No pending pair request for that device.")
                lanSyncDao.approvePairRequest(pending)
                publishState()
            }
        }

        override suspend fun rejectPendingPairRequest(remoteDeviceId: String) {
            withContext(ioDispatcher) {
                lanSyncDao.deletePendingPairRequest(remoteDeviceId)
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
                lastPeerAddressStore.remove(deviceId)
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

        override fun setAutoSyncOnAppOpen(enabled: Boolean) {
            preferencesStore.autoSyncOnAppOpen = enabled
            scope.launch { publishState() }
        }

        override suspend fun syncActiveTrustedPeersOnAppOpen(mergeOptions: LanSyncMergeOptions): LanSyncNowResult? =
            syncMutex.withLock {
                withContext(ioDispatcher) {
                    if (!preferencesStore.autoSyncOnAppOpen || !identityStore.isLanSyncEnabled()) {
                        return@withContext null
                    }
                    runCatching { ensureReadyForSync() }.getOrElse { return@withContext null }
                    ensurePeerSyncInfrastructure()
                    refreshPeerDiscovery()
                    val allTrusted = lanSyncDao.getTrustedDevices()
                    if (allTrusted.isEmpty()) return@withContext null
                    val baseTargets = resolveSyncTargets(allTrusted)
                    if (baseTargets.isEmpty()) return@withContext null
                    val discoveredIds = nsdHelper.discoveredPeers().map { it.deviceId }.toSet()
                    val activeTargets = baseTargets.filter { it.remoteDeviceId in discoveredIds }
                    if (activeTargets.isEmpty()) return@withContext null
                    runCatching {
                        syncWithPeers(
                            devices = activeTargets,
                            oneWayLocalPayload = false,
                            mergeOptions = mergeOptions,
                        )
                    }.getOrNull()
                }
            }

        override suspend fun syncNow(
            mergeOptions: LanSyncMergeOptions,
            manualPeerAddress: String?,
        ): LanSyncNowResult =
            syncMutex.withLock {
                withContext(ioDispatcher) {
                    ensureReadyForSync()
                    val manual = manualPeerAddress?.trim()?.ifBlank { null }
                    if (manual == null) {
                        refreshPeerDiscovery()
                        delay(DISCOVERY_BEFORE_SYNC_MS)
                    }
                    val allTrusted = lanSyncDao.getTrustedDevices()
                    val targets = resolveSyncTargets(allTrusted)
                    ensureSyncTargetsPresent(allTrusted, targets)
                    val manualForPeer = manual?.takeIf { targets.size == 1 }
                    syncWithPeers(
                        devices = targets,
                        oneWayLocalPayload = false,
                        mergeOptions = mergeOptions,
                        manualPeerAddress = manualForPeer,
                    )
                }
            }

        override suspend fun restoreFromPeer(
            remoteDeviceId: String,
            mergeOptions: LanSyncMergeOptions,
        ): LanSyncMergeSummary =
            syncMutex.withLock {
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
            }

        private fun resolvePeerForSync(
            device: TrustedLanDeviceEntity,
            manualPeerAddress: String? = null,
        ): DiscoveredLanPeer? {
            LanSyncManualPeerAddress.parse(manualPeerAddress)?.let { manual ->
                return DiscoveredLanPeer(
                    deviceId = device.remoteDeviceId,
                    displayName = device.displayName,
                    host = manual.host,
                    port = manual.port,
                )
            }
            nsdHelper.findPeer(device.remoteDeviceId)?.let { return it }
            val stored = lastPeerAddressStore.get(device.remoteDeviceId) ?: return null
            val endpoint = LanSyncManualPeerAddress.parse(stored) ?: return null
            return DiscoveredLanPeer(
                deviceId = device.remoteDeviceId,
                displayName = device.displayName,
                host = endpoint.host,
                port = endpoint.port,
            )
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
            if (!networkUtils.canUseLocalPeerSync()) {
                throw IllegalStateException("Connect to Wi‑Fi or join a phone hotspot before syncing.")
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
            manualPeerAddress: String? = null,
            successLogDetail: (TrustedLanDeviceEntity, LanSyncMergeSummary) -> String =
                { _, summary ->
                    if (summary.totalChanges == 0) {
                        "Already in sync"
                    } else {
                        "${summary.totalChanges} items updated from the other phone"
                    }
                },
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
                    resolvePeerForSync(device, manualPeerAddress)
                        ?: run {
                            failures += "${device.displayName} is not on this network right now."
                            recordSyncLog(device.displayName, false, "Other phone not found on the network.", null)
                            continue
                        }
                val outboundPayload: MmrLanSyncPayload =
                    if (oneWayLocalPayload) {
                        exporter.buildEmptyPayload(
                            sourceDeviceId = identityStore.deviceId,
                            sourceDeviceName = identityStore.displayName,
                            sourceListenPort = currentListenPort(),
                        )
                    } else {
                        exporter.buildPayload(
                            sourceDeviceId = identityStore.deviceId,
                            sourceDeviceName = identityStore.displayName,
                            appSettings = MmrAppSettingsBackup(smsScanMonths = smsScanSettingsStore.getScanMonths()),
                            displaySettings = null,
                            includeAttachments = preferencesStore.includeAttachments,
                            peerDeviceId = device.remoteDeviceId,
                            sourceListenPort = currentListenPort(),
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
                            authToken = identityStore.inboundAuthToken,
                            body = localPayloadJson,
                            network = networkUtils.wifiNetwork(),
                        )
                    }.getOrElse { error ->
                        failures += "${device.displayName}: could not connect over Wi‑Fi."
                        recordSyncLog(device.displayName, false, "Could not connect.", null)
                        continue
                    }
                if (response.statusCode != 200) {
                    val detail = syncHttpFailureDetail(device.displayName, response.statusCode, response.body)
                    failures += detail
                    recordSyncLog(device.displayName, false, "HTTP ${response.statusCode}", null)
                    continue
                }
                val remotePayload =
                    runCatching {
                        LanSyncJsonCodec.decode(response.body).withoutDeviceAppearance()
                    }.getOrElse { error ->
                        failures += "${device.displayName}: invalid sync payload"
                        recordSyncLog(
                            device.displayName,
                            false,
                            error.message ?: "Invalid sync payload",
                            null,
                        )
                        continue
                    }
                val summaryResult =
                    runCatching {
                        merger.merge(remotePayload, mergeOptions).also {
                            applySettings(remotePayload)
                        }
                    }
                if (summaryResult.isFailure) {
                    val detail =
                        summaryResult.exceptionOrNull()?.message ?: "Data from the other phone could not be applied."
                    failures += "${device.displayName}: $detail"
                    recordSyncLog(
                        device.displayName,
                        false,
                        detail,
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
                            authToken = identityStore.inboundAuthToken,
                            records = remotePayload.attachments,
                        )
                    }
                }
                if (preferencesStore.includeAttachments && remotePayload.assetAttachments.isNotEmpty()) {
                    runCatching {
                        attachmentPuller.pullMissingAssetAttachments(
                            host = peer.host,
                            port = peer.port,
                            deviceId = identityStore.deviceId,
                            authToken = identityStore.inboundAuthToken,
                            records = remotePayload.assetAttachments,
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
                lastPeerAddressStore.put(device.remoteDeviceId, peer.host, peer.port)
                val warningText = summary.warnings.takeIf { it.isNotEmpty() }?.joinToString("\n")
                recordSyncLog(
                    device.displayName,
                    true,
                    successLogDetail(device, summary),
                    warningText,
                    summary,
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
            summary: LanSyncMergeSummary? = null,
        ) {
            lanSyncDao.appendSyncLogEntry(
                LanSyncLogEntryEntity(
                    completedAtMillis = System.currentTimeMillis(),
                    peerName = peerName,
                    success = success,
                    summaryText = summaryText,
                    warningsText = warningsText,
                    unchangedCount = summary?.unchangedCount ?: 0,
                    addedCount = summary?.addedCount ?: 0,
                    overridesJson = LanSyncOverrideJson.encode(summary?.overrides.orEmpty()),
                ),
            )
        }

        private suspend fun publishState() {
            val offer = pairingOffer()
            val port = httpServer?.takeIf { it.isListening() }?.port ?: LanSyncHttpConstants.DEFAULT_PORT
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
                    onWifi = networkUtils.canUseLocalPeerSync(),
                    wifiSsid = wifi.ssid,
                    localSyncUrl = networkUtils.localBaseUrl(port),
                    serverPort = port,
                    syncLog = lanSyncDao.getRecentSyncLog(),
                    hasLocalLedgerData = transactionDao.getAllApprovedTransactions().isNotEmpty(),
                    peerMode = preferencesStore.peerMode,
                    selectedPeerDeviceId = preferencesStore.selectedPeerDeviceId,
                    includeAttachments = preferencesStore.includeAttachments,
                    autoSyncOnAppOpen = preferencesStore.autoSyncOnAppOpen,
                    pendingApprovalCount = transactionApprovalDao.getPendingCountOnce(),
                    pendingPairRequests = lanSyncDao.getPendingPairRequests(),
                )
        }

        private fun ensurePeerSyncInfrastructure() {
            if (!identityStore.isLanSyncEnabled()) return
            val server = httpServer
            if (server == null || !server.isListening()) {
                stopInfrastructure()
                startInfrastructure()
            }
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
            activePairingPinIssuedAtMillis = 0L
            lanServerNotifier.onLanServerStopped()
        }

        private fun ensureActivePairingPin(): String {
            val now = System.currentTimeMillis()
            val existing = activePairingPin
            if (existing != null &&
                !LanSyncPairingGate.isPinExpired(activePairingPinIssuedAtMillis, now)
            ) {
                return existing
            }
            val pin = LanSyncPairingPin.generate()
            activePairingPin = pin
            activePairingPinIssuedAtMillis = now
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
                LanSyncHttpConstants.PATH_PAIR ->
                    if (!method.equals("POST", ignoreCase = true)) {
                        HttpLanResponse.json(405, """{"error":"method_not_allowed"}""")
                    } else {
                        handlePairRequest(body)
                    }
                LanSyncHttpConstants.PATH_SYNC -> handleSyncRequest(headers, body)
                else -> HttpLanResponse.json(404, """{"error":"not_found"}""")
            }
        }

        private fun handlePairingOfferGet(query: String): HttpLanResponse {
            val code =
                parseQueryParams(query)["code"]?.let(LanSyncPairingPin::normalize)
                    ?: return HttpLanResponse.json(400, """{"error":"missing_code"}""")
            val expected = activePairingPin ?: return HttpLanResponse.json(404, """{"error":"not_found"}""")
            if (LanSyncPairingGate.isPinExpired(activePairingPinIssuedAtMillis)) {
                return HttpLanResponse.json(403, """{"error":"pairing_code_expired"}""")
            }
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

        private suspend fun resolveOfferFromLanPeers(
            pin: String,
            manualPeerAddress: String?,
        ): LanSyncPairingOffer {
            val path =
                LanSyncHttpConstants.PATH_PAIRING_OFFER +
                    "?code=${java.net.URLEncoder.encode(pin, Charsets.UTF_8.name())}"
            val targets = linkedSetOf<Pair<String, Int>>()
            val manual = LanSyncManualPeerAddress.parse(manualPeerAddress)
            if (manual != null) {
                targets.add(manual.host to manual.port)
            } else {
                nsdHelper
                    .discoveredPeers()
                    .filter { it.deviceId != identityStore.deviceId }
                    .forEach { targets.add(it.host to it.port) }
            }
            if (targets.isEmpty()) {
                throw IllegalArgumentException(
                    "No phones found on this network. Open Peer Device Sync on the other phone, " +
                        "turn on Sync on same Wi‑Fi, and wait until it appears under On this network.",
                )
            }
            var wrongCode = false
            var unreachable = false
            var lastReachError: String? = null
            val wifi = networkUtils.wifiNetwork()
            for ((host, port) in targets) {
                val attempt =
                    runCatching {
                        LanSyncHttpClient.get(host = host, port = port, path = path, network = wifi)
                    }
                val response = attempt.getOrNull()
                if (attempt.isFailure) {
                    lastReachError = attempt.exceptionOrNull()?.message
                }
                when (response?.statusCode) {
                    200 -> {
                        val json = runCatching { JSONObject(response.body) }.getOrNull() ?: continue
                        val deviceId = json.optString("deviceId")
                        val inboundAuthToken = json.optString("inboundAuthToken")
                        if (deviceId.isBlank() || inboundAuthToken.isBlank()) continue
                        resolvedPairDeviceId = deviceId
                        resolvedPairHost = host
                        resolvedPairPort = port
                        return LanSyncPairingOffer(
                            deviceId = deviceId,
                            displayName = json.optString("displayName").ifBlank { "MMR device" },
                            inboundAuthToken = inboundAuthToken,
                        )
                    }
                    404 -> wrongCode = true
                    else -> if (response == null) unreachable = true
                }
            }
            throw when {
                wrongCode ->
                    IllegalArgumentException(
                        "That code does not match the other phone. Copy the pairing code from " +
                            "the other device's screen (not this phone's code under This device).",
                    )
                unreachable ->
                    IllegalArgumentException(
                        buildString {
                            append(
                                "Could not reach the other phone over Wi‑Fi. " +
                                    "Reselect it under On this network, turn off VPN on both devices, " +
                                    "and try again.",
                            )
                            val detail = lastReachError?.trim()?.take(140)
                            if (!detail.isNullOrEmpty()) {
                                append(" (").append(detail).append(")")
                            }
                        },
                    )
                else ->
                    IllegalArgumentException(
                        "Pairing code not found. Check the code and that both devices are on the same Wi‑Fi.",
                    )
            }
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
            val rawSyncId = path.removePrefix(LanSyncHttpConstants.PATH_ATTACHMENT_PREFIX)
            if (rawSyncId.isBlank()) {
                return HttpLanResponse.json(404, """{"error":"not_found"}""")
            }
            val syncId =
                runCatching { MmrAttachmentSyncId.requireSafeSyncId(rawSyncId) }.getOrNull()
                    ?: return HttpLanResponse.json(404, """{"error":"not_found"}""")
            val bytes =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) {
                        attachmentStore.readBytes(syncId)
                            ?: assetAttachmentStore.readBytes(syncId)
                    }
                }.getOrNull()
            if (bytes == null) {
                return HttpLanResponse.json(404, """{"error":"not_found"}""")
            }
            return HttpLanResponse.bytes(200, bytes, "application/octet-stream")
        }

        private fun handlePairRequest(body: String): HttpLanResponse {
            val json =
                runCatching { JSONObject(body) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            val deviceId = json.optString("deviceId")
            val displayName = json.optString("displayName")
            val inboundAuthToken = json.optString("inboundAuthToken")
            val pairingCode = json.optString("pairingCode")
            if (deviceId.isBlank() || inboundAuthToken.isBlank()) {
                return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            }
            if (deviceId == identityStore.deviceId) {
                return HttpLanResponse.json(400, """{"error":"self_pair"}""")
            }
            val activePin = activePairingPin
            if (activePin == null ||
                LanSyncPairingGate.isPinExpired(activePairingPinIssuedAtMillis)
            ) {
                return HttpLanResponse.json(403, """{"error":"pairing_code_expired"}""")
            }
            if (!LanSyncPairingGate.pairingCodeMatches(pairingCode, activePin)) {
                return HttpLanResponse.json(403, """{"error":"invalid_pairing_code"}""")
            }
            val existingTrusted =
                runCatching {
                    kotlinx.coroutines.runBlocking(ioDispatcher) {
                        lanSyncDao.getTrustedDevice(deviceId)
                    }
                }.getOrNull()
            if (existingTrusted != null) {
                return HttpLanResponse.json(200, """{"status":"already_trusted"}""")
            }
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
            return HttpLanResponse.json(202, """{"status":"pending_approval"}""")
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
                        val payload = LanSyncJsonCodec.decode(body).withoutDeviceAppearance()
                        rememberCallerListenAddress(remoteDeviceId, headers, payload)
                        val summary = merger.merge(payload, LanSyncMergeOptions())
                        applySettings(payload)
                        val peerName =
                            lanSyncDao.getTrustedDevice(remoteDeviceId)?.displayName ?: "Peer"
                        recordSyncLog(
                            peerName,
                            true,
                            "${summary.overrides.size} replaced on this phone",
                            summary.warnings.takeIf { it.isNotEmpty() }?.joinToString(" "),
                            summary,
                        )
                        summary
                    }
                }
            if (mergeResult.isFailure) {
                val failure = mergeResult.exceptionOrNull()
                Log.e(LAN_SYNC_LOG_TAG, "Inbound merge failed", failure)
                val detail =
                    (failure?.message ?: failure?.javaClass?.simpleName ?: "merge failed")
                        .replace("\"", "'")
                        .take(180)
                return HttpLanResponse.json(
                    400,
                    JSONObject().put("error", "merge_failed").put("detail", detail).toString(),
                )
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
                    sourceListenPort = currentListenPort(),
                )
            return LanSyncJsonCodec.encode(payload)
        }

        private fun currentListenPort(): Int? = httpServer?.port?.takeIf { it in 1..65535 }

        /**
         * Hotspot mDNS often misses the caller. Their sync body carries the listen port; the socket
         * has their hotspot IP. Remember both so this phone can Sync now without typing the address.
         */
        private fun rememberCallerListenAddress(
            remoteDeviceId: String,
            headers: Map<String, String>,
            payload: com.rrparallelstudio.miapp.core.lansync.model.MmrLanSyncPayload,
        ) {
            if (payload.sourceDeviceId != remoteDeviceId) return
            val port = payload.sourceListenPort ?: return
            val host =
                headers["x-mmr-client-address"]
                    ?.trim()
                    ?.substringBefore('%')
                    ?.takeIf { it.isNotEmpty() && it != "unknown" && !it.contains('/') }
                    ?: return
            lastPeerAddressStore.put(remoteDeviceId, host, port)
        }

        /** SMS scan window only. Theme, colors, and Transaction Appearance stay on this phone. */
        private suspend fun applySettings(payload: com.rrparallelstudio.miapp.core.lansync.model.MmrLanSyncPayload) {
            val remoteMonths = payload.appSettings?.smsScanMonths ?: return
            val merged = maxOf(smsScanSettingsStore.getScanMonths(), remoteMonths)
            smsScanSettingsStore.setScanMonths(merged)
        }

        private suspend fun sendPairRequestToPeer(
            offer: LanSyncPairingOffer,
            pairingCode: String,
        ) {
            val remembered =
                if (resolvedPairDeviceId == offer.deviceId &&
                    !resolvedPairHost.isNullOrBlank() &&
                    resolvedPairPort > 0
                ) {
                    resolvedPairHost!! to resolvedPairPort
                } else {
                    null
                }
            val peer = remembered ?: nsdHelper.findPeer(offer.deviceId)?.let { it.host to it.port } ?: return
            val body =
                JSONObject()
                    .apply {
                        put("deviceId", identityStore.deviceId)
                        put("displayName", identityStore.displayName)
                        put("inboundAuthToken", identityStore.inboundAuthToken)
                        put("pairingCode", LanSyncPairingPin.normalize(pairingCode))
                    }.toString()
            LanSyncHttpClient.post(
                host = peer.first,
                port = peer.second,
                path = LanSyncHttpConstants.PATH_PAIR,
                deviceId = identityStore.deviceId,
                authToken = offer.inboundAuthToken,
                body = body,
                network = networkUtils.wifiNetwork(),
            )
        }

        private fun syncHttpFailureDetail(
            displayName: String,
            statusCode: Int,
            body: String,
        ): String {
            if (statusCode == 401) {
                return "$displayName: open Peer Device Sync on the other phone and approve this device, then try again."
            }
            val remoteDetail =
                runCatching { JSONObject(body).optString("detail") }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            return if (remoteDetail != null) {
                "$displayName: $remoteDetail"
            } else {
                "$displayName ($statusCode)"
            }
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
                addedCount = addedCount + other.addedCount,
                unchangedCount = unchangedCount + other.unchangedCount,
                overrides = overrides + other.overrides,
            )

        init {
            networkUtils.addLocalNetworkChangeListener(onLocalNetworkChanged)
            if (identityStore.isLanSyncEnabled()) {
                startInfrastructure()
            }
            scope.launch { publishState() }
        }

        private companion object {
            const val LAN_SYNC_LOG_TAG = "LanSync"
            const val DISCOVERY_BEFORE_SYNC_MS = 1_500L
        }
    }
