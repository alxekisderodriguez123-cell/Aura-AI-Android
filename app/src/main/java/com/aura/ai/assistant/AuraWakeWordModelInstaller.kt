package com.aura.ai.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

object AuraWakeWordController {

    private val handler = Handler(Looper.getMainLooper())
    private var restartRunnable: Runnable? = null

    fun start(context: Context): Boolean {
        val appContext = context.applicationContext

        cancelScheduledRestart()

        if (!AuraWakeWordPreferences.isEnabled(appContext)) {
            return false
        }

        if (
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.w(
                "AuraWakeWord",
                "No hay permiso de micrófono."
            )
            return false
        }

        if (!AuraWakeWordModelInstaller.isModelInstalled(appContext)) {
            android.util.Log.w(
                "AuraWakeWord",
                "El modelo español todavía no está instalado."
            )
            return false
        }

        val intent = Intent(
            appContext,
            AuraWakeWordService::class.java
        )

        return try {
            if (Build.VERSION.SDK_INT >= 26) {
                ContextCompat.startForegroundService(
                    appContext,
                    intent
                )
            } else {
                appContext.startService(intent)
            }

            true
        } catch (error: Exception) {
            android.util.Log.e(
                "AuraWakeWord",
                "No se pudo iniciar la escucha.",
                error
            )
            false
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext

        appContext.stopService(
            Intent(
                appContext,
                AuraWakeWordService::class.java
            )
        )
    }

    /**
     * Detiene Vosk y espera a que Android libere el micrófono
     * antes de iniciar el reconocedor de órdenes.
     */
    fun stopAndRunAfterRelease(
        context: Context,
        action: () -> Unit
    ) {
        val appContext = context.applicationContext

        cancelScheduledRestart()
        stop(appContext)

        handler.postDelayed(
            {
                action()
            },
            900L
        )
    }

    /**
     * Reactiva Vosk después de terminar una conversación.
     * Si el usuario vuelve a hablar antes, start() cancela
     * esta reactivación pendiente.
     */
    fun restartAfterRelease(
        context: Context,
        delayMillis: Long = 1000L
    ) {
        val appContext = context.applicationContext

        cancelScheduledRestart()

        val task = Runnable {
            restartRunnable = null

            if (AuraWakeWordPreferences.isEnabled(appContext)) {
                start(appContext)
            }
        }

        restartRunnable = task

        handler.postDelayed(
            task,
            delayMillis
        )
    }

    fun cancelScheduledRestart() {
        restartRunnable?.let {
            handler.removeCallbacks(it)
        }

        restartRunnable = null
    }
}
