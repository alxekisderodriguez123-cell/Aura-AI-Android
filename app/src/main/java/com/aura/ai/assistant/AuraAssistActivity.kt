package com.aura.ai.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.aura.ai.voice.AuraVoiceState

class AuraAssistActivity : ComponentActivity() {

    private var overlay: AuraVoiceOverlayView? = null
    private var runtime: AuraVoiceRuntime? = null

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startAura()
            } else {
                overlay?.setStatus(
                    "Necesito permiso del micrófono."
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        )

        window.setDimAmount(0.12f)

        val view = AuraVoiceOverlayView(this) {
            closeAura()
        }

        overlay = view
        setContentView(view)

        window.setGravity(Gravity.BOTTOM)

        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startAura()
        } else {
            microphonePermission.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private fun startAura() {
        if (runtime != null) return

        overlay?.setStatus("Te estoy escuchando")
        overlay?.setTranscript("Dime, ¿en qué te ayudo?")

        runtime = AuraVoiceRuntime(
            context = applicationContext,

            onStateChanged = { state ->
                overlay?.setSpeaking(
                    state == AuraVoiceState.SPEAKING
                )
            },

            onTranscriptChanged = { text ->
                if (text.isNotBlank()) {
                    overlay?.setTranscript(text)
                }
            },

            onMessageChanged = { message ->
                overlay?.setStatus(message)
            }
        )

        runtime?.startConversation()
    }

    private fun closeAura() {
        runtime?.stopConversation()
        finish()
    }

    override fun onDestroy() {
        runtime?.release()
        runtime = null

        overlay?.stopOrbAnimation()
        overlay = null

        super.onDestroy()
    }
}
