package com.aura.ai.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object AuraWakeWordNotification {

    const val CHANNEL_ID = "aura_wake_word"
    const val NOTIFICATION_ID = 7401

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return

        val manager = context.getSystemService(
            NotificationManager::class.java
        )

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Escucha de Aura",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                "Indica cuándo Aura mantiene activa la detección de voz."

            setShowBadge(false)
        }

        manager.createNotificationChannel(channel)
    }

    fun build(context: Context): Notification {
        val settingsIntent = Intent(
            context,
            AuraVoiceSettingsActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val settingsPendingIntent = PendingIntent.getActivity(
            context,
            7402,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Aura está escuchando")
            .setContentText(
                "Di Hey Aura, Ey Aura, Oye Aura o Hola Aura."
            )
            .setContentIntent(settingsPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_manage,
                "Configurar",
                settingsPendingIntent
            )
            .build()
    }
}
