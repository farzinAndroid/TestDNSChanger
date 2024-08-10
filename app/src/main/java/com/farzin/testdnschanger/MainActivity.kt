package com.farzin.testdnschanger

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.farzin.testdnschanger.API.API
import com.farzin.testdnschanger.service.DNSVpnService
import com.farzin.testdnschanger.ui.theme.TestDNSChangerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var broadCastReceiver: BroadcastReceiver
    private var vpnRunning = false

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

        broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("TAG", "Received broadcast: ${intent.action}")
//                vpnRunning = intent.getBooleanExtra("vpn_running", false)
            }
        }
        ContextCompat.registerReceiver(
            this,
            broadCastReceiver,
            IntentFilter(API.BROADCAST_SERVICE_STATE_REQUEST),
            ContextCompat.RECEIVER_EXPORTED
        )
        setContent {

            var notificationPermissionGranted by remember {
                mutableStateOf(false)
            }
            var notificationPermission =
                rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) {
                    if (it) {
                        notificationPermissionGranted = true
                    }
                }

            TestDNSChangerTheme {

                var shit by remember {
                    mutableStateOf(false)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,

                        ) {
                        val scope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                scope.launch {
                                    if (notificationPermission.status.isGranted) {
                                        if (API.checkVPNServiceRunning(this@MainActivity)) {
                                            stopDNSService()
                                            delay(500)
                                            shit = API.checkVPNServiceRunning(this@MainActivity)
                                        } else {
                                            startDnsService()
                                            shit = API.checkVPNServiceRunning(this@MainActivity)
                                        }
                                    } else {
                                        notificationPermission.launchPermissionRequest()
                                    }
                                }

                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start DNS $shit")
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadCastReceiver)
    }
}