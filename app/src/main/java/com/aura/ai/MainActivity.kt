package com.aura.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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

    private var pendingMusicCommand: AuraCommand? = null
    private var pendingMusicSpeakResponse = false

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

    private val audioPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            val command = pendingMusicCommand
            val speakResponse = pendingMusicSpeakResponse

            pendingMusicCommand = null
            pendingMusicSpeakResponse = false

            if (command != null) {
                val result = if (granted) {
                    actionEngine.execute(command)
                } else {
                    "No tengo permiso para consultar tu música local. " +
                        "Voy a buscarla en YouTube Music."
                }

                if (granted) {
                    finishResponse(result, speakResponse)
                } else {
                    val fallback = actionEngine.execute(
                        command.copyForExternalMusic()
                    )
                    finishResponse(fallback, speakResponse)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        if (command == AuraCommand.CopyAppCheckToken) {
            actionEngine.copyAppCheckToken { result ->
                finishResponse(result, speakResponse)
            }
            return
        }

        if (command != null) {

            if (
                command is AuraCommand.SearchYouTubeMusic &&
                command.play &&
                !hasAudioPermission()
            ) {
                pendingMusicCommand = command
                pendingMusicSpeakResponse = speakResponse
                audioPermission.launch(audioPermissionName())
                return
            }

            val result = actionEngine.execute(command)
            finishResponse(result, speakResponse)
            return
        }

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

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            audioPermissionName()
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun audioPermissionName(): String {
        return if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
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

/*
 * La orden de reproducción se conserva, pero el motor recibe una búsqueda
 * externa cuando el usuario deniega el permiso de biblioteca local.
 */
private fun AuraCommand.copyForExternalMusic(): AuraCommand {
    return when (this) {
        is AuraCommand.SearchYouTubeMusic ->
            AuraCommand.SearchYouTubeMusic(
                query = query,
                play = true
            )

        else -> this
    }
}
