package com.farzin.testdnschanger.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.farzin.testdnschanger.API.API
import com.farzin.testdnschanger.API.API.randomLocalIPv6Address
import com.farzin.testdnschanger.API.API.randomString
import com.farzin.testdnschanger.MainActivity
import com.farzin.testdnschanger.R
import java.io.IOException
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import java.util.Random

class DNSVpnService : VpnService() {
    private var run = true
    private var isRunning = false
    private var stopped = false
    private var thread: Thread? = null
    private var tunnelInterface: ParcelFileDescriptor? = null
    private val builder: Builder = Builder()
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private val NOTIFICATION_ID = 112
    private val handler = Handler()
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
        sendBroadcast(
            Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra(
                "vpn_running",
                vpnRunning
            )
        )
    }

    @SuppressLint("RestrictedApi")
    private fun updateNotification() {
        initNotification()
        if (stopped || notificationBuilder == null || notificationManager == null) {
            if (notificationManager != null) notificationManager!!.cancel(NOTIFICATION_ID)
            return
        }
        val a1 = notificationBuilder!!.mActions[0]
        a1.actionIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, DNSVpnService::class.java).setAction(
                Random().nextInt(50).toString() + "_action"
            ).putExtra(if (isRunning) "stop_vpn" else "start_vpn", true),
            PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder!!.mActions[1].actionIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DNSVpnService::class.java)
                .setAction(randomString(80) + "_action").putExtra("destroy", true),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder!!.setStyle(
            NotificationCompat.BigTextStyle().bigText
                ("DNS 1: $dns1\nDNS 2: $dns2\nDNSV6 1: $dns1_v6\nDNSV6 2: $dns2_v6")
        )
        handler.postDelayed({
            if (notificationManager != null && notificationBuilder != null && !stopped) notificationManager!!.notify(
                NOTIFICATION_ID,
                notificationBuilder!!.build()
            )
        }, 10)
    }

    private fun initNotification() {
        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(this, "vpn_service_channel")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_launcher_background,
                        "getString(R.string.action_pause)",
                        null
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_launcher_background,
                        "getString(R.string.action_stop)",
                        null
                    )
                )
                .setUsesChronometer(true)
            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            var channel: NotificationChannel? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = NotificationChannel(
                    "vpn_service_channel",
                    "VPN Service",
                    NotificationManager.IMPORTANCE_LOW
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager!!.createNotificationChannel(channel!!)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initNotification()
        registerReceiver(stateRequestReceiver, IntentFilter(API.BROADCAST_SERVICE_STATE_REQUEST))
        Log.d("TAG", "service created")
    }

    override fun onDestroy() {
        stopped = true
        run = false
        if (thread != null) thread!!.interrupt()
        thread = null
        notificationManager!!.cancel(NOTIFICATION_ID)
        notificationManager = null
        notificationBuilder = null
        unregisterReceiver(stateRequestReceiver)
        super.onDestroy()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("TAG", "service onStart Command")
        if (intent != null) {
            if (intent.getBooleanExtra("stop_vpn", false)) {
                if (thread != null) {
                    run = false
                    thread!!.interrupt()
                    thread = null
                }
            } else if (intent.getBooleanExtra("start_vpn", false)) {
                if (thread != null) {
                    run = false
                    thread!!.interrupt()
                }
                thread = Thread {
                    var tunnel: DatagramChannel? = null
                    var tunnelSocket: DatagramSocket? = null
                    try {
                        Thread.setDefaultUncaughtExceptionHandler { t, e ->
                            println(e)
                            stopSelf()
                        }
                        initNotification()
                        startForeground(NOTIFICATION_ID, notificationBuilder!!.build())
                        if (notificationBuilder != null) notificationBuilder!!.setWhen(System.currentTimeMillis())
                        tunnelInterface =
                            builder.setSession("DnsChanger").addAddress("172.31.255.250", 30)
                                .addAddress(randomLocalIPv6Address(), 48).addDnsServer(dns1)
                                .addDnsServer(dns2)
                                .addDnsServer(dns1_v6).addDnsServer(dns2_v6).establish()
                        tunnel = DatagramChannel.open()
                        tunnel.connect(InetSocketAddress("127.0.0.1", 8087))
                        protect(tunnel.socket().also { tunnelSocket = it })
                        isRunning = true
                        sendBroadcast(
                            Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra(
                                "vpn_running",
                                true
                            )
                        )
                        updateNotification()
                        try {
                            while (run) {
                                Thread.sleep(250)
                            }
                        } catch (e2: InterruptedException) {
                            e2.printStackTrace()
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
                        updateNotification()
                        if (tunnelInterface != null) try {
                            tunnelInterface!!.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        if (tunnel != null) try {
                            tunnel.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (tunnelSocket != null) try {
                            tunnelSocket!!.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                run = true
                thread!!.start()
            } else if (intent.getBooleanExtra("destroy", false)) {
                stopped = true
                if (thread != null) {
                    run = false
                    thread!!.interrupt()
                }
                stopSelf()
            }
        }
        updateNotification()
        return START_STICKY
    }
}