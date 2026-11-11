package com.aura.ai.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object AuraWakeWordController {

    fun start(context: Context) {
        val appContext = context.applicationContext

        if (!AuraWakeWordPreferences.isEnabled(appContext)) {
            return
        }

        if (
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(
            appContext,
            AuraWakeWordService::class.java
        )

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                ContextCompat.startForegroundService(
                    appContext,
                    intent
                )
            } else {
                appContext.startService(intent)
            }
        } catch (error: Exception) {
            android.util.Log.e(
                "AuraWakeWord",
                "No se pudo iniciar la escucha",
                error
            )
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
