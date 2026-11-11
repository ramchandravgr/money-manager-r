package com.rrparallelstudio.miapp.core.lansync.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import com.rrparallelstudio.miapp.core.lansync.LanSyncNetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredLanPeer(
    val deviceId: String,
    val displayName: String,
    val host: String,
    val port: Int,
)

@Singleton
class LanSyncNsdHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val networkUtils: LanSyncNetworkUtils,
        private val wifiMulticastLock: LanSyncWifiMulticastLock,
    ) {
        private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        private var registrationListener: NsdManager.RegistrationListener? = null
        private var discoveryListener: NsdManager.DiscoveryListener? = null

        private val peers = ConcurrentHashMap<String, DiscoveredLanPeer>()
        private val serviceInfoCallbacks = CopyOnWriteArrayList<NsdManager.ServiceInfoCallback>()

        fun discoveredPeers(): List<DiscoveredLanPeer> = peers.values.sortedBy { it.displayName }

        fun findPeer(deviceId: String): DiscoveredLanPeer? = peers[deviceId]

        /**
         * Instrumented tests only — register a peer without NSD (e.g. localhost loopback helper).
         * No-op in non-debug builds.
         */
        fun registerTestPeer(peer: DiscoveredLanPeer) {
            if (!com.rrparallelstudio.miapp.BuildConfig.DEBUG) return
            peers[peer.deviceId] = peer
        }

        fun clearTestPeers() {
            if (!com.rrparallelstudio.miapp.BuildConfig.DEBUG) return
            peers.keys.toList().forEach { key ->
                val peer = peers[key]
                if (peer?.host == "127.0.0.1" || peer?.host == "localhost") {
                    peers.remove(key)
                }
            }
        }

        fun registerService(
            deviceId: String,
            displayName: String,
            port: Int,
        ) {
            unregisterService()
            val serviceInfo =
                NsdServiceInfo().apply {
                    serviceName = nsdServiceName(deviceId)
                    serviceType = LanSyncHttpConstants.SERVICE_TYPE
                    setPort(port)
                    setAttribute("deviceId", deviceId)
                    setAttribute("displayName", displayName.take(48))
                }
            val listener =
                object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(info: NsdServiceInfo) = Unit

                    override fun onRegistrationFailed(
                        serviceInfo: NsdServiceInfo,
                        errorCode: Int,
                    ) = Unit

                    override fun onServiceUnregistered(info: NsdServiceInfo) = Unit

                    override fun onUnregistrationFailed(
                        serviceInfo: NsdServiceInfo,
                        errorCode: Int,
                    ) = Unit
                }
            registrationListener = listener
            val executor = ContextCompat.getMainExecutor(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                nsdManager.registerService(
                    serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,
                    executor,
                    listener,
                )
            } else {
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
            }
        }

        fun unregisterService() {
            registrationListener?.let { listener ->
                runCatching { nsdManager.unregisterService(listener) }
            }
            registrationListener = null
        }

        fun startDiscovery(onUpdated: () -> Unit) {
            stopDiscovery()
            wifiMulticastLock.acquire()
            beginDiscovery(onUpdated)
        }

        /** Re-runs NSD browse without clearing known peers (keeps list while mDNS catches up). */
        fun refreshDiscovery(onUpdated: () -> Unit) {
            discoveryListener?.let { listener ->
                runCatching { nsdManager.stopServiceDiscovery(listener) }
            }
            discoveryListener = null
            wifiMulticastLock.acquire()
            beginDiscovery(onUpdated)
        }

        private fun beginDiscovery(onUpdated: () -> Unit) {
            val listener =
                object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(serviceType: String) = Unit

                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        if (serviceInfo.serviceType != LanSyncHttpConstants.SERVICE_TYPE) return
                        resolveDiscoveredService(serviceInfo, onUpdated)
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                        val lostName = serviceInfo.serviceName ?: return
                        peers.entries.removeIf { (deviceId, _) -> nsdServiceName(deviceId) == lostName }
                        onUpdated()
                    }

                    override fun onDiscoveryStopped(serviceType: String) = Unit

                    override fun onStartDiscoveryFailed(
                        serviceType: String,
                        errorCode: Int,
                    ) = Unit

                    override fun onStopDiscoveryFailed(
                        serviceType: String,
                        errorCode: Int,
                    ) = Unit
                }
            discoveryListener = listener
            val executor = ContextCompat.getMainExecutor(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                nsdManager.discoverServices(
                    LanSyncHttpConstants.SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    networkUtils.localPeerSyncNetworkRequest(),
                    executor,
                    listener,
                )
            } else {
                @Suppress("DEPRECATION")
                nsdManager.discoverServices(
                    LanSyncHttpConstants.SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    listener,
                )
            }
        }

        private fun nsdServiceName(deviceId: String): String = "MMR-${deviceId.take(12)}"

        private fun resolveDiscoveredService(
            serviceInfo: NsdServiceInfo,
            onUpdated: () -> Unit,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val callback =
                    object : NsdManager.ServiceInfoCallback {
                        override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                            unregisterServiceInfoCallback(this)
                        }

                        override fun onServiceUpdated(info: NsdServiceInfo) {
                            recordResolvedPeer(info, onUpdated)
                        }

                        override fun onServiceLost() {
                            unregisterServiceInfoCallback(this)
                        }

                        override fun onServiceInfoCallbackUnregistered() {
                            serviceInfoCallbacks.remove(this)
                        }
                    }
                serviceInfoCallbacks.add(callback)
                runCatching {
                    nsdManager.registerServiceInfoCallback(
                        serviceInfo,
                        ContextCompat.getMainExecutor(context),
                        callback,
                    )
                }.onFailure {
                    unregisterServiceInfoCallback(callback)
                }
                return
            }
            @Suppress("DEPRECATION")
            nsdManager.resolveService(
                serviceInfo,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(
                        serviceInfo: NsdServiceInfo,
                        errorCode: Int,
                    ) = Unit

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        recordResolvedPeer(resolved, onUpdated)
                    }
                },
            )
        }

        private fun recordResolvedPeer(
            resolved: NsdServiceInfo,
            onUpdated: () -> Unit,
        ) {
            val host = resolved.resolvedIpv4Host() ?: return
            val deviceId = resolved.attributes.readAttr("deviceId") ?: return
            val displayName = resolved.attributes.readAttr("displayName") ?: "MMR device"
            peers[deviceId] =
                DiscoveredLanPeer(
                    deviceId = deviceId,
                    displayName = displayName,
                    host = host,
                    port = resolved.port,
                )
            onUpdated()
        }

        private fun unregisterServiceInfoCallback(callback: NsdManager.ServiceInfoCallback) {
            serviceInfoCallbacks.remove(callback)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching { nsdManager.unregisterServiceInfoCallback(callback) }
            }
        }

        private fun clearServiceInfoCallbacks() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                serviceInfoCallbacks.forEach { callback ->
                    runCatching { nsdManager.unregisterServiceInfoCallback(callback) }
                }
            }
            serviceInfoCallbacks.clear()
        }

        fun stopDiscovery() {
            discoveryListener?.let { listener ->
                runCatching { nsdManager.stopServiceDiscovery(listener) }
            }
            discoveryListener = null
            clearServiceInfoCallbacks()
            peers.clear()
            wifiMulticastLock.release()
        }

        private fun NsdServiceInfo.resolvedIpv4Host(): String? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                hostAddresses
                    .mapNotNull { it.hostAddress }
                    .firstOrNull { !it.contains(':') }
                    ?.let { return it }
            }
            @Suppress("DEPRECATION")
            val legacy = host?.hostAddress
            if (legacy != null && !legacy.contains(':')) return legacy
            return null
        }

        private fun Map<String, ByteArray>.readAttr(key: String): String? =
            this[key]?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotEmpty() }
    }
