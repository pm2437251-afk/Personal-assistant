package com.jarvis.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jarvis.android.bridge.LocalBridgeServer
import com.jarvis.android.system.SystemControlManager
import com.jarvis.android.voice.OfflineVoskService

class MainActivity : AppCompatActivity() {

    private var bridgeServer: LocalBridgeServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Jarvis Android Core"
            textSize = 24f
        }
        val status = TextView(this).apply {
            text = "Offline Vosk Engine + Local Bridge (127.0.0.1:8765)"
            setPadding(0, 16, 0, 32)
        }
        val startBtn = Button(this).apply {
            text = "Start Offline Wake-Word (Hey Jarvis)"
            setOnClickListener {
                val intent = Intent(this@MainActivity, OfflineVoskService::class.java)
                ContextCompat.startForegroundService(this@MainActivity, intent)
                status.text = "Listening offline for 'Hey Jarvis'..."
            }
        }
        val accBtn = Button(this).apply {
            text = "Enable Accessibility Control"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(startBtn)
        layout.addView(accBtn)
        setContentView(layout)

        requestJarvisPermissions()

        val sys = SystemControlManager(this)
        bridgeServer = LocalBridgeServer(8765, sys).also { it.start() }
    }

    private fun requestJarvisPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    override fun onDestroy() {
        bridgeServer?.stop()
        super.onDestroy()
    }
}
