package com.jarvis.android.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager

class SystemControlManager(private val ctx: Context) {

    fun callContactOrNumber(query: String): String {
        val number = lookupPhoneNumber(query) ?: query.filter { it.isDigit() || it == '+' }
        if (number.length < 3) return "Contact $query not found."
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
        return "Calling $query."
    }

    fun sendSms(phone: String, message: String) {
        val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        sms.sendTextMessage(phone, null, message, null, null)
    }

    fun launchPackage(packageName: String) {
        val launch = ctx.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(launch)
        }
    }

    fun setFlashlight(enabled: Boolean) {
        val cam = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cam.cameraIdList.firstOrNull() ?: return
        cam.setTorchMode(cameraId, enabled)
    }

    fun setMediaVolume(percent: Int) {
        val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = ((percent.coerceIn(0, 100) / 100.0) * max).toInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
    }

    fun openWifiPanel() {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Panel.ACTION_WIFI
        } else {
            Settings.ACTION_WIFI_SETTINGS
        }
        ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openBluetoothSettings() {
        ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openSettings() {
        ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun lookupPhoneNumber(nameQuery: String): String? {
        if (nameQuery.isBlank()) return null
        val cursor = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        ) ?: return null

        cursor.use {
            while (it.moveToNext()) {
                val name = it.getString(0) ?: ""
                val num = it.getString(1) ?: ""
                if (name.contains(nameQuery, ignoreCase = true)) {
                    return num.replace(Regex("[^0-9+]"), "")
                }
            }
        }
        return null
    }
}
