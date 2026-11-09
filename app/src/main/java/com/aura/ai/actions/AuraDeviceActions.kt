
package com.aura.ai.actions

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings

class AuraDeviceActions(private val context: Context) {

    fun volume(operation: String): String {
        val audio = context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

        val stream = AudioManager.STREAM_MUSIC
        val max = audio.getStreamMaxVolume(stream)
        val current = audio.getStreamVolume(stream)

        val next = when (operation) {
            "up" -> current + maxOf(1, max / 5)
            "down" -> current - maxOf(1, max / 5)
            "max" -> max
            "min", "mute" -> 0
            else -> current
        }.coerceIn(0, max)

        audio.setStreamVolume(
            stream,
            next,
            AudioManager.FLAG_SHOW_UI
        )

        return if (next == 0) {
            "He silenciado el volumen multimedia."
        } else {
            "Volumen multimedia al ${next * 100 / max} por ciento."
        }
    }

    fun brightness(operation: String): String {
        if (!Settings.System.canWrite(context)) {
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(permissionIntent)
            return "Necesito autorización para modificar el brillo. Activa el permiso de Aura y vuelve a pedírmelo."
        }

        val resolver = context.contentResolver
        val current = Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            128
        )

        val next = when (operation) {
            "up" -> current + 50
            "down" -> current - 50
            "max" -> 255
            "min" -> 10
            else -> current
        }.coerceIn(10, 255)

        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )

        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            next
        )

        return "Brillo ajustado al ${next * 100 / 255} por ciento."
    }

    fun wifi(): String = openPanel(
        Settings.Panel.ACTION_WIFI,
        "Abriendo los controles de Wi-Fi."
    )

    fun internet(): String = openPanel(
        Settings.Panel.ACTION_INTERNET_CONNECTIVITY,
        "Abriendo los controles de Internet y conectividad."
    )

    fun bluetooth(): String {
        val intent = Intent(
            Settings.ACTION_BLUETOOTH_SETTINGS
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
        return "Abriendo los ajustes de Bluetooth."
    }

    fun settings(): String {
        val intent = Intent(
            Settings.ACTION_SETTINGS
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) etc

        context.startActivity(intent)
        return "Abriendo los ajustes del teléfono."
    }

    private fun openPanel(action: String, message: String): String {
        val intent = Intent(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
        return message
    }
}
