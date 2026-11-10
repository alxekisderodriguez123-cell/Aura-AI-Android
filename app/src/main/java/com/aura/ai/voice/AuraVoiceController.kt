package com.aura.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AuraVoiceController(
    context: Context,
    private val onStateChanged: (AuraVoiceState) -> Unit,
    private val onTranscriptChanged: (String) -> Unit,
    private val onMessageChanged: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit
) {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private var isTtsReady = false
    private var isReleased = false
    private var requestActive = false
    private var continuousMode = false

    private val restartListening = Runnable {
        if (continuousMode && !isReleased && !requestActive) {
            startListeningInternal()
        }
    }

    init {
        initializeTextToSpeech()
        initializeSpeechRecognizer()
    }

    private fun initializeTextToSpeech() {

        textToSpeech = TextToSpeech(appContext) { status ->

            if (status == TextToSpeech.SUCCESS && !isReleased) {

                val engine = textToSpeech ?: return@TextToSpeech

                val result = engine.setLanguage(
                    Locale("es", "US")
                )

                isTtsReady =
                    result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

                if (isTtsReady) {
                    engine.setSpeechRate(0.96f)
                    onMessageChanged("Voz de Aura preparada")
                } else {
                    onMessageChanged(
                        "Instala una voz en español en los ajustes de Android."
                    )
                }

            } else if (!isReleased) {
                onMessageChanged(
                    "No se pudo inicializar la síntesis de voz."
                )
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                    if (!isReleased) {
                        onStateChanged(AuraVoiceState.SPEAKING)
                        onMessageChanged("Aura está hablando...")
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (!isReleased) {
                        if (continuousMode) {
                            onMessageChanged(
                                "Puedes seguir hablándome."
                            )
                            handler.postDelayed(
                                restartListening,
                                450L
                            )
                        } else {
                            onStateChanged(AuraVoiceState.IDLE)
                            onMessageChanged("Aura está preparada")
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (!isReleased) {
                        onStateChanged(AuraVoiceState.IDLE)
                        onMessageChanged(
                            "No se pudo reproducir la respuesta de voz."
                        )
                    }
                }
            }
        )
    }

    private fun initializeSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onMessageChanged(
                "Android no tiene un servicio de reconocimiento disponible."
            )
            return
        }

        recognizer = try {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } catch (e: Exception) {
            onMessageChanged(
                "No se pudo iniciar el reconocimiento de voz."
            )
            null
        }

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    if (!isReleased && requestActive) {
                        onStateChanged(AuraVoiceState.LISTENING)
                        onMessageChanged("Te estoy escuchando")
                    }
                }

                override fun onBeginningOfSpeech() {
                    if (!isReleased && requestActive) {
                        onMessageChanged("Te escucho...")
                    }
                }

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    if (!isReleased && requestActive) {
                        onStateChanged(AuraVoiceState.PROCESSING)
                        onMessageChanged("Entendiendo lo que dijiste...")
                    }
                }

                override fun onError(error: Int) {

                    if (isReleased || !requestActive) return

                    requestActive = false

                    if (
                        continuousMode &&
                        (
                            error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        )
                    ) {
                        onMessageChanged(
                            "No te escuché bien. Te sigo escuchando."
                        )

                        handler.postDelayed(
                            restartListening,
                            500L
                        )
                    } else {
                        onStateChanged(AuraVoiceState.IDLE)
                        onMessageChanged(errorMessage(error))
                    }
                }

                override fun onResults(results: Bundle?) {

                    if (isReleased || !requestActive) return

                    requestActive = false

                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val recognizedText = matches
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()

                    if (recognizedText.isBlank()) {
                        onStateChanged(AuraVoiceState.IDLE)
                        onMessageChanged(
                            "No pude reconocer lo que dijiste."
                        )
                        return
                    }

                    onTranscriptChanged(recognizedText)
                    onStateChanged(AuraVoiceState.PROCESSING)
                    onMessageChanged("Aura está pensando...")

                    // La respuesta se procesa en MainActivity.
                    // Aquí no se pronuncia una confirmación falsa.
                    onFinalTranscript(recognizedText)
                }

                override fun onPartialResults(partialResults: Bundle?) {

                    if (isReleased || !requestActive) return

                    val matches = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val partial = matches?.firstOrNull()

                    if (!partial.isNullOrBlank()) {
                        onTranscriptChanged(partial)
                    }
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) = Unit
            }
        )
    }

    fun startConversation() {
        if (isReleased) return

        continuousMode = true
        handler.removeCallbacks(restartListening)

        startListeningInternal()
    }

    fun startSingleListening() {
        if (isReleased) return

        continuousMode = false
        handler.removeCallbacks(restartListening)

        startListeningInternal()
    }

    private fun startListeningInternal() {

        if (isReleased || requestActive) return

        val speechRecognizer = recognizer

        if (speechRecognizer == null) {
            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "El servicio de reconocimiento no está disponible."
            )
            return
        }

        onTranscriptChanged("")
        onMessageChanged("Conectando con el reconocimiento de voz...")
        onStateChanged(AuraVoiceState.LISTENING)

        requestActive = true

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
            speechRecognizer.startListening(intent)

        } catch (e: Exception) {
            requestActive = false
            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "No se pudo iniciar la escucha: ${e.message ?: "error desconocido"}"
            )
        }
    }

    fun speakResponse(
        text: String,
        continueListening: Boolean
    ) {

        if (isReleased) return

        continuousMode = continueListening

        if (!isTtsReady) {
            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "La respuesta está lista, pero la voz de salida no está disponible."
            )

            if (continuousMode) {
                handler.postDelayed(restartListening, 500L)
            }

            return
        }

        val result = textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "aura-response"
        )

        if (result == TextToSpeech.ERROR) {
            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "El motor de voz rechazó la respuesta."
            )
        }
    }

    fun stopConversation() {

        continuousMode = false
        handler.removeCallbacks(restartListening)

        try {
            recognizer?.cancel()
            textToSpeech?.stop()
        } catch (_: Exception) {
        }

        requestActive = false

        onStateChanged(AuraVoiceState.IDLE)
        onMessageChanged("Conversación por voz detenida")
    }

    fun stopListening() {
        stopConversation()
    }

    private fun errorMessage(error: Int): String =
        when (error) {
            SpeechRecognizer.ERROR_AUDIO ->
                "Error al acceder al micrófono."

            SpeechRecognizer.ERROR_CLIENT ->
                "El servicio de reconocimiento interrumpió la solicitud."

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Android no concedió permiso para utilizar el micrófono."

            SpeechRecognizer.ERROR_NETWORK ->
                "El servicio de voz tiene un problema de conexión."

            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "El servicio de reconocimiento agotó el tiempo de conexión."

            SpeechRecognizer.ERROR_NO_MATCH ->
                "No pude reconocer las palabras."

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "El servicio de reconocimiento está ocupado."

            SpeechRecognizer.ERROR_SERVER ->
                "El servidor de reconocimiento devolvió un error."

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "No detecté voz."

            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->
                "Se realizaron demasiadas solicitudes de voz."

            else ->
                "Error de reconocimiento de voz. Código: $error"
        }

    fun release() {

        if (isReleased) return

        isReleased = true
        continuousMode = false
        requestActive = false

        handler.removeCallbacksAndMessages(null)

        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = null

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }

        textToSpeech = null
        isTtsReady = false
    }
}
