package com.aura.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.aura.ai.actions.AuraActionEngine
import com.aura.ai.actions.AuraCommand
import com.aura.ai.actions.AuraCommandParser
import com.aura.ai.chat.AuraChatDatabase
import com.aura.ai.chat.ChatMessage
import com.aura.ai.ui.AuraScreen
import com.aura.ai.voice.AuraVoiceController
import com.aura.ai.voice.AuraVoiceState
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var voiceState by mutableStateOf(AuraVoiceState.IDLE)
    private var transcript by mutableStateOf("")
    private var voiceMessage by mutableStateOf(
        "Pulsa el botón para hablar con Aura"
    )

    private var voiceController: AuraVoiceController? = null

    private lateinit var chatDatabase: AuraChatDatabase
    private lateinit var actionEngine: AuraActionEngine
    private lateinit var auraCerebro: AuraCerebro

    private val mainScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val chatMessages = mutableStateListOf<ChatMessage>()

    private var continuousVoiceActive = false
    private var pendingContinuousMode = false

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                if (pendingContinuousMode) {
                    continuousVoiceActive = true
                    voiceController?.startConversation()
                } else {
                    continuousVoiceActive = false
                    voiceController?.startSingleListening()
                }
            } else {
                continuousVoiceActive = false
                voiceMessage =
                    "Necesito permiso del micrófono para escucharte."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // App Check Debug se utiliza únicamente en la compilación debug.
        try {
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
        } catch (e: Exception) {
            android.util.Log.e(
                "Aura",
                "No se pudo inicializar App Check Debug",
                e
            )
        }

        chatDatabase = AuraChatDatabase(applicationContext)
        actionEngine = AuraActionEngine(applicationContext)
        auraCerebro = AuraCerebro(applicationContext)

        chatMessages.addAll(chatDatabase.getMessages())

        voiceController = AuraVoiceController(
            context = applicationContext,

            onStateChanged = { state ->
                voiceState = state
            },

            onTranscriptChanged = { text ->
                transcript = text
            },

            onMessageChanged = { message ->
                voiceMessage = message
            },

            onFinalTranscript = { recognizedText ->
                processMessage(
                    recognizedText,
                    speakResponse = continuousVoiceActive
                )
            }
        )

        setContent {
            AuraScreen(
                voiceState = voiceState,
                transcript = transcript,
                message = voiceMessage,
                chatMessages = chatMessages,

                onVoiceButtonClick = {
                    handleVoiceButton()
                },

                onChatVoiceButtonClick = {
                    handleChatVoiceButton()
                },

                onSendChatMessage = { text ->
                    processMessage(
                        text,
                        speakResponse = false
                    )
                }
            )
        }
    }

    private fun processMessage(
        rawText: String,
        speakResponse: Boolean
    ) {

        val text = rawText.trim()

        if (text.isBlank()) return

        val userMessage = ChatMessage(
            text = text,
            isUser = true
        )

        val savedUserMessage =
            chatDatabase.insertMessage(userMessage)

        chatMessages.add(savedUserMessage)

        val command = AuraCommandParser.parse(text)

        // El token se procesa de forma independiente.
        if (command == AuraCommand.CopyAppCheckToken) {

            actionEngine.copyAppCheckToken { result ->
                finishResponse(result, speakResponse)
            }

            return
        }

        // Los comandos locales no necesitan Gemini.
        if (command != null) {

            val result = actionEngine.execute(command)

            finishResponse(result, speakResponse)

            return
        }

        // Si no es un comando local, consulta Gemini.
        mainScope.launch {

            voiceState = if (speakResponse) {
                AuraVoiceState.PROCESSING
            } else {
                voiceState
            }

            if (speakResponse) {
                voiceMessage = "Aura está pensando..."
            }

            val history = chatMessages.toList()

            val response = try {
                auraCerebro.responder(
                    pregunta = text,
                    historialLocal = history
                )
            } catch (e: Exception) {
                android.util.Log.e(
                    "Aura",
                    "Error procesando mensaje",
                    e
                )

                "No pude procesar tu solicitud. Inténtalo otra vez."
            }

            finishResponse(response, speakResponse)
        }
    }

    private fun finishResponse(
        responseText: String,
        speakResponse: Boolean
    ) {

        val response = ChatMessage(
            text = responseText,
            isUser = false
        )

        val savedResponse =
            chatDatabase.insertMessage(response)

        chatMessages.add(savedResponse)

        if (speakResponse) {
            voiceController?.speakResponse(
                text = responseText,
                continueListening = continuousVoiceActive
            )
        } else {
            if (voiceState == AuraVoiceState.PROCESSING) {
                voiceState = AuraVoiceState.IDLE
                voiceMessage = "Aura está preparada"
            }
        }
    }

    private fun handleVoiceButton() {

        if (continuousVoiceActive) {
            continuousVoiceActive = false
            voiceController?.stopConversation()
            return
        }

        requestMicrophone(continuous = true)
    }

    private fun handleChatVoiceButton() {

        if (
            !continuousVoiceActive &&
            voiceState == AuraVoiceState.LISTENING
        ) {
            voiceController?.stopListening()
            return
        }

        continuousVoiceActive = false

        requestMicrophone(continuous = false)
    }

    private fun requestMicrophone(continuous: Boolean) {

        pendingContinuousMode = continuous

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {

            if (continuous) {
                continuousVoiceActive = true
                voiceController?.startConversation()
            } else {
                continuousVoiceActive = false
                voiceController?.startSingleListening()
            }

        } else {
            microphonePermission.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    override fun onDestroy() {

        continuousVoiceActive = false

        voiceController?.release()
        voiceController = null

        mainScope.cancel()

        if (::chatDatabase.isInitialized) {
            chatDatabase.close()
        }

        super.onDestroy()
    }
}
