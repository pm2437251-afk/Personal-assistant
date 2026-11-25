package com.jarvis.android.nlu

import com.jarvis.android.system.SystemControlManager

class OfflineCommandRouter(private val sys: SystemControlManager) {

    fun execute(raw: String): String {
        val text = raw.lowercase().trim()

        return when {
            text.contains("flashlight") || text.contains("torch") -> {
                val on = text.contains("on") || text.contains("jala") || text.contains("start")
                sys.setFlashlight(on)
                if (on) "Flashlight on." else "Flashlight off."
            }
            text.contains("wifi") || text.contains("wi-fi") -> {
                sys.openWifiPanel()
                "Opening Wi-Fi quick panel."
            }
            text.contains("bluetooth") -> {
                sys.openBluetoothSettings()
                "Opening Bluetooth settings."
            }
            text.contains("volume") -> {
                val level = Regex("\\d+").find(text)?.value?.toIntOrNull()
                    ?: if (text.contains("full") || text.contains("up")) 90 else 40
                sys.setMediaVolume(level)
                "Media volume set to $level percent."
            }
            text.contains("call") || text.contains("phone lagao") -> {
                val target = text.replace(Regex("call|phone|lagao|karo|ko|to"), "").trim()
                sys.callContactOrNumber(target)
            }
            text.contains("whatsapp") -> {
                val target = text.replace(Regex("whatsapp|karo|open|send"), "").trim()
                sys.launchPackage("com.whatsapp")
                "Opening WhatsApp for $target"
            }
            text.contains("youtube") -> { sys.launchPackage("com.google.android.youtube"); "Opening YouTube." }
            text.contains("spotify") -> { sys.launchPackage("com.spotify.music"); "Opening Spotify." }
            text.contains("settings") -> { sys.openSettings(); "Opening Android Settings." }
            else -> "Command heard offline: $raw"
        }
    }
}
