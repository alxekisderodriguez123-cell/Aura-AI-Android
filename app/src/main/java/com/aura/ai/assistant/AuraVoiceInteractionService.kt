package com.aura.ai.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.util.Log

class AuraVoiceInteractionService : VoiceInteractionService() {

    companion object {
        @Volatile
        private var instance: AuraVoiceInteractionService? = null

        fun showAura() {
            try {
                instance?.showSession(
                    Bundle(),
                    0
                )
            } catch (error: Exception) {
                Log.e(
                    "AuraVoiceInteraction",
                    "No se pudo mostrar la esfera",
                    error
                )
            }
        }
    }

    override fun onReady() {
        super.onReady()

        instance = this

        AuraWakeWordController.start(this)

        Log.i(
            "AuraVoiceInteraction",
            "Aura está lista para escuchar"
        )
    }

    override fun onShutdown() {
        AuraWakeWordController.stop(this)

        instance = null

        super.onShutdown()
    }
}
