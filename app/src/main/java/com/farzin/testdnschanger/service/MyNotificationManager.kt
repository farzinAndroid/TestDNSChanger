package com.farzin.testdnschanger.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import com.farzin.testdnschanger.API.API.randomString
import com.farzin.testdnschanger.MainActivity
import com.farzin.testdnschanger.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyNotificationManager(private val context: Context) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "vpn_service_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "VPN_Service"
        private const val NOTIFICATION_ID = 112
    }

    private var notificationBuilder: NotificationCompat.Builder? = null
    private val notificationManager: NotificationManager? by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val dns1 = "10.202.10.202"
    private val dns2 = "10.202.10.102"
    private val dns1_v6 = "2001:4860:4860::8888"
    private val dns2_v6 = "2001:4860:4860::8844"

    suspend fun updateNotification(stopped: Boolean) {
        initNotification()
        if (stopped || notificationBuilder == null || notificationManager == null) {
            if (notificationManager != null) notificationManager!!.cancel(NOTIFICATION_ID)
            return
        }

        notificationBuilder!!.clearActions()

//        val actionBuilder = NotificationCompat.Action.Builder(
//            R.drawable.ic_launcher_background,
//            "pause",
//            PendingIntent.getService(
//                this,
//                0,
//                Intent(this, DNSVpnService::class.java).setAction(
//                    Random().nextInt(50).toString() + "_action"
//                ).putExtra(if (isRunning) "stop_vpn" else "start_vpn", true),
//                PendingIntent.FLAG_IMMUTABLE
//            )
//        )
//        val action1 = actionBuilder.build()

        val actionBuilder2 = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_background,
            "stop",
            PendingIntent.getService(
                context,
                1,
                Intent(context, DNSVpnService::class.java)
                    .setAction(randomString(80) + "_action").putExtra("destroy", true),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        val action2 = actionBuilder2.build()

        // Only add the two desired actions
//        notificationBuilder!!.addAction(action1)
        notificationBuilder!!.addAction(action2)

        notificationBuilder!!.setStyle(
            NotificationCompat.BigTextStyle().bigText(
                "DNS 1: $dns1\nDNS 2: $dns2\nDNSV6 1: $dns1_v6\nDNSV6 2: $dns2_v6"
            )
        )

        withContext(Dispatchers.Main) {
            if (notificationManager != null && notificationBuilder != null && !stopped) {
                notificationManager!!.notify(NOTIFICATION_ID, notificationBuilder!!.build())
            }
        }
    }


    fun initNotification() {
        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(context, "vpn_service_channel")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(false)
                .setOngoing(true)
//                .addAction(
//                    NotificationCompat.Action(
//                        R.drawable.ic_launcher_background,
//                        "pause",
//                        null
//                    )
//                )
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_launcher_background,
                        "stop",
                        null
                    )
                )
                .setUsesChronometer(true)

            // Create the notification channel for Android 8.0 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_NONE
                )
                channel.description = "Notification channel for DNS service"
                notificationManager?.createNotificationChannel(channel)


            }
        }
    }

    fun cancelNotification() {
        notificationManager?.cancel(NOTIFICATION_ID)
        notificationBuilder = null
    }


    fun startNotificationForeground(context: Service) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder?.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        } else {
            notificationBuilder?.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFAULT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.startForeground(
                NOTIFICATION_ID,
                notificationBuilder!!.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            context.startForeground(NOTIFICATION_ID, notificationBuilder!!.build())
        }
        if (notificationBuilder != null) notificationBuilder?.setWhen(System.currentTimeMillis())
    }
}
