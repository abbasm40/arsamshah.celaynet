package com.myv2ray.client

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object ConfigParser {
    fun parseVlessToXrayJson(vlessUrl: String): String {
        val uri = Uri.parse(vlessUrl.trim())
        require(uri.scheme == "vless") { "لینک باید با vless:// شروع شود" }

        val uuid = uri.userInfo ?: throw IllegalArgumentException("UUID یافت نشد")
        val host = uri.host ?: throw IllegalArgumentException("آدرس سرور یافت نشد")
        val port = if (uri.port != -1) uri.port else 443
        val security = uri.getQueryParameter("security") ?: "none"
        val network = uri.getQueryParameter("type") ?: "tcp"
        val sni = uri.getQueryParameter("sni") ?: host
        val fp = uri.getQueryParameter("fp") ?: "chrome"
        val flow = uri.getQueryParameter("flow") ?: ""

        val inbound = JSONObject().apply {
            put("tag", "socks")
            put("port", 10808)
            put("listen", "127.0.0.1")
            put("protocol", "socks")
            put("settings", JSONObject().apply { put("udp", true) })
        }

        val user = JSONObject().apply {
            put("id", uuid)
            put("encryption", "none")
            if (flow.isNotEmpty()) put("flow", flow)
        }

        val streamSettings = JSONObject().apply {
            put("network", network)
            put("security", security)
            if (security == "reality") {
                put("realitySettings", JSONObject().apply {
                    put("serverName", sni)
                    put("fingerprint", fp)
                    put("publicKey", uri.getQueryParameter("pbk") ?: "")
                    put("shortId", uri.getQueryParameter("sid") ?: "")
                    put("spiderX", uri.getQueryParameter("spx") ?: "/")
                })
            } else if (security == "tls") {
                put("tlsSettings", JSONObject().apply {
                    put("serverName", sni)
                    put("fingerprint", fp)
                })
            }
            if (network == "ws") {
                put("wsSettings", JSONObject().apply {
                    put("path", uri.getQueryParameter("path") ?: "/")
                    val wsHost = uri.getQueryParameter("host")
                    if (!wsHost.isNullOrEmpty()) {
                        put("headers", JSONObject().apply { put("Host", wsHost) })
                    }
                })
            }
        }

        val outbound = JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().put(JSONObject().apply {
                    put("address", host)
                    put("port", port)
                    put("users", JSONArray().put(user))
                }))
            })
            put("streamSettings", streamSettings)
        }

        return JSONObject().apply {
            put("inbounds", JSONArray().put(inbound))
            put("outbounds", JSONArray().put(outbound))
        }.toString(2)
    }
}
