package com.farzin.testdnschanger.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import com.farzin.testdnschanger.service.BackgroundVpnConfigureActivity.Companion.startBackgroundConfigure

class ConnectivityBackgroundService : Service() {
    private val connectivityChange: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connected = !intent.hasExtra("noConnectivity")
            val type = intent.getIntExtra("networkType", -1)
            if (!connected) return
            if (type == ConnectivityManager.TYPE_WIFI) {
                startService()
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                startService()
            }
        }
    }

    private fun startService() {
        val i = VpnService.prepare(this)
        if (i == null) {
            val serviceIntent = Intent(this, DNSVpnService::class.java)
            serviceIntent.putExtra("start_vpn", true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            startBackgroundConfigure(this, true)
        }
    }


    override fun onCreate() {
        super.onCreate()
        registerReceiver(connectivityChange, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm.activeNetworkInfo ?: return
        if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
            startService()
        } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
            startService()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
