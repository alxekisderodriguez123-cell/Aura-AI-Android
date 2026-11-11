package com.aura.ai.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
                "Mantiene activa la detección de la palabra de activación."

            setShowBadge(false)
        }

        manager.createNotificationChannel(channel)
    }

    fun build(context: Context): Notification {
        return NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Aura está escuchando")
            .setContentText(
                "Di Hey Aura, Ey Aura, Oye Aura o Hola Aura."
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
