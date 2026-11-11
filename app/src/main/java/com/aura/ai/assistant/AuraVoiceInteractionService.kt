package com.aura.ai.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.util.Log

class AuraVoiceInteractionService : VoiceInteractionService() {

    companion object {
        private const val TAG = "AuraVoiceInteraction"

        @Volatile
        private var instance: AuraVoiceInteractionService? = null

        fun showAura(): Boolean {
            val service = instance

            if (service == null) {
                Log.e(TAG, "El servicio de voz todavía no está disponible")
                return false
            }

            return try {
                service.showSession(Bundle(), 0)
                true
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo mostrar la esfera", error)
                false
            }
        }
    }

    override fun onReady() {
        super.onReady()

        instance = this

        AuraWakeWordController.start(this)

        Log.i(TAG, "Aura está lista para escuchar")
    }

    override fun onShutdown() {
        AuraWakeWordController.stop(this)

        if (instance === this) {
            instance = null
        }

        Log.i(TAG, "Servicio de interacción de voz detenido")

        super.onShutdown()
    }
}
