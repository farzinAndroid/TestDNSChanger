package com.farzin.testdnschanger

import android.annotation.SuppressLint
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.farzin.testdnschanger.API.API
import com.farzin.testdnschanger.service.DNSVpnService
import com.farzin.testdnschanger.ui.theme.TestDNSChangerTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vpnPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    startService(
                        Intent(this, DNSVpnService::class.java).putExtra(
                            "start_vpn",
                            true
                        )
                    )
                }
            }
        setContent {
            TestDNSChangerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var running by remember {
                        mutableStateOf(API.checkVPNServiceRunning(this@MainActivity))
                    }
                    Log.e("TAG",running.toString())
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,

                        ) {
                        Button(
                            onClick = {
                                if (API.checkVPNServiceRunning(this@MainActivity)) {
                                    stopDNSService()
                                } else {
                                    startDnsService()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start DNS")
                        }
                    }
                }
            }
        }
    }

    private fun startDnsService() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startService(Intent(this, DNSVpnService::class.java).putExtra("start_vpn", true))
        }
    }

    private fun stopDNSService() {
        startService(Intent(this, DNSVpnService::class.java).putExtra("stop_vpn", true))
        stopService(Intent(this, DNSVpnService::class.java))
    }
}