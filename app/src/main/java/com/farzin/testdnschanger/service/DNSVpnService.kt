package com.farzin.testdnschanger.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import com.farzin.testdnschanger.API.API
import com.farzin.testdnschanger.API.API.randomLocalIPv6Address
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

class DNSVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob())

    private var run = true
    private var isRunning = false
    private var stopped = false
    private var tunnelInterface: ParcelFileDescriptor? = null
    private val builder: Builder = Builder()
    private val myNotificationManager = MyNotificationManager(this)
    private val dns1 = "10.202.10.202"
    private val dns2 = "10.202.10.102"
    private val dns1_v6 = "2001:4860:4860::8888"
    private val dns2_v6 = "2001:4860:4860::8844"
    private val stateRequestReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            broadcastServiceState(isRunning)
        }
    }

    private fun broadcastServiceState(vpnRunning: Boolean) {
        Log.d("TAG", "Sending broadcast: $vpnRunning")
        sendBroadcast(
            Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra(
                "vpn_running",
                vpnRunning
            )
        )
    }




    override fun onCreate() {
        super.onCreate()
        myNotificationManager.initNotification()
        ContextCompat.registerReceiver(
            this,
            stateRequestReceiver,
            IntentFilter(API.BROADCAST_SERVICE_STATE_REQUEST),
            ContextCompat.RECEIVER_EXPORTED
        )
        Log.d("TAG", "service created")
    }

    override fun onDestroy() {
        scope.launch {
            stopped = true
            run = false
        }
        myNotificationManager.cancelNotification()
        unregisterReceiver(stateRequestReceiver)
        super.onDestroy()
    }

    @Override
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("TAG", "service onStart Command")
        if (intent != null) {
            when {
                intent.getBooleanExtra("stop_vpn", false) -> {
                    scope.launch {
                        stopVpn()
                    }
                }
                intent.getBooleanExtra("start_vpn", false) -> {
                    scope.launch {
                        startVpn()
                    }
                }
                intent.getBooleanExtra("destroy", false) -> {
                    stopped = true
                    stopVpn()
                    stopSelf()
                }
            }
        }
        scope.launch {
            myNotificationManager.updateNotification(stopped)
        }
        return START_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    private suspend fun startVpn() {
        val tunnel = withContext(Dispatchers.IO) {
            DatagramChannel.open()
        }
        myNotificationManager.initNotification()
        myNotificationManager.startNotificationForeground(this)

        try {
            tunnelInterface = builder.setSession("DnsChanger")
                .addAddress("172.31.255.250", 30)
                .addAddress(randomLocalIPv6Address(), 48)
                .addDnsServer(dns1)
                .addDnsServer(dns2)
//                .addDnsServer(dns1_v6)
//                .addDnsServer(dns2_v6)
                .establish()


            withContext(Dispatchers.IO) {
                tunnel.connect(InetSocketAddress("127.0.0.1", 8087))
            }
            protect(tunnel.socket())

            isRunning = true
            sendBroadcast(
                Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra(
                    "vpn_running",
                    true
                )
            )
            myNotificationManager.updateNotification(stopped)

            while (run) {
                delay(250) // Suspend for 250 milliseconds
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRunning = false
            sendBroadcast(
                Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra(
                    "vpn_running",
                    false
                )
            )
            myNotificationManager.updateNotification(stopped)
            tunnelInterface?.close()
            withContext(Dispatchers.IO) {
                tunnel?.close()
            }
        }
    }

    private fun stopVpn() {
        run = false
    }
}