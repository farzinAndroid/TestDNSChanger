package com.farzin.testdnschanger.API

import android.app.ActivityManager
import android.content.Context
import com.farzin.testdnschanger.service.DNSVpnService
import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetAddress
import java.security.SecureRandom
import java.util.regex.Pattern
import kotlin.math.floor
import kotlin.math.pow

object API {
    const val BROADCAST_SERVICE_STATUS_CHANGE: String =
        "com.frostnerd.dnschanger.VPN_SERVICE_CHANGE"
    const val BROADCAST_SERVICE_STATE_REQUEST: String = "com.frostnerd.dnschanger.VPN_STATE_CHANGE"
    private val random = SecureRandom()
    private val ipv4Pattern: Pattern =
        Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
    private val domainPattern: Pattern =
        Pattern.compile("^(([a-zA-Z]{1})|([a-zA-Z]{1}[a-zA-Z]{1})|([a-zA-Z]{1}[0-9]{1})|([0-9]{1}[a-zA-Z]{1})|([a-zA-Z0-9][a-zA-Z0-9-_]{1,61}[a-zA-Z0-9]))\\.([a-zA-Z]{2,6}|[a-zA-Z0-9-]{2,30}\\.[a-zA-Z]{2,3})$")

    @JvmStatic
    fun randomLocalIPv6Address(): String {
        var prefix = randomIPv6LocalPrefix()
        for (i in 0..4) prefix += ":" + randomIPv6Block(16, false)
        return prefix
    }

    private fun randomIPv6LocalPrefix(): String {
        return "fd" + randomIPv6Block(8, true) + ":" + randomIPv6Block(
            16,
            false
        ) + ":" + randomIPv6Block(16, false)
    }

    private fun randomIPv6Block(bits: Int, leading_zeros: Boolean): String {
        var hex = java.lang.Long.toHexString(
            floor(Math.random() * 2.0.pow(bits.toDouble())).toLong()
        )
        if (!leading_zeros || hex.length == bits / 4);
        hex = "0000".substring(0, bits / 4 - hex.length) + hex
        return hex
    }

    fun checkVPNServiceRunning(c: Context): Boolean {
        val am = c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val name = DNSVpnService::class.java.name
        for (service in am.getRunningServices(Int.MAX_VALUE)) {
            if (name == service.service.className) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun randomString(bits: Int): String {
        return BigInteger(bits, random).toString(32)
    }

    fun isIP(s: String?): Boolean {
        return isIP(s, false)
    }

    fun isIP(s: String?, ipv6: Boolean): Boolean {
        return ipv4Pattern.matcher(s).matches() || (ipv6 && isIPv6(s))
    }

    private fun isIPv6(addr: String?): Boolean {
        try {
            val a = InetAddress.getByName(addr)
            return a is Inet6Address
        } catch (e: Exception) {
        }
        return false
    }

    fun isDomain(s: String?): Boolean {
        return domainPattern.matcher(s).matches()
    }

    fun isInteger(s: String): Boolean {
        try {
            s.toInt()
            return true
        } catch (e: Exception) {
        }
        return false
    }

    fun between(i: Int, min: Int, max: Int): Boolean {
        return i >= min && i <= max
    }
}
