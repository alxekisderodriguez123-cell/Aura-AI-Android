package com.aura.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
    private val onMessageChanged: (String) -> Unit
) {

    private val appContext = context.applicationContext

    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private var isTtsReady = false
    private var isReleased = false

    init {
        initializeTextToSpeech()
        initializeSpeechRecognizer()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS && !isReleased) {
                val engine = textToSpeech ?: return@TextToSpeech

                val languageResult = engine.setLanguage(
                    Locale("es", "VE")
                )

                isTtsReady =
                    languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED

                if (isTtsReady) {
                    engine.setSpeechRate(0.96f)
                }
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                    if (!isReleased) {
                        onStateChanged(AuraVoiceState.SPEAKING)
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
                        onMessageChanged("No se pudo reproducir el audio")
                    }
                }
            }
        )
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onMessageChanged(
                "No hay un servicio de reconocimiento de voz disponible"
            )
            return
        }

        recognizer = try {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            } else {
                SpeechRecognizer.createSpeechRecognizer(appContext)
            }
        } catch (exception: Exception) {
            null
        }

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    if (!isReleased) {
                        onStateChanged(AuraVoiceState.LISTENING)
                        onMessageChanged("Te estoy escuchando")
                    }
                }

                override fun onBeginningOfSpeech() {
                    if (!isReleased) {
                        onMessageChanged("Escuchando tu voz...")
                    }
                }

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    if (!isReleased) {
                        onStateChanged(AuraVoiceState.PROCESSING)
                        onMessageChanged("Procesando tu voz...")
                    }
                }

                override fun onError(error: Int) {
                    if (!isReleased) {
                        onStateChanged(AuraVoiceState.IDLE)
                        onMessageChanged(errorMessage(error))
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (isReleased) return

                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val recognizedText = matches?.firstOrNull()
                        ?.trim()
                        .orEmpty()

                    if (recognizedText.isBlank()) {
                        onStateChanged(AuraVoiceState.IDLE)
                        onMessageChanged("No pude reconocer lo que dijiste")
                        return
                    }

                    onTranscriptChanged(recognizedText)
                    onMessageChanged("He reconocido tu mensaje")

                    speak(
                        "He reconocido: $recognizedText"
                    )
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (isReleased) return

                    val matches = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val partialText = matches?.firstOrNull()

                    if (!partialText.isNullOrBlank()) {
                        onTranscriptChanged(partialText)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }
        )
    }

    fun startListening() {
        if (isReleased) return

        val speechRecognizer = recognizer

        if (speechRecognizer == null) {
            onMessageChanged(
                "El reconocimiento de voz no está disponible"
            )
            return
        }

        onTranscriptChanged("")
        onMessageChanged("Preparando el micrófono...")
        onStateChanged(AuraVoiceState.LISTENING)

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "es-VE"
            )

            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )

            putExtra(
                RecognizerIntent.EXTRA_PREFER_OFFLINE,
                true
            )
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (exception: Exception) {
            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "No se pudo iniciar el reconocimiento de voz"
            )
        }
    }

    fun stopListening() {
        if (isReleased) return

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
            onStateChanged(AuraVoiceState.IDLE)
        }
    }

    private fun speak(text: String) {
        if (isReleased) return

        if (!isTtsReady) {
            onStateChanged(AuraVoiceState.IDLE)
            onMessageChanged(
                "Texto reconocido. La voz de salida no está disponible"
            )
            return
        }

        onMessageChanged("Aura está respondiendo...")

        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "aura-voice-confirmation"
        )
    }

    private fun errorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO ->
                "Error al acceder al audio"

            SpeechRecognizer.ERROR_CLIENT ->
                "Se interrumpió el reconocimiento"

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Falta el permiso del micrófono"

            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Error de conexión del servicio de voz"

            SpeechRecognizer.ERROR_NO_MATCH ->
                "No pude entender lo que dijiste"

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "El reconocimiento está ocupado"

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "No detecté ninguna voz"

            else ->
                "Ocurrió un error de reconocimiento"
        }
    }

    fun release() {
        if (isReleased) return

        isReleased = true

        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
            // El servicio puede haberse cerrado previamente.
        }

        recognizer = null

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
            // El motor de voz puede haberse cerrado previamente.
        }

        textToSpeech = null
        isTtsReady = false
    }
}
