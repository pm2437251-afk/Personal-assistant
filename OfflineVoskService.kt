package com.jarvis.android.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.jarvis.android.nlu.OfflineCommandRouter
import com.jarvis.android.system.SystemControlManager
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.util.Locale

class OfflineVoskService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    private var speechService: SpeechService? = null
    private lateinit var systemControl: SystemControlManager
    private lateinit var router: OfflineCommandRouter
    private var tts: TextToSpeech? = null
    private var awakeUntilMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        systemControl = SystemControlManager(this)
        router = OfflineCommandRouter(systemControl)
        tts = TextToSpeech(this, this)
        startForegroundNotification()
        initOfflineModel()
    }

    private fun startForegroundNotification() {
        val channelId = "jarvis_offline_voice"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Jarvis Offline Voice", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Jarvis Active (Offline)")
            .setContentText("Say 'Hey Jarvis' followed by any command")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(1, notif)
    }

    private fun initOfflineModel() {
        StorageService.unpack(this, "model-en-in", "model",
            { model: Model ->
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f).apply {
                    startListening(this@OfflineVoskService)
                }
            },
            { /* Model unpack failed */ }
        )
    }

    override fun onResult(hypothesis: String?) {
        val text = JSONObject(hypothesis ?: "{}").optString("text", "").trim()
        if (text.isEmpty()) return

        val now = System.currentTimeMillis()
        if (text.contains("jarvis", ignoreCase = true)) {
            awakeUntilMs = now + 8000L
            val stripped = text.replace(Regex("(?i)(hey|ok|hi)?\s*jarvis"), "").trim()
            if (stripped.isEmpty()) {
                speak("Yes, I am listening.")
                return
            }
            val reply = router.execute(stripped)
            speak(reply)
            return
        }

        if (now < awakeUntilMs) {
            awakeUntilMs = 0L
            val reply = router.execute(text)
            speak(reply)
        }
    }

    private fun speak(msg: String) {
        tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utt")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "IN")
        }
    }

    override fun onPartialResult(hypothesis: String?) {}
    override fun onFinalResult(hypothesis: String?) { onResult(hypothesis) }
    override fun onError(exception: Exception?) {}
    override fun onTimeout() {}

    override fun onDestroy() {
        speechService?.stop()
        speechService?.shutdown()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
