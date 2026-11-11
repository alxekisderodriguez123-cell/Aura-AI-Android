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
                closeAura()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        window.clearFlags(
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )

        val view = AuraVoiceOverlayView(
            context = this,

            onTapWhileSpeaking = {
                runtime?.interruptSpeechAndListen()
            },

            onClose = {
                closeAura()
            }
        )

        overlay = view
        setContentView(view)

        window.setGravity(
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )

        window.setLayout(
            dp(148),
            dp(148)
        )

        window.attributes = window.attributes.apply {
            y = dp(22)
        }

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

        overlay?.startOrbAnimation()

        runtime = AuraVoiceRuntime(
            context = applicationContext,

            onStateChanged = { state ->
                overlay?.setSpeaking(
                    state == AuraVoiceState.SPEAKING
                )
            },

            onTranscriptChanged = {
                // La esfera no muestra transcripciones.
            },

            onMessageChanged = {
                // La respuesta se escucha mediante TTS.
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
