package com.aura.ai.assistant

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.text.Normalizer

class AuraWakeWordService : Service(), RecognitionListener {

    companion object {
        private const val TAG = "AuraWakeWordService"

        private const val SAMPLE_RATE = 16000.0f

        private const val GRAMMAR =
            """["hey aura", "ey aura", "oye aura", "hola aura"]"""
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null

    private var triggered = false
    private var loading = false

    override fun onCreate() {
        super.onCreate()

        AuraWakeWordNotification.createChannel(this)

        startForeground(
            AuraWakeWordNotification.NOTIFICATION_ID,
            AuraWakeWordNotification.build(this)
        )

        loadModel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (!loading && model == null) {
            loadModel()
        }

        return START_STICKY
    }

    private fun loadModel() {
        if (loading || model != null) return

        loading = true

        AuraWakeWordModelInstaller.ensureModel(this) { directory ->

            loading = false

            if (directory == null) {
                Log.e(TAG, "No se pudo instalar el modelo español")
                stopSelf()
                return@ensureModel
            }

            try {
                model = Model(directory.absolutePath)

                startRecognition()

            } catch (error: Exception) {
                Log.e(TAG, "Error cargando Vosk", error)
                stopSelf()
            }
        }
    }

    private fun startRecognition() {
        if (triggered) return

        try {
            val currentModel = model ?: return

            recognizer = Recognizer(
                currentModel,
                SAMPLE_RATE,
                GRAMMAR
            )

            speechService = SpeechService(
                recognizer,
                SAMPLE_RATE
            )

            speechService?.startListening(this)

            Log.i(TAG, "Detector de Aura iniciado")

        } catch (error: Exception) {
            Log.e(TAG, "No se pudo iniciar el reconocimiento", error)
            stopSelf()
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        checkWakeWord(hypothesis)
    }

    override fun onResult(hypothesis: String?) {
        checkWakeWord(hypothesis)
    }

    override fun onFinalResult(hypothesis: String?) {
        checkWakeWord(hypothesis)
    }

    private fun checkWakeWord(hypothesis: String?) {
        if (triggered || hypothesis.isNullOrBlank()) return

        try {
            val json = JSONObject(hypothesis)

            val recognized = json.optString(
                "partial",
                json.optString("text", "")
            )

            val normalized = normalize(recognized)

            val phrases = listOf(
                "hey aura",
                "ey aura",
                "oye aura",
                "hola aura"
            )

            if (phrases.any { normalized.contains(it) }) {
                invokeAura()
            }

        } catch (error: Exception) {
            Log.w(TAG, "Resultado de voz no válido", error)
        }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(
            value.lowercase(),
            Normalizer.Form.NFD
        )
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun invokeAura() {
        if (triggered) return

        triggered = true

        Log.i(TAG, "Palabra de activación detectada")

        stopRecognition()

        AuraVoiceInteractionService.showAura()

        stopSelf()
    }

    private fun stopRecognition() {
        try {
            speechService?.stop()
            speechService?.shutdown()
        } catch (error: Exception) {
            Log.w(TAG, "Error deteniendo reconocimiento", error)
        }

        speechService = null

        try {
            recognizer?.close()
        } catch (_: Exception) {
        }

        recognizer = null
    }

    override fun onError(exception: Exception?) {
        Log.e(TAG, "Error de reconocimiento", exception)
    }

    override fun onTimeout() {
        if (!triggered) {
            Log.d(TAG, "Tiempo de reconocimiento agotado")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRecognition()

        try {
            model?.close()
        } catch (_: Exception) {
        }

        model = null

        super.onDestroy()
    }
}
