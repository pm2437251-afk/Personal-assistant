package com.jarvis.android.bridge

import com.jarvis.android.system.SystemControlManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

class LocalBridgeServer(
    private val port: Int,
    private val sys: SystemControlManager
) {
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        running = true
        thread(isDaemon = true) {
            serverSocket = ServerSocket(port, 16, InetAddress.getByName("127.0.0.1"))
            while (running) {
                try {
                    val client = serverSocket?.accept() ?: break
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val requestLine = reader.readLine() ?: ""
                    var contentLength = 0
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        if (line.lowercase().startsWith("content-length:")) {
                            contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                        }
                    }

                    val bodyChars = CharArray(contentLength)
                    if (contentLength > 0) reader.read(bodyChars, 0, contentLength)
                    val json = if (contentLength > 0) JSONObject(String(bodyChars)) else JSONObject()

                    if (requestLine.startsWith("POST /execute")) {
                        handleCommand(json)
                    }

                    val responseBody = """{"ok":true}"""
                    val httpResp = "HTTP/1.1 200 OK\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Headers: Content-Type, X-Jarvis-Token\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: ${responseBody.length}\r\n\r\n" +
                        responseBody
                    client.getOutputStream().write(httpResp.toByteArray())
                    client.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun handleCommand(json: JSONObject) {
        when (json.optString("action")) {
            "make_call" -> sys.callContactOrNumber(json.optString("phone"))
            "send_sms" -> sys.sendSms(json.optString("phone"), json.optString("message"))
            "open_app" -> sys.launchPackage(json.optString("packageName"))
            "setting" -> {
                when (json.optString("key")) {
                    "flashlight" -> sys.setFlashlight(json.optBoolean("value"))
                    "wifi" -> sys.openWifiPanel()
                    "bluetooth" -> sys.openBluetoothSettings()
                    "volume" -> sys.setMediaVolume(json.optInt("value", 70))
                }
            }
        }
    }

    fun stop() {
        running = false
        serverSocket?.close()
    }
}
