
package com.aura.ai.assistant

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.aura.ai.AuraCerebro
import com.aura.ai.actions.AuraActionEngine
import com.aura.ai.actions.AuraCommand
import com.aura.ai.actions.AuraCommandParser
import com.aura.ai.chat.AuraChatDatabase
import com.aura.ai.chat.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class AuraVoiceInteractionSession(
    context: android.content.Context
) : VoiceInteractionSession(context) {

    private val handler = Handler(Looper.getMainLooper())

    private var overlay: AuraVoiceOverlayView? = null
    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private var database: AuraChatDatabase? = null
    private var actionEngine: AuraActionEngine? = null
    private var cerebro: AuraCerebro? = null

    private var scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var ttsReady = false
    private var listening = false
    private var processing = false
    private var destroyed = false

    private val restartListening = Runnable {
        if (!destroyed && !processing) {
            startListening()
        }
    }

    override fun onShow(
        args: Bundle?,
        showFlags: Int
    ) {
        super.onShow(args, showFlags)

        destroyed = false

        initializeComponents()
        createOverlay()
        initializeSpeech()
        initializeTts()

        overlay?.setStatus("Te estoy escuchando")
        overlay?.setTranscript("Dime, ¿en qué te ayudo?")

        handler.postDelayed(
            restartListening,
            450L
        )
    }

    private fun initializeComponents() {
        if (database == null) {
            database = AuraChatDatabase(
                context.applicationContext
            )
        }

        if (actionEngine == null) {
            actionEngine = AuraActionEngine(
                context.applicationContext
            )
        }

        if (cerebro == null) {
            cerebro = AuraCerebro(
                context.applicationContext
            )
        }

        if (!scope.coroutineContext[kotlinx.coroutines.Job]!!.isActive) {
            scope = CoroutineScope(
                SupervisorJob() + Dispatchers.Main.immediate
            )
        }
    }

    private fun createOverlay() {
        val view = AuraVoiceOverlayView(
            context
        ) {
            hide()
        }

        overlay = view
        setContentView(view)

        window?.window?.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )
            setDimAmount(0.12f)
        }
    }

    private fun initializeSpeech() {
        recognizer?.destroy()
        recognizer = null

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            overlay?.setStatus(
                "Reconocimiento de voz no disponible"
            )
            return
        }

        recognizer = try {
            SpeechRecognizer.createSpeechRecognizer(
                context.applicationContext
            )
        } catch (_: Exception) {
            null
        }

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    if (!destroyed) {
                        listening = true
                        overlay?.setStatus("Te estoy escuchando")
                    }
                }

                override fun onBeginningOfSpeech() {
                    overlay?.setStatus("Te escucho...")
                }

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    listening = false
                    overlay?.setStatus("Entendiendo...")
                }

                override fun onError(error: Int) {
                    listening = false

                    if (destroyed || processing) return

                    if (
                        error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    ) {
                        overlay?.setStatus(
                            "Sigo aquí. Te escucho..."
                        )
                        handler.postDelayed(
                            restartListening,
                            550L
                        )
                    } else {
                        overlay?.setStatus(
                            "No pude escucharte. Reintentando..."
                        )
                        handler.postDelayed(
                            restartListening,
                            1200L
                        )
                    }
                }

                override fun onResults(results: Bundle?) {
                    listening = false

                    val text = results
                        ?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()

                    if (text.isBlank()) {
                        handler.postDelayed(
                            restartListening,
                            500L
                        )
                        return
                    }

                    overlay?.setTranscript(text)
                    processMessage(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        ?.firstOrNull()

                    if (!text.isNullOrBlank()) {
                        overlay?.setTranscript(text)
                    }
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) = Unit
            }
        )
    }

    private fun initializeTts() {
        textToSpeech?.shutdown()

        textToSpeech = TextToSpeech(
            context.applicationContext
        ) { status ->

            if (destroyed) return@TextToSpeech

            if (status == TextToSpeech.SUCCESS) {
                val engine = textToSpeech ?: return@TextToSpeech

                val language = engine.setLanguage(
                    Locale("es", "US")
                )

                ttsReady =
                    language != TextToSpeech.LANG_MISSING_DATA &&
                    language != TextToSpeech.LANG_NOT_SUPPORTED

                engine.setSpeechRate(0.96f)

                if (!ttsReady) {
                    overlay?.setStatus(
                        "Instala una voz en español"
                    )
                }
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                    handler.post {
                        overlay?.setStatus("Aura está hablando")
                        overlay?.setSpeaking(true)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    handler.post {
                        overlay?.setSpeaking(false)
                        overlay?.setStatus("Puedes seguir hablándome")
                        processing = false

                        handler.postDelayed(
                            restartListening,
                            450L
                        )
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handler.post {
                        overlay?.setSpeaking(false)
                        processing = false
                        overlay?.setStatus("Te escucho...")
                        handler.postDelayed(
                            restartListening,
                            500L
                        )
                    }
                }
            }
        )
    }

    private fun startListening() {
        if (
            destroyed ||
            processing ||
            listening ||
            recognizer == null
        ) return

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "es-US"
            )
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
        }

        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            overlay?.setStatus(
                "No pude iniciar el micrófono"
            )
            handler.postDelayed(
                restartListening,
                1000L
            )
        }
    }

    private fun processMessage(rawText: String) {
        if (processing || destroyed) return

        val text = rawText.trim()
        if (text.isBlank()) return

        processing = true
        overlay?.setStatus("Aura está pensando...")

        scope.launch {
            try {
                val db = database ?: return@launch

                db.insertMessage(
                    ChatMessage(
                        text = text,
                        isUser = true
                    )
                )

                val command = AuraCommandParser.parse(text)

                val response = if (
                    command == AuraCommand.CopyAppCheckToken
                ) {
                    "Esta acción requiere abrir Aura para copiar el token."
                } else if (command != null) {
                    actionEngine?.execute(command)
                        ?: "No pude ejecutar esa acción."
                } else {
                    cerebro?.responder(
                        pregunta = text,
                        historialLocal = db.getMessages()
                    ) ?: "No pude conectar con mi motor de inteligencia."
                }

                db.insertMessage(
                    ChatMessage(
                        text = response,
                        isUser = false
                    )
                )

                overlay?.setTranscript(response)
                speak(response)

            } catch (error: Exception) {
                android.util.Log.e(
                    "AuraVoiceSession",
                    "Error procesando solicitud",
                    error
                )

                speak(
                    "Ocurrió un problema al procesar tu solicitud."
                )
            }
        }
    }

    private fun speak(text: String) {
        if (destroyed) return

        if (!ttsReady || textToSpeech == null) {
            overlay?.setStatus(
                "Respuesta lista. Voz no disponible."
            )
            processing = false
            handler.postDelayed(
                restartListening,
                700L
            )
            return
        }

        val result = textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "aura-session-response"
        )

        if (result == TextToSpeech.ERROR) {
            processing = false
            overlay?.setStatus("No pude reproducir la respuesta")
            handler.postDelayed(
                restartListening,
                700L
            )
        }
    }

    override fun onHide() {
        super.onHide()

        handler.removeCallbacksAndMessages(null)

        listening = false
        processing = false

        try {
            recognizer?.cancel()
            textToSpeech?.stop()
        } catch (_: Exception) {
        }

        overlay?.stopOrbAnimation()
        overlay = null
    }

    override fun onDestroy() {
        destroyed = true

        handler.removeCallbacksAndMessages(null)

        try {
            recognizer?.cancel()
            recognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }

        recognizer = null
        textToSpeech = null

        database?.close()
        database = null

        scope.cancel()

        super.onDestroy()
    }
}
