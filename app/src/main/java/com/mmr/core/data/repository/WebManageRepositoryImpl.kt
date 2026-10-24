package com.mmr.core.data.repository

import com.mmr.core.common.dispatcher.IoDispatcher
import com.mmr.core.data.preferences.WebManagePreferencesStore
import com.mmr.core.database.dao.TransactionApprovalDao
import com.mmr.core.domain.repository.LanSyncRepository
import com.mmr.core.domain.repository.WebManageRepository
import com.mmr.core.domain.repository.WebManageSessionState
import com.mmr.core.lansync.LanSyncNetworkUtils
import com.mmr.core.lansync.LocalLanServerCoordinator
import com.mmr.core.lansync.LocalLanServerMode
import com.mmr.core.lansync.LocalLanServerNotifier
import com.mmr.core.lansync.transport.LanSyncHttpConstants
import com.mmr.core.lansync.transport.LanSyncHttpServer
import com.mmr.core.webmanage.WebManagePasswordHasher
import com.mmr.core.webmanage.WebManageRequestHandler
import com.mmr.core.webmanage.WebManageSessionManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebManageRepositoryImpl
    @Inject
    constructor(
        private val preferencesStore: WebManagePreferencesStore,
        private val coordinator: LocalLanServerCoordinator,
        private val lanSyncRepository: LanSyncRepository,
        private val requestHandler: WebManageRequestHandler,
        private val sessionManager: WebManageSessionManager,
        private val networkUtils: LanSyncNetworkUtils,
        private val transactionApprovalDao: TransactionApprovalDao,
        private val lanServerNotifier: LocalLanServerNotifier,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : WebManageRepository {
        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private var httpServer: LanSyncHttpServer? = null

        private val _sessionState = MutableStateFlow(WebManageSessionState())
        override val sessionState: StateFlow<WebManageSessionState> = _sessionState.asStateFlow()

        override suspend fun refreshSessionState() {
            withContext(ioDispatcher) {
                publishState()
            }
        }

        override fun setWebManageEnabled(enabled: Boolean) {
            if (enabled) {
                if (!preferencesStore.hasAccessPassword()) {
                    scope.launch { publishState() }
                    return
                }
                lanSyncRepository.setLanSyncEnabled(false)
                coordinator.markWebManageActive()
                startInfrastructure()
            } else {
                coordinator.clearWebManage()
                stopInfrastructure()
                sessionManager.invalidateAll()
            }
            scope.launch { publishState() }
        }

        override fun setAccessPassword(password: String) {
            val trimmed = password.trim()
            if (trimmed.length < 6) {
                throw IllegalArgumentException("Password must be at least 6 characters.")
            }
            val salt = WebManagePasswordHasher.newSalt()
            val hash = WebManagePasswordHasher.hash(trimmed, salt)
            preferencesStore.setAccessPassword(salt, hash)
            sessionManager.invalidateAll()
            scope.launch { publishState() }
        }

        override fun onPeerSyncStarting() {
            stopInfrastructure()
            sessionManager.invalidateAll()
            scope.launch { publishState() }
        }

        private fun startInfrastructure() {
            if (httpServer != null) return
            val server =
                LanSyncHttpServer(
                    scope,
                    requestHandler = { method, path, query, headers, body ->
                        requestHandler.handle(method, path, query, headers, body)
                    },
                )
            server.start()
            httpServer = server
            lanServerNotifier.onLanServerStarted(LocalLanServerMode.WEB_MANAGE)
        }

        private fun stopInfrastructure() {
            val active = httpServer ?: return
            active.stop()
            httpServer = null
            lanServerNotifier.onLanServerStopped()
        }

        private suspend fun publishState() {
            val listening = httpServer != null && preferencesStore.isWebManageEnabled()
            val port = httpServer?.port ?: LanSyncHttpConstants.DEFAULT_PORT
            val wifi = networkUtils.wifiInfo()
            val ipv4 = wifi.ipv4
            _sessionState.value =
                WebManageSessionState(
                    enabled = preferencesStore.isWebManageEnabled(),
                    serverListening = listening,
                    onWifi = networkUtils.isOnWifi(),
                    wifiSsid = wifi.ssid,
                    lanIpv4 = ipv4,
                    localUrl =
                        if (listening && ipv4 != null) {
                            networkUtils.localBaseUrl(port)
                        } else {
                            null
                        },
                    loopbackUrl = if (listening) "http://127.0.0.1:$port" else null,
                    serverPort = port,
                    hasAccessPassword = preferencesStore.hasAccessPassword(),
                    pendingApprovalCount = transactionApprovalDao.getPendingCountOnce(),
                )
        }

        init {
            when (coordinator.currentMode()) {
                LocalLanServerMode.WEB_MANAGE ->
                    if (preferencesStore.hasAccessPassword()) {
                        startInfrastructure()
                    } else {
                        coordinator.clearWebManage()
                    }
                else -> {
                    stopInfrastructure()
                    if (preferencesStore.isWebManageEnabled()) {
                        coordinator.clearWebManage()
                    }
                }
            }
            scope.launch { publishState() }
        }
    }
