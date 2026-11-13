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
    private var releaseRunnable: Runnable? = null

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

    fun stopAndRunAfterRelease(
        context: Context,
        action: () -> Unit
    ) {
        val appContext = context.applicationContext

        cancelScheduledRestart()

        releaseRunnable?.let {
            handler.removeCallbacks(it)
        }

        stop(appContext)

        val task = Runnable {
            releaseRunnable = null
            action()
        }

        releaseRunnable = task

        handler.postDelayed(
            task,
            900L
        )
    }

    fun restartAfterRelease(
        context: Context,
        delayMillis: Long = 1000L
    ) {
        val appContext = context.applicationContext

        releaseRunnable?.let {
            handler.removeCallbacks(it)
        }

        releaseRunnable = null

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
