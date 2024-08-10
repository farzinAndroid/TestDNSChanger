package com.farzin.testdnschanger.service

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.farzin.testdnschanger.MainActivity
import com.farzin.testdnschanger.R

class BackgroundVpnConfigureActivity : AppCompatActivity() {
    private var startService = false
    private var dialog1: AlertDialog? = null
    private var dialog2: AlertDialog? = null
    private var requestTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportActionBar != null) supportActionBar!!.hide()
        val i = intent
        val conf = VpnService.prepare(this)
        startService = i != null && i.getBooleanExtra("startService", false)
        if (conf != null) {
            showDialog { dialog, which ->
                requestTime = System.currentTimeMillis()
                startActivityForResult(conf, REQUEST_CODE)
            }
        } else {
            if (startService) startService(
                Intent(
                    this,
                    DNSVpnService::class.java
                ).putExtra("start_vpn", true)
            )
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun showDialog(click: DialogInterface.OnClickListener) {
        dialog1 = AlertDialog.Builder(this)
            .setTitle("getString(R.string.information)" + " - " + getString(R.string.app_name))
            .setMessage("R.string.vpn_explain")
            .setCancelable(false).setPositiveButton("ok", click).show()
    }

    override fun onDestroy() {
        if (dialog1 != null) dialog1!!.cancel()
        if (dialog2 != null) dialog2!!.cancel()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (startService) startService(
                    Intent(
                        this,
                        DNSVpnService::class.java
                    ).putExtra("start_vpn", true)
                )
                setResult(RESULT_OK)
            } else if (resultCode == RESULT_CANCELED) {
                setResult(RESULT_CANCELED)
                if (System.currentTimeMillis() - requestTime <= 750) { //Most likely the system
                    dialog2 = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.app_name) + " - " + "getString(R.string.information)")
                        .setMessage("R.string.background_configure_error")
                        .setPositiveButton("R.string.open_app") { dialog, which ->
                            startActivity(
                                Intent(
                                    this@BackgroundVpnConfigureActivity,
                                    MainActivity::class.java
                                )
                            )
                            finish()
                        }
                        .setNegativeButton("R.string.cancel") { dialog, which ->
                            dialog.cancel()
                            finish()
                        }.show()
                } else finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val REQUEST_CODE = 112
        @JvmStatic
        fun startBackgroundConfigure(context: Context, startService: Boolean) {
            context.startActivity(
                Intent(context, BackgroundVpnConfigureActivity::class.java).putExtra(
                    "startService",
                    startService
                ).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            )
        }
    }
}
