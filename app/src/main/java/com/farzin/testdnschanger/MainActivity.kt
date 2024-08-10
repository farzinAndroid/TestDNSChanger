package com.farzin.testdnschanger

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationPermission:PermissionState

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

            var notificationPermissionGranted by remember {
                mutableStateOf(false)
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                notificationPermission =
                    rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS){
                        if (it) {
                            notificationPermissionGranted = true
                        }
                    }
            }
            TestDNSChangerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var running by remember {
                        mutableStateOf(API.checkVPNServiceRunning(this@MainActivity))
                    }
                    Log.e("TAG", running.toString())
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,

                        ) {
                        Button(
                            onClick = {
                                if (notificationPermission.status.isGranted) {
                                    if (API.checkVPNServiceRunning(this@MainActivity)) {
                                        stopDNSService()
                                    } else {
                                        startDnsService()
                                    }
                                } else {
                                    notificationPermission.launchPermissionRequest()
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