
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
import com.aura.ai.actions.AuraCommandParser
import com.aura.ai.chat.AuraChatDatabase
import com.aura.ai.chat.ChatMessage
import com.aura.ai.ui.AuraScreen
import com.aura.ai.voice.AuraVoiceController
import com.aura.ai.voice.AuraVoiceState

class MainActivity : ComponentActivity() {

    private var voiceState by mutableStateOf(AuraVoiceState.IDLE)
    private var transcript by mutableStateOf("")
    private var voiceMessage by mutableStateOf(
        "Pulsa el botón para hablar con Aura"
    )

    private var voiceController: AuraVoiceController? = null
    private lateinit var chatDatabase: AuraChatDatabase
    private lateinit var actionEngine: AuraActionEngine

    private val chatMessages = mutableStateListOf<ChatMessage>()

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                voiceController?.startListening()
            } else {
                voiceMessage =
                    "Necesito permiso del micrófono para escucharte."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chatDatabase = AuraChatDatabase(applicationContext)
        actionEngine = AuraActionEngine(this)

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
                sendChatMessage(recognizedText)
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
                onSendChatMessage = { text ->
                    sendChatMessage(text)
                }
            )
        }
    }

    private fun sendChatMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        val userMessage = ChatMessage(
            text = text,
            isUser = true
        )

        val savedUserMessage = chatDatabase.insertMessage(userMessage)
        chatMessages.add(savedUserMessage)

        val command = AuraCommandParser.parse(text)

        val responseText = if (command != null) {
            actionEngine.execute(command)
        } else {
            "He guardado tu mensaje. Todavía no tengo conectado el motor de conversación inteligente. Puedo ejecutar comandos locales como abrir aplicaciones, configurar temporizadores, abrir ajustes y controlar el volumen."
        }

        val response = ChatMessage(
            text = responseText,
            isUser = false
        )

        val savedResponse = chatDatabase.insertMessage(response)
        chatMessages.add(savedResponse)
    }

    private fun handleVoiceButton() {
        if (voiceState == AuraVoiceState.LISTENING) {
            voiceController?.stopListening()
            return
        }

        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            voiceController?.startListening()
        } else {
            microphonePermission.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    override fun onDestroy() {
        voiceController?.release()
        voiceController = null

        if (::chatDatabase.isInitialized) {
            chatDatabase.close()
        }

        super.onDestroy()
    }
}
