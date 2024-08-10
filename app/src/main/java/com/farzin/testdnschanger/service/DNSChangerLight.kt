package com.farzin.testdnschanger.service;

import android.app.Application;


public class DNSChangerLight extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println(e);
//                ErrorDialogActivity.show(DNSChangerLight.this, e);
            }
        });
    }
}
