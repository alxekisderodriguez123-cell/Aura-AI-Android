package com.aura.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.aura.ai.ui.AuraScreen
import com.aura.ai.voice.AuraVoiceController
import com.aura.ai.voice.AuraVoiceState

class MainActivity : ComponentActivity() {

    private var voiceState by mutableStateOf(
        AuraVoiceState.IDLE
    )

    private var transcript by mutableStateOf("")

    private var voiceMessage by mutableStateOf(
        "Pulsa el botón para hablar con Aura"
    )

    private var voiceController: AuraVoiceController? = null

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                voiceController?.startListening()
            } else {
                voiceMessage =
                    "Necesito permiso del micrófono para escucharte"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            }
        )

        setContent {
            AuraScreen(
                voiceState = voiceState,
                transcript = transcript,
                message = voiceMessage,
                onVoiceButtonClick = {
                    handleVoiceButton()
                }
            )
        }
    }

    private fun handleVoiceButton() {
        if (voiceState == AuraVoiceState.LISTENING) {
            voiceController?.stopListening()
            return
        }

        val permissionGranted =
            ContextCompat.checkSelfPermission(
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

        super.onDestroy()
    }
}
