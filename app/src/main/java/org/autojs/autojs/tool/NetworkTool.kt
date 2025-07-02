package org.autojs.autojs.tool

import android.content.Context
import android.net.wifi.WifiManager
import com.stardust.app.GlobalAppContext
import java.net.NetworkInterface
import java.net.SocketException

object NetworkTool {

    fun getLocalIp(): String {
        val context = GlobalAppContext.get()
        return getWifiAddress(context) ?: getHotspotIp() ?: ""
    }


    private fun getWifiAddress(context: Context): String? {
        val wifiMgr =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ip = wifiMgr?.connectionInfo?.ipAddress ?: return null
        return intToIpAddress(ip).takeIf { it != "0.0.0.0" }
    }


    private fun getHotspotIp(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.firstOrNull { it.name == "wlan1" || it.name == "ap0" }?.inetAddresses?.toList()
                ?.firstOrNull { address ->
                    address.hostAddress?.contains(".") == true && !address.isLoopbackAddress
                }?.hostAddress
        } catch (_: SocketException) {
            null
        }
    }


    private fun intToIpAddress(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}