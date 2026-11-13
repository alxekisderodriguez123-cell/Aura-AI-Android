package com.aura.ai.assistant

import Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object AuraWakeWordController {

    fun start(context: Context): Boolean {
        val appContext = context.applicationContext

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
        context.applicationContext.stopService(
            Intent(
                context.applicationContext,
                AuraWakeWordService::class.java
            )
        )
    }
}
