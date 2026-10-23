package com.mmr.core.lansync.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
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
    ) {
        private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        private var registrationListener: NsdManager.RegistrationListener? = null
        private var discoveryListener: NsdManager.DiscoveryListener? = null
        private val resolveListeners = mutableListOf<NsdManager.ResolveListener>()

        private val peers = ConcurrentHashMap<String, DiscoveredLanPeer>()

        fun discoveredPeers(): List<DiscoveredLanPeer> = peers.values.sortedBy { it.displayName }

        fun findPeer(deviceId: String): DiscoveredLanPeer? = peers[deviceId]

        fun registerService(
            deviceId: String,
            displayName: String,
            port: Int,
        ) {
            unregisterService()
            val serviceInfo =
                NsdServiceInfo().apply {
                    serviceName = "MMR-${deviceId.take(12)}"
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
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        }

        fun unregisterService() {
            registrationListener?.let { listener ->
                runCatching { nsdManager.unregisterService(listener) }
            }
            registrationListener = null
        }

        fun startDiscovery(onUpdated: () -> Unit) {
            stopDiscovery()
            val listener =
                object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(serviceType: String) = Unit

                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        if (serviceInfo.serviceType != LanSyncHttpConstants.SERVICE_TYPE) return
                        nsdManager.resolveService(
                            serviceInfo,
                            object : NsdManager.ResolveListener {
                                override fun onResolveFailed(
                                    serviceInfo: NsdServiceInfo,
                                    errorCode: Int,
                                ) = Unit

                                override fun onServiceResolved(resolved: NsdServiceInfo) {
                                    val host = resolved.host?.hostAddress ?: return
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
                            },
                        )
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                        peers.entries.removeIf { (_, peer) -> peer.displayName == serviceInfo.serviceName }
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
            nsdManager.discoverServices(LanSyncHttpConstants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }

        fun stopDiscovery() {
            discoveryListener?.let { listener ->
                runCatching { nsdManager.stopServiceDiscovery(listener) }
            }
            discoveryListener = null
            peers.clear()
        }

        private fun Map<String, ByteArray>.readAttr(key: String): String? =
            this[key]?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotEmpty() }
    }
