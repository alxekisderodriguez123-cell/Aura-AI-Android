package com.aura.ai

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_ASSISTANT_INVOCATION =
            "com.aura.ai.action.ASSISTANT_INVOCATION"

        const val ACTION_ONE_TAP_VOICE =
            "com.aura.ai.action.ONE_TAP_VOICE"

        const val EXTRA_START_LISTENING =
            "com.aura.ai.extra.START_LISTENING"
    }

    private var voiceState by mutableStateOf(AuraVoiceState.IDLE)
    private var transcript by mutableStateOf("")
    private var voiceMessage by mutableStateOf(
        "Pulsa el botón para hablar con Aura"
    )

    private var assistantRoleHeld by mutableStateOf(false)

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

    private val assistantRoleLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updateAssistantRoleState()
        }

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

        updateAssistantRoleState()

        setContent {
            AuraScreen(
                voiceState = voiceState,
                transcript = transcript,
                message = voiceMessage,
                chatMessages = chatMessages,
                assistantRoleHeld = assistantRoleHeld,

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
                },

                onRequestAssistantRole = {
                    requestAssistantRole()
                }
            )
        }

        if (shouldStartListening(intent)) {
            window.decorView.post {
                requestMicrophone(continuous = true)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (shouldStartListening(intent)) {
            requestMicrophone(continuous = true)
        }
    }

    private fun shouldStartListening(
        incomingIntent: Intent?
    ): Boolean {
        if (incomingIntent == null) return false

        return incomingIntent.action == ACTION_ASSISTANT_INVOCATION ||
            incomingIntent.action == ACTION_ONE_TAP_VOICE ||
            incomingIntent.getBooleanExtra(
                EXTRA_START_LISTENING,
                false
            )
    }

    private fun updateAssistantRoleState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            assistantRoleHeld = false
            return
        }

        val roleManager = getSystemService(
            RoleManager::class.java
        )

        assistantRoleHeld =
            roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT) &&
                roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
    }

    private fun requestAssistantRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            voiceMessage =
                "Esta versión de Android no admite el rol de asistente."
            return
        }

        val roleManager = getSystemService(
            RoleManager::class.java
        )

        if (!roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
            voiceMessage =
                "Android no ofrece el rol de asistente en este dispositivo."
            return
        }

        if (roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
            assistantRoleHeld = true
            voiceMessage = "Aura ya es tu asistente predeterminado."
            return
        }

        assistantRoleLauncher.launch(
            roleManager.createRequestRoleIntent(
                RoleManager.ROLE_ASSISTANT
            )
        )
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

    override fun onResume() {
        super.onResume()
        updateAssistantRoleState()
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
