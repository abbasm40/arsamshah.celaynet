package com.myv2ray.client

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import libv2ray.Libv2ray
import libv2ray.V2RayPoint
import libv2ray.V2RayVPNServiceSupportsSet

class V2RayVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var v2rayPoint: V2RayPoint? = null

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

        v2rayPoint = Libv2ray.newV2RayPoint(object : V2RayVPNServiceSupportsSet {
            override fun shutdown(): Long = 0
            override fun prepare(): Long = 0
            override fun protect(fd: Long): Boolean = this@V2RayVpnService.protect(fd.toInt())
            override fun onEmitStatus(code: Long, msg: String?): Long = 0
            override fun setup(conf: String?): Long = 0
        }, false)

        v2rayPoint?.apply {
            configureFileContent = jsonConfig
            domainName = "proxy"
            runLoop(false)
        }
    }

    private fun stopVpn() {
        try {
            v2rayPoint?.stopLoop()
            vpnInterface?.close()
        } catch (_: Exception) {}
        stopSelf()
    }
}
