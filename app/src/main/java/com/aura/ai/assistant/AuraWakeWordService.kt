package com.aura.ai.assistant

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.text.Normalizer

class AuraWakeWordService : Service(), RecognitionListener {

    companion object {
        private const val TAG = "AuraWakeWordService"
        private const val SAMPLE_RATE = 16000.0f

        private const val GRAMMAR =
            """["hey aura", "ey aura", "oye aura", "hola aura"]"""
    }

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null

    private var triggered = false
    private var loading = false
    private var destroyed = false

    private var retryAttempt = 0
    private var modelRetryAttempt = 0

    private var restartPending = false
    private var modelRetryPending = false

    private val restartRunnable = Runnable {
        restartPending = false

        if (!triggered && !destroyed) {
            stopRecognition()
            startRecognition()
        }
    }

    private val modelRetryRunnable = Runnable {
        modelRetryPending = false

        if (!destroyed && model == null) {
            loadModel()
        }
    }

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
        if (!loading && model == null && !destroyed) {
            loadModel()
        }

        return START_STICKY
    }

    private fun loadModel() {
        if (loading || model != null || destroyed) return

        loading = true

        Log.i(TAG, "Comprobando modelo español de Vosk")

        AuraWakeWordModelInstaller.ensureModel(this) { directory ->

            loading = false

            if (destroyed) return@ensureModel

            if (directory == null) {
                Log.e(
                    TAG,
                    "No se pudo instalar el modelo. Se programará otro intento."
                )

                scheduleModelRetry()
                return@ensureModel
            }

            try {
                model = Model(directory.absolutePath)

                modelRetryAttempt = 0

                Log.i(
                    TAG,
                    "Modelo Vosk cargado correctamente"
                )

                startRecognition()

            } catch (error: Exception) {
                Log.e(
                    TAG,
                    "Error cargando el modelo Vosk",
                    error
                )

                try {
                    model?.close()
                } catch (_: Exception) {
                }

                model = null
                scheduleModelRetry()
            }
        }
    }

    private fun scheduleModelRetry() {
        if (destroyed || modelRetryPending) return

        modelRetryPending = true

        modelRetryAttempt =
            (modelRetryAttempt + 1).coerceAtMost(6)

        val delay = (
            5000L * (1 shl (modelRetryAttempt - 1))
        ).coerceAtMost(300000L)

        Log.w(
            TAG,
            "Nuevo intento de instalación en ${delay}ms"
        )

        mainHandler.postDelayed(
            modelRetryRunnable,
            delay
        )
    }

    private fun startRecognition() {
        if (triggered || destroyed) return

        val currentModel = model ?: return

        try {
            val newRecognizer = try {
                Recognizer(
                    currentModel,
                    SAMPLE_RATE,
                    GRAMMAR
                )
            } catch (grammarError: Exception) {
                Log.w(
                    TAG,
                    "No se pudo aplicar la gramática restringida. Usando reconocimiento general.",
                    grammarError
                )

                Recognizer(
                    currentModel,
                    SAMPLE_RATE
                )
            }

            recognizer = newRecognizer

            val newSpeechService = SpeechService(
                newRecognizer,
                SAMPLE_RATE
            )

            speechService = newSpeechService

            val started =
                newSpeechService.startListening(this)

            if (!started) {
                throw IllegalStateException(
                    "Vosk no pudo iniciar el reconocimiento."
                )
            }

            retryAttempt = 0

            Log.i(
                TAG,
                "Detector de Aura iniciado"
            )

        } catch (error: Exception) {
            Log.e(
                TAG,
                "No se pudo iniciar el reconocimiento",
                error
            )

            stopRecognition()
            scheduleRecognitionRestart()
        }
    }

    private fun scheduleRecognitionRestart() {
        if (triggered || destroyed || restartPending) return

        restartPending = true

        retryAttempt =
            (retryAttempt + 1).coerceAtMost(5)

        val delay = (
            1000L * (1 shl retryAttempt)
        ).coerceAtMost(30000L)

        Log.w(
            TAG,
            "Reiniciando reconocimiento en ${delay}ms"
        )

        mainHandler.postDelayed(
            restartRunnable,
            delay
        )
    }

    override fun onPartialResult(hypothesis: String?) {
        checkWakeWord(hypothesis)
    }

    override fun onResult(hypothesis: String?) {
        checkWakeWord(hypothesis)
    }

    override fun onFinalResult(hypothesis: String?) {
        checkWakeWord(hypothesis)

        if (!triggered && !destroyed) {
            scheduleRecognitionRestart()
        }
    }

    private fun checkWakeWord(hypothesis: String?) {
        if (triggered || hypothesis.isNullOrBlank()) return

        try {
            val json = JSONObject(hypothesis)

            val partial = json.optString("partial")
            val finalText = json.optString("text")

            val recognized = if (partial.isNotBlank()) {
                partial
            } else {
                finalText
            }

            if (recognized.isBlank()) return

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
            Log.w(
                TAG,
                "Resultado de voz no válido",
                error
            )
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
        if (triggered || destroyed) return

        triggered = true

        Log.i(
            TAG,
            "Frase de activación detectada"
        )

        mainHandler.removeCallbacks(restartRunnable)
        restartPending = false

        stopRecognition()

        val shown =
            AuraVoiceInteractionService.showAura()

        if (shown) {
            stopSelf()
        } else {
            Log.e(
                TAG,
                "No se pudo mostrar Aura. Se reintentará la escucha."
            )

            triggered = false
            scheduleRecognitionRestart()
        }
    }

    private fun stopRecognition() {
        val currentSpeechService = speechService
        speechService = null

        try {
            currentSpeechService?.cancel()
        } catch (error: Exception) {
            Log.w(
                TAG,
                "Error cancelando reconocimiento",
                error
            )
        }

        try {
            currentSpeechService?.shutdown()
        } catch (error: Exception) {
            Log.w(
                TAG,
                "Error liberando servicio de voz",
                error
            )
        }

        val currentRecognizer = recognizer
        recognizer = null

        try {
            currentRecognizer?.close()
        } catch (error: Exception) {
            Log.w(
                TAG,
                "Error cerrando reconocedor",
                error
            )
        }
    }

    override fun onError(exception: Exception?) {
        Log.e(
            TAG,
            "Error de reconocimiento",
            exception
        )

        if (!triggered && !destroyed) {
            scheduleRecognitionRestart()
        }
    }

    override fun onTimeout() {
        if (!triggered && !destroyed) {
            Log.d(
                TAG,
                "Tiempo de reconocimiento agotado"
            )

            scheduleRecognitionRestart()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        destroyed = true

        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.removeCallbacks(modelRetryRunnable)

        restartPending = false
        modelRetryPending = false

        stopRecognition()

        try {
            model?.close()
        } catch (error: Exception) {
            Log.w(
                TAG,
                "Error cerrando el modelo",
                error
            )
        }

        model = null

        super.onDestroy()
    }
}
