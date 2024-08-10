package com.farzin.testdnschanger.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;


public class ConnectivityBackgroundService extends Service {
    private BroadcastReceiver connectivityChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = !intent.hasExtra("noConnectivity");
            int type = intent.getIntExtra("networkType", -1);
            if(!connected)return;
            if(type == ConnectivityManager.TYPE_WIFI){
                startService();
            }else if(type == ConnectivityManager.TYPE_MOBILE ){
                startService();
            }
        }
    };

    private void startService(){
        Intent i = VpnService.prepare(this);
        if (i == null) {
            Intent serviceIntent = new Intent(this, DNSVpnService.class);
            serviceIntent.putExtra("start_vpn", true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            BackgroundVpnConfigureActivity.startBackgroundConfigure(this, true);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(connectivityChange, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        ConnectivityManager cm =(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork == null)return;
        if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ){
            startService();
        }else if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE ){
            startService();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
