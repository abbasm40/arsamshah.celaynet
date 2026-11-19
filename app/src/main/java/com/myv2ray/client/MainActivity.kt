package com.myv2ray.client

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var isConnected = false
    private var pendingJsonConfig: String? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingJsonConfig != null) {
            launchVpnService(pendingJsonConfig!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val configInput = findViewById<EditText>(R.id.configInput)
        val btnConnect = findViewById<Button>(R.id.btnConnect)

        btnConnect.setOnClickListener {
            if (!isConnected) {
                try {
                    val json = ConfigParser.parseVlessToXrayJson(configInput.text.toString())
                    pendingJsonConfig = json
                    val intent = VpnService.prepare(this)
                    if (intent != null) {
                        vpnPermissionLauncher.launch(intent)
                    } else {
                        launchVpnService(json)
                    }
                    btnConnect.text = "قطع اتصال"
                    isConnected = true
                } catch (e: Exception) {
                    Toast.makeText(this, "خطا در کانفیگ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                val stopIntent = Intent(this, V2RayVpnService::class.java).apply { action = "STOP" }
                startService(stopIntent)
                btnConnect.text = "اتصال"
                isConnected = false
            }
        }
    }

    private fun launchVpnService(json: String) {
        val intent = Intent(this, V2RayVpnService::class.java).apply {
            putExtra("CONFIG_JSON", json)
        }
        startService(intent)
    }
}
