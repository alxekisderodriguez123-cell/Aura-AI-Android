package com.aura.ai.assistant

import android.content.Context
import android.util.Log
import com.aura.ai.AuraCerebro
import com.aura.ai.actions.AuraActionEngine
import com.aura.ai.actions.AuraCommandParser
import com.aura.ai.chat.AuraChatDatabase
import com.aura.ai.chat.ChatMessage
import com.aura.ai.voice.AuraVoiceController
import com.aura.ai.voice.AuraVoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AuraVoiceRuntime(
    context: Context,
    private val onStateChanged: (AuraVoiceState) -> Unit,
    private val onTranscriptChanged: (String) -> Unit,
    private val onMessageChanged: (String) -> Unit
) {

    private val appContext = context.applicationContext

    private val database = AuraChatDatabase(appContext)
    private val actionEngine = AuraActionEngine(appContext)
    private val cerebro = AuraCerebro(appContext)

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val voiceController = AuraVoiceController(
        context = appContext,
        onStateChanged = onStateChanged,
        onTranscriptChanged = onTranscriptChanged,
        onMessageChanged = onMessageChanged,
        onFinalTranscript = { text ->
            processMessage(text)
        }
    )

    private var released = false

    fun startConversation() {
        if (!released) {
            voiceController.startConversation()
        }
    }

    fun stopConversation() {
        if (!released) {
            voiceController.stopConversation()
        }
    }

    fun interruptSpeechAndListen() {
        if (released) return

        voiceController.stopConversation()
        voiceController.startConversation()
    }

    private fun processMessage(rawText: String) {
        if (released) return

        val text = rawText.trim()
        if (text.isBlank()) return

        scope.launch {
            try {
                database.insertMessage(
                    ChatMessage(
                        text = text,
                        isUser = true
                    )
                )

                val command = AuraCommandParser.parse(text)

                val response = if (command != null) {
                    actionEngine.execute(command)
                } else {
                    cerebro.responder(
                        pregunta = text,
                        historialLocal = database.getMessages()
                    )
                }

                finishResponse(response)

            } catch (error: Exception) {
                Log.e(
                    "AuraVoiceRuntime",
                    "Error procesando solicitud",
                    error
                )

                finishResponse(
                    "Ocurrió un problema al procesar tu solicitud."
                )
            }
        }
    }

    private fun finishResponse(response: String) {
        if (released) return

        database.insertMessage(
            ChatMessage(
                text = response,
                isUser = false
            )
        )

        onTranscriptChanged(response)

        voiceController.speakResponse(
            text = response,
            continueListening = true
        )
    }

    fun release() {
        if (released) return

        released = true

        voiceController.release()
        scope.cancel()
        database.close()
    }
}
