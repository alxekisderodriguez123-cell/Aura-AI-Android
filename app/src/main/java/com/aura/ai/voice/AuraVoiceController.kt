
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
    private var audioReported = false

    private val recognitionTimeout = Runnable {
        if (requestActive && !isReleased) {
            requestActive = false

            try {
                recognizer?.cancel()
            } catch (_: Exception) {
            }

            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "No recibí una respuesta del servicio de reconocimiento. Comprueba el motor de voz de Android."
            )
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

                val languageResult = engine.setLanguage(
                    Locale("es", "US")
                )

                isTtsReady =
                    languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED

                if (isTtsReady) {
                    engine.setSpeechRate(0.96f)
                    onMessageChanged("Voz de Aura preparada")
                } else {
                    onMessageChanged(
                        "El motor de voz no tiene español instalado"
                    )
                }
            } else if (!isReleased) {
                onMessageChanged(
                    "No se pudo inicializar la síntesis de voz"
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
                        onStateChanged(AuraVoiceState.IDLE)
                        onMessageChanged("Aura está preparada")
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (!isReleased) {
                        onStateChanged(AuraVoiceState.IDLE)
                        onMessageChanged(
                            "No se pudo reproducir la respuesta de voz"
                        )
                    }
                }
            }
        )
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onMessageChanged(
                "Android no tiene un servicio de reconocimiento disponible"
            )
            return
        }

        recognizer = try {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } catch (_: Exception) {
            onMessageChanged(
                "No se pudo iniciar el servicio de reconocimiento"
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
                        audioReported = true
                        onMessageChanged("He detectado tu voz...")
                    }
                }

                override fun onRmsChanged(rmsdB: Float) {
                    if (
                        !isReleased &&
                        requestActive &&
                        !audioReported
                    ) {
                        audioReported = true
                        onMessageChanged(
                            "Recibiendo señal de audio..."
                        )
                    }
                }

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
                    handler.removeCallbacks(recognitionTimeout)

                    onStateChanged(AuraVoiceState.IDLE)
                    onMessageChanged(errorMessage(error))
                }

                override fun onResults(results: Bundle?) {
                    if (isReleased || !requestActive) return

                    requestActive = false
                    handler.removeCallbacks(recognitionTimeout)

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
                            "El servicio respondió, pero no reconoció palabras"
                        )
                        return
                    }

                    onTranscriptChanged(recognizedText)
                    onMessageChanged("Procesando tu instrucción...")
                    onFinalTranscript(recognizedText)

                    speak("He reconocido: $recognizedText")
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (isReleased || !requestActive) return

                    val matches = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val partialText = matches?.firstOrNull()

                    if (!partialText.isNullOrBlank()) {
                        onTranscriptChanged(partialText)
                    }
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) = Unit
            }
        )
    }

    fun startListening() {
        if (isReleased || requestActive) return

        val speechRecognizer = recognizer

        if (speechRecognizer == null) {
            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "El servicio de reconocimiento no está disponible"
            )
            return
        }

        onTranscriptChanged("")
        onMessageChanged("Conectando con el reconocimiento de voz...")
        onStateChanged(AuraVoiceState.LISTENING)

        requestActive = true
        audioReported = false

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

            handler.removeCallbacks(recognitionTimeout)
            handler.postDelayed(
                recognitionTimeout,
                20000L
            )
        } catch (exception: Exception) {
            requestActive = false
            handler.removeCallbacks(recognitionTimeout)

            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "No se pudo iniciar la escucha: ${exception.message ?: "error desconocido"}"
            )
        }
    }

    fun stopListening() {
        if (isReleased || !requestActive) return

        requestActive = false
        handler.removeCallbacks(recognitionTimeout)

        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }

        onStateChanged(AuraVoiceState.IDLE)
        onMessageChanged("Escucha detenida")
    }

    private fun speak(text: String) {
        if (isReleased) return

        if (!isTtsReady) {
            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "Reconocí tu mensaje, pero la voz de salida no está disponible"
            )
            return
        }

        val result = textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "aura-voice-confirmation"
        )

        if (result == TextToSpeech.ERROR) {
            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "El motor de voz rechazó la reproducción"
            )
        }
    }

    private fun errorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO ->
                "Error al acceder al audio del micrófono"

            SpeechRecognizer.ERROR_CLIENT ->
                "El servicio de reconocimiento interrumpió la solicitud"

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Android no concedió permiso para utilizar el micrófono"

            SpeechRecognizer.ERROR_NETWORK ->
                "El servicio de voz tiene un problema de conexión"

            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "El servicio de voz agotó el tiempo de conexión"

            SpeechRecognizer.ERROR_NO_MATCH ->
                "No pude reconocer las palabras. Inténtalo otra vez"

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "El servicio de reconocimiento está ocupado"

            SpeechRecognizer.ERROR_SERVER ->
                "El servidor de reconocimiento devolvió un error"

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "No detecté voz. Comprueba el micrófono y vuelve a intentarlo"

            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->
                "Se realizaron demasiadas solicitudes de voz"

            else ->
                "Error de reconocimiento de voz. Código: $error"
        }
    }

    fun release() {
        if (isReleased) return

        isReleased = true
        requestActive = false

        handler.removeCallbacks(recognitionTimeout)

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
