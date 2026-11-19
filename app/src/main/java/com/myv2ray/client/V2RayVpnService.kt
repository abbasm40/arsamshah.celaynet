package com.myv2ray.client

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class V2RayVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var coreController: CoreController? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        val jsonConfig = intent?.getStringExtra("CONFIG_JSON") ?: return START_NOT_STICKY
        startVpn(jsonConfig)
        return START_STICKY
    }

    private fun startVpn(jsonConfig: String) {
        val builder = Builder()
            .setSession("MyV2RayClient")
            .addAddress("10.0.0.2", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDisallowedApplication(packageName)

        vpnInterface = builder.establish() ?: return

        coreController = Libv2ray.newCoreController(object : CoreCallbackHandler {
            override fun startup(): Long = 0
            override fun shutdown(): Long = 0
            override fun onEmitStatus(l: Long, s: String?): Long = 0
        })

        coreController?.startLoop(jsonConfig)
    }

    private fun stopVpn() {
        try {
            coreController?.stopLoop()
            vpnInterface?.close()
        } catch (_: Exception) {}
        stopSelf()
    }
}
