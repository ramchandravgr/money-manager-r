package com.rrparallelstudio.miapp.core.lansync

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

data class LanWifiInfo(
    val ssid: String?,
    val ipv4: String?,
)

@Singleton
class LanSyncNetworkUtils
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private data class TrackedLanNetwork(
            val network: Network,
            @Volatile var capabilities: NetworkCapabilities? = null,
            @Volatile var linkProperties: LinkProperties? = null,
        )

        private val trackedNetworks = ConcurrentHashMap<Network, TrackedLanNetwork>()
        private val preferredOutboundNetwork = AtomicReference<Network?>(null)
        private val localNetworkChangeListeners = CopyOnWriteArraySet<() -> Unit>()

        init {
            watchWifiNetworks()
        }

        fun addLocalNetworkChangeListener(listener: () -> Unit) {
            localNetworkChangeListeners.add(listener)
        }

        fun removeLocalNetworkChangeListener(listener: () -> Unit) {
            localNetworkChangeListeners.remove(listener)
        }

        private fun notifyLocalNetworkChanged() {
            localNetworkChangeListeners.forEach { listener ->
                runCatching { listener() }
            }
        }

        fun localPeerSyncNetworkRequest(): NetworkRequest = LanPeerSyncNetworkRequest.localWifi

        /** Wi‑Fi [Network] for outbound LAN sockets. The process default route may forbid local addresses. */
        fun wifiNetwork(): Network? = lanOutboundNetwork()

        /**
         * Network to bind for peer HTTP. Includes tethering/hotspot when the default route is mobile data.
         */
        fun lanOutboundNetwork(): Network? {
            preferredOutboundNetwork.get()?.let { return it }
            trackedWifiNetworks().firstOrNull()?.let { return it }
            val localHost = lanIpv4Address() ?: return null
            for (network in trackedWifiNetworks()) {
                val link = linkPropertiesFor(network) ?: continue
                val matches =
                    link.linkAddresses.any { addr ->
                        addr.address.hostAddress == localHost
                    }
                if (matches) return network
            }
            return null
        }

        private fun watchWifiNetworks() {
            val connectivity = connectivityManager()
            val request = localPeerSyncNetworkRequest()
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trackedNetworks.putIfAbsent(network, TrackedLanNetwork(network))
                        seedNetworkSnapshot(connectivity, network)
                        refreshPreferredOutboundNetwork()
                        notifyLocalNetworkChanged()
                    }

                    override fun onLost(network: Network) {
                        trackedNetworks.remove(network)
                        preferredOutboundNetwork.compareAndSet(network, null)
                        refreshPreferredOutboundNetwork()
                        notifyLocalNetworkChanged()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        updateTracked(network) { it.capabilities = capabilities }
                        refreshPreferredOutboundNetwork()
                        notifyLocalNetworkChanged()
                    }

                    override fun onLinkPropertiesChanged(
                        network: Network,
                        linkProperties: LinkProperties,
                    ) {
                        updateTracked(network) { it.linkProperties = linkProperties }
                        refreshPreferredOutboundNetwork()
                        notifyLocalNetworkChanged()
                    }
                }
            val register = {
                runCatching { connectivity.registerNetworkCallback(request, callback) }
            }
            if (Looper.myLooper() != null) {
                register()
            } else {
                Handler(Looper.getMainLooper()).post { register() }
            }
        }

        private fun seedNetworkSnapshot(
            connectivity: ConnectivityManager,
            network: Network,
        ) {
            updateTracked(network) { tracked ->
                tracked.capabilities = connectivity.getNetworkCapabilities(network)
                tracked.linkProperties = connectivity.getLinkProperties(network)
            }
        }

        private fun updateTracked(
            network: Network,
            block: (TrackedLanNetwork) -> Unit,
        ) {
            trackedNetworks.compute(network) { _, existing ->
                val tracked = existing ?: TrackedLanNetwork(network)
                block(tracked)
                tracked
            }
        }

        private fun refreshPreferredOutboundNetwork() {
            val best =
                trackedNetworks.values
                    .mapNotNull { tracked ->
                        val ipv4 =
                            tracked.linkProperties
                                ?.linkAddresses
                                ?.filter { isUsableLanIpv4(it) }
                                ?.mapNotNull { it.address.hostAddress }
                                ?.let { pickBestLanIpv4(it.toSet()) }
                        tracked.network to ipv4
                    }.sortedByDescending { (_, ipv4) -> if (ipv4 == null) 0 else 1 }
                    .firstOrNull { (_, ipv4) -> ipv4 != null && !looksLikeTypicalGateway(ipv4) }
                    ?.first
                    ?: trackedWifiNetworks().firstOrNull()
            preferredOutboundNetwork.set(best)
        }

        private fun trackedWifiNetworks(): List<Network> =
            trackedNetworks.values
                .filter { tracked ->
                    tracked.capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                }.map { it.network }

        private fun linkPropertiesFor(network: Network): LinkProperties? =
            trackedNetworks[network]?.linkProperties
                ?: connectivityManager().getLinkProperties(network)

        private fun connectivityManager(): ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        /** True when Wi‑Fi or phone hotspot gives this device a usable LAN address (sync / Web Manage). */
        fun isOnWifi(): Boolean = canUseLocalPeerSync()

        fun canUseLocalPeerSync(): Boolean {
            if (hasWifiTransportNetwork()) return true
            if (allWifiLinkIpv4Candidates().isNotEmpty()) return true
            return wlanInterfaceIpv4Candidates().isNotEmpty()
        }

        private fun hasWifiTransportNetwork(): Boolean {
            if (trackedWifiNetworks().isNotEmpty()) return true
            val connectivity = connectivityManager()
            val active = connectivity.activeNetwork ?: return false
            val caps = connectivity.getNetworkCapabilities(active) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }

        private fun allWifiLinkIpv4Candidates(): List<String> {
            val results = mutableListOf<String>()
            for (tracked in trackedNetworks.values) {
                if (tracked.capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
                    continue
                }
                tracked.linkProperties
                    ?.linkAddresses
                    ?.filter { isUsableLanIpv4(it) }
                    ?.mapNotNull { it.address.hostAddress }
                    ?.forEach { results.add(it) }
            }
            if (results.isNotEmpty()) return results
            return activeWifiLinkIpv4Candidates()
        }

        fun wifiInfo(): LanWifiInfo =
            LanWifiInfo(
                ssid = readWifiSsid(),
                ipv4 = lanIpv4Address(),
            )

        fun localBaseUrl(port: Int): String? {
            val host = lanIpv4Address() ?: return null
            return "http://$host:$port"
        }

        fun lanIpv4Address(): String? {
            reconcileTrackedNetworks()
            val liveOnInterface = wlanInterfaceIpv4Candidates().toSet()
            val candidates = linkedSetOf<String>()
            allWifiLinkIpv4Candidates().forEach { candidates.add(it) }
            wifiManagerConnectionInfoIpv4()?.let { candidates.add(it) }
            liveOnInterface.forEach { candidates.add(it) }
            val assignedNow =
                if (liveOnInterface.isEmpty()) {
                    candidates
                } else {
                    candidates.filter { it in liveOnInterface }.toCollection(linkedSetOf())
                }
            return pickBestLanIpv4(if (assignedNow.isEmpty()) candidates else assignedNow)
        }

        /** Drop remembered Wi‑Fi addresses that are no longer on an interface (stale home-LAN IP). */
        @Suppress("DEPRECATION")
        private fun reconcileTrackedNetworks() {
            val connectivity = connectivityManager()
            val live = connectivity.allNetworks.toSet()
            trackedNetworks.keys.toList().forEach { network ->
                if (network !in live) trackedNetworks.remove(network)
            }
            for (network in live) {
                val caps = connectivity.getNetworkCapabilities(network) ?: continue
                if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
                updateTracked(network) { tracked ->
                    tracked.capabilities = caps
                    tracked.linkProperties = connectivity.getLinkProperties(network)
                }
            }
            refreshPreferredOutboundNetwork()
        }

        /** Prefer the process default network when it is Wi‑Fi (more reliable on Android 12+). */
        private fun activeWifiLinkIpv4Candidates(): List<String> {
            val connectivity = connectivityManager()
            val network = connectivity.activeNetwork ?: return emptyList()
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return emptyList()
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return emptyList()
            val linkProperties = connectivity.getLinkProperties(network) ?: return emptyList()
            return linkProperties.linkAddresses
                .filter { isUsableLanIpv4(it) }
                .mapNotNull { it.address.hostAddress }
        }

        /** Fallback when [LinkProperties] does not expose an address (deprecated [WifiManager] API). */
        @Suppress("DEPRECATION")
        private fun wifiManagerConnectionInfoIpv4(): String? {
            if (!hasWifiTransportNetwork()) return null
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val packed = wifi.connectionInfo?.ipAddress ?: return null
            if (packed == 0) return null
            return String.format(
                Locale.US,
                "%d.%d.%d.%d",
                packed and 0xff,
                packed shr 8 and 0xff,
                packed shr 16 and 0xff,
                packed shr 24 and 0xff,
            )
        }

        private fun wlanInterfaceIpv4Candidates(): List<String> {
            val results = mutableListOf<String>()
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return results
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val name = networkInterface.name.lowercase()
                if (!isLanInterfaceName(name)) continue
                for (address in networkInterface.inetAddresses) {
                    if (address is Inet4Address && !address.isLoopbackAddress && !address.isLinkLocalAddress) {
                        address.hostAddress?.let { results.add(it) }
                    }
                }
            }
            return results
        }

        private fun pickBestLanIpv4(candidates: Set<String>): String? {
            if (candidates.isEmpty()) return null
            val nonGateway = candidates.filterNot { looksLikeTypicalGateway(it) }
            if (nonGateway.isNotEmpty()) return nonGateway.first()
            return candidates.first()
        }

        /** Many DHCP networks use x.x.x.1 for the router; phones are rarely assigned .1. */
        private fun looksLikeTypicalGateway(ip: String): Boolean = ip.substringAfterLast('.') == "1"

        private fun isUsableLanIpv4(linkAddress: LinkAddress): Boolean {
            val address = linkAddress.address
            return address is Inet4Address &&
                !address.isLoopbackAddress &&
                !address.isLinkLocalAddress
        }

        private fun trackedInterfaceNames(): Set<String> =
            trackedNetworks.values
                .mapNotNull { it.linkProperties?.interfaceName?.lowercase() }
                .toSet()

        private fun isLanInterfaceName(name: String): Boolean {
            if (trackedInterfaceNames().contains(name)) return true
            return name.startsWith("wlan") ||
                name.startsWith("wifi") ||
                name.startsWith("ap") ||
                name.startsWith("swlan") ||
                name.startsWith("p2p")
        }

        private fun readWifiSsid(): String? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                for (tracked in trackedNetworks.values) {
                    if (tracked.capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
                        continue
                    }
                    val transport = tracked.capabilities?.transportInfo
                    if (transport is WifiInfo) {
                        formatWifiSsid(transport.ssid)?.let { return it }
                    }
                }
            }
            @Suppress("DEPRECATION")
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            return formatWifiSsid(wifi.connectionInfo?.ssid)
        }

        private fun formatWifiSsid(raw: String?): String? =
            raw?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotEmpty() && it != "<unknown ssid>" }
    }
