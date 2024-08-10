package com.farzin.testdnschanger.service;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.farzin.testdnschanger.API.API;
import com.farzin.testdnschanger.MainActivity;
import com.farzin.testdnschanger.R;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Random;


public class DNSVpnService extends VpnService {
    private boolean run = true, isRunning = false, stopped = false;
    private Thread thread;
    private ParcelFileDescriptor tunnelInterface;
    private Builder builder = new Builder();
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private final int NOTIFICATION_ID = 112;
    private Handler handler = new Handler();
    private String dns1 = "10.202.10.202", dns2 = "10.202.10.102", dns1_v6 = "2001:4860:4860::8888", dns2_v6 = "2001:4860:4860::8844";
    private BroadcastReceiver stateRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            broadcastServiceState(isRunning);
        }
    };

    private void broadcastServiceState(boolean vpnRunning) {
        sendBroadcast(new Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra("vpn_running", vpnRunning));
    }

    @SuppressLint("RestrictedApi")
    private void updateNotification() {
        initNotification();
        if (stopped || notificationBuilder == null || notificationManager == null) {
            if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
            return;
        }
        NotificationCompat.Action a1 = notificationBuilder.mActions.get(0);
        a1.actionIntent = PendingIntent.getService(this, 0, new Intent(this, DNSVpnService.class).setAction(new Random().nextInt(50) + "_action").putExtra(isRunning ? "stop_vpn" : "start_vpn", true), PendingIntent.FLAG_IMMUTABLE);
        notificationBuilder.mActions.get(1).actionIntent = PendingIntent.getService(this, 1, new Intent(this, DNSVpnService.class)
                .setAction(API.randomString(80) + "_action").putExtra("destroy", true), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().
                bigText("DNS 1: " + dns1 + "\nDNS 2: " + dns2 + "\nDNSV6 1: " + dns1_v6 + "\nDNSV6 2: " + dns2_v6));
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (notificationManager != null && notificationBuilder != null && !stopped)
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }
        }, 10);
    }

    private void initNotification() {
        if (notificationBuilder == null) {
            notificationBuilder = new NotificationCompat.Builder(this, "vpn_service_channel")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .addAction(new NotificationCompat.Action(R.drawable.ic_launcher_background, "getString(R.string.action_pause)", null))
                    .addAction(new NotificationCompat.Action(R.drawable.ic_launcher_background, "getString(R.string.action_stop)", null))
                    .setUsesChronometer(true);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel("vpn_service_channel", "VPN Service", NotificationManager.IMPORTANCE_LOW);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initNotification();
        registerReceiver(stateRequestReceiver, new IntentFilter(API.BROADCAST_SERVICE_STATE_REQUEST));
        Log.d("TAG", "service created");
    }

    @Override
    public void onDestroy() {
        stopped = true;
        run = false;
        if (thread != null) thread.interrupt();
        thread = null;
        notificationManager.cancel(NOTIFICATION_ID);
        notificationManager = null;
        notificationBuilder = null;
        unregisterReceiver(stateRequestReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TAG", "service onStart Command");
        if (intent != null) {
            if (intent.getBooleanExtra("stop_vpn", false)) {
                if (thread != null) {
                    run = false;
                    thread.interrupt();
                    thread = null;
                }
            } else if (intent.getBooleanExtra("start_vpn", false)) {
                if (thread != null) {
                    run = false;
                    thread.interrupt();
                }
                thread = new Thread(new Runnable() {
                    @SuppressLint("ForegroundServiceType")
                    @Override
                    public void run() {
                        DatagramChannel tunnel = null;
                        DatagramSocket tunnelSocket = null;
                        try {
                            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                                @Override
                                public void uncaughtException(Thread t, Throwable e) {
                                    System.out.println(e);
                                    stopSelf();
                                }
                            });
                            initNotification();
                            startForeground(NOTIFICATION_ID, notificationBuilder.build());
                            if (notificationBuilder != null) notificationBuilder.setWhen(System.currentTimeMillis());
                            tunnelInterface = builder.setSession("DnsChanger").addAddress("172.31.255.250", 30)
                                    .addAddress(API.randomLocalIPv6Address(), 48).addDnsServer(dns1).addDnsServer(dns2)
                                    .addDnsServer(dns1_v6).addDnsServer(dns2_v6).establish();
                            tunnel = DatagramChannel.open();
                            tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));
                            protect(tunnelSocket = tunnel.socket());
                            isRunning = true;
                            sendBroadcast(new Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra("vpn_running", true));
                            updateNotification();
                            try {
                                while (run) {
                                    Thread.sleep(250);
                                }
                            } catch (InterruptedException e2) {
                                e2.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            isRunning = false;
                            sendBroadcast(new Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra("vpn_running", false));
                            updateNotification();
                            if (tunnelInterface != null) try {
                                tunnelInterface.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (tunnel != null) try {
                                tunnel.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (tunnelSocket != null) try {
                                tunnelSocket.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                run = true;
                thread.start();
            } else if (intent.getBooleanExtra("destroy", false)) {
                stopped = true;
                if (thread != null) {
                    run = false;
                    thread.interrupt();
                }
                stopSelf();
            }
        }
        updateNotification();
        return START_STICKY;
    }
}

