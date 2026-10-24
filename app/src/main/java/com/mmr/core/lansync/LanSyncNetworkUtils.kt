package com.mmr.core.lansync

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
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
        fun isOnWifi(): Boolean {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return connectivity.allNetworks.any { network ->
                connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
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
            val candidates = linkedSetOf<String>()
            wifiLinkIpv4Candidates().forEach { candidates.add(it) }
            legacyWifiManagerIpv4()?.let { candidates.add(it) }
            wlanInterfaceIpv4Candidates().forEach { candidates.add(it) }
            return pickBestLanIpv4(candidates)
        }

        private fun wifiLinkIpv4Candidates(): List<String> {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val results = mutableListOf<String>()
            for (network in connectivity.allNetworks) {
                val capabilities = connectivity.getNetworkCapabilities(network) ?: continue
                if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
                val linkProperties = connectivity.getLinkProperties(network) ?: continue
                linkProperties.linkAddresses
                    .filter { isUsableLanIpv4(it) }
                    .mapNotNull { it.address.hostAddress }
                    .forEach { results.add(it) }
            }
            return results
        }

        @Suppress("DEPRECATION")
        private fun legacyWifiManagerIpv4(): String? {
            if (!isOnWifi()) return null
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
                if (!name.startsWith("wlan") && !name.startsWith("wifi") && !name.startsWith("ap")) continue
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

        private fun readWifiSsid(): String? {
            if (!isOnWifi()) return null
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val raw = wifi.connectionInfo?.ssid ?: return null
            return raw.trim().removeSurrounding("\"").takeIf { it.isNotEmpty() && it != "<unknown ssid>" }
        }
    }
