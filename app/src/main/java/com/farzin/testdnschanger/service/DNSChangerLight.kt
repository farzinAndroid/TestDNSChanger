package com.farzin.testdnschanger.service

import android.app.Application

class DNSChangerLight : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            println(e)
            //                ErrorDialogActivity.show(DNSChangerLight.this, e);
        }
    }
}
