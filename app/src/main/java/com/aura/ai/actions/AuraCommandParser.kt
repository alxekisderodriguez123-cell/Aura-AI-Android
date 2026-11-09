
package com.aura.ai.actions

import java.text.Normalizer
import java.util.Locale

sealed class AuraCommand {
    data class OpenApp(val name: String) : AuraCommand()
    data class StartTimer(val seconds: Int) : AuraCommand()
    data class SetAlarm(val hour: Int, val minute: Int) : AuraCommand()
    data object OpenWifiPanel : AuraCommand()
    data object OpenBluetoothPanel : AuraCommand()
    data object OpenInternetPanel : AuraCommand()
    data object VolumeUp : AuraCommand()
    data object VolumeDown : AuraCommand()
    data object MuteVolume : AuraCommand()
    data object OpenDisplaySettings : AuraCommand()
    data object OpenGeneralSettings : AuraCommand()
    data class OpenMaps(val place: String) : AuraCommand()
    data class DialNumber(val number: String) : AuraCommand()
    data class ComposeSms(val number: String) : AuraCommand()
}

object AuraCommandParser {

    private fun normalize(value: String): String {
        return Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun parse(rawText: String): AuraCommand? {
        val text = normalize(rawText)
        if (text.isBlank()) return null

        if (
            text.contains("wifi") ||
            text.contains("wi fi")
        ) {
            return AuraCommand.OpenWifiPanel
        }

        if (text.contains("bluetooth")) {
            return AuraCommand.OpenBluetoothPanel
        }

        if (
            text.contains("datos moviles") ||
            text.contains("internet") ||
            text.contains("conexion")
        ) {
            return AuraCommand.OpenInternetPanel
        }

        if (
            text.contains("sube el volumen") ||
            text.contains("sube volumen") ||
            text.contains("aumenta el volumen") ||
            text.contains("mas volumen")
        ) {
            return AuraCommand.VolumeUp
        }

        if (
            text.contains("baja el volumen") ||
            text.contains("baja volumen") ||
            text.contains("disminuye el volumen") ||
            text.contains("menos volumen")
        ) {
            return AuraCommand.VolumeDown
        }

        if (
            text.contains("silencio") ||
            text.contains("silencia") ||
            text.contains("quita el sonido") ||
            text.contains("mute")
        ) {
            return AuraCommand.MuteVolume
        }

        if (
            text.contains("brillo") ||
            text.contains("pantalla")
        ) {
            return AuraCommand.OpenDisplaySettings
        }

        if (
            text == "abre ajustes" ||
            text == "abre configuracion" ||
            text == "abre los ajustes" ||
            text == "configuracion del telefono"
        ) {
            return AuraCommand.OpenGeneralSettings
        }

        val timerMatch = Regex(
            """(?:temporizador|temporizador|timer|cuenta atras)(?:\s+de)?\s+(\d+)\s*(segundos?|seg|minutos?|min|horas?|h)"""
        ).find(text)

        if (timerMatch != null) {
            val amount = timerMatch.groupValues[1].toLongOrNull()
                ?: return null

            val unit = timerMatch.groupValues[2]
            val multiplier = when {
                unit.startsWith("seg") -> 1L
                unit.startsWith("min") -> 60L
                else -> 3600L
            }

            val seconds = (amount * multiplier)
                .coerceIn(1L, 86400L)
                .toInt()

            return AuraCommand.StartTimer(seconds)
        }

        if (
            text.contains("alarma") ||
            text.contains("despiertame") ||
            text.contains("pon una alarma") ||
            text.contains("pon la alarma")
        ) {
            val timeMatch = Regex(
                """(?:a las|para las|alarma)\s+(\d{1,2})(?::(\d{2}))?"""
            ).find(text)

            if (timeMatch != null) {
                val hour = timeMatch.groupValues[1].toIntOrNull()
                    ?: return null
                val minute = timeMatch.groupValues[2]
                    .takeIf { it.isNotBlank() }
                    ?.toIntOrNull() ?: 0

                if (hour in 0..23 && minute in 0..59) {
                    return AuraCommand.SetAlarm(hour, minute)
                }
            }
        }

        if (
            text.contains("mapa") ||
            text.contains("maps") ||
            text.contains("google maps") ||
            text.startsWith("navega")
        ) {
            val place = text
                .replace(Regex("^(abre|abrir|inicia|iniciar)\\s+"), "")
                .replace("google maps", "")
                .replace("maps", "")
                .replace("mapas", "")
                .replace(Regex("^(navega|navegar)\\s+(hasta|a|hacia)\\s+"), "")
                .replace(Regex("^(a|hasta|hacia)\\s+"), "")
                .trim()

            return AuraCommand.OpenMaps(place)
        }

        val phone = Regex("""\+?\d[\d\s-]{5,18}\d""")
            .find(text)
            ?.value
            ?.replace(Regex("[^\\d+]"), "")

        if (
            phone != null &&
            (text.startsWith("llama") || text.startsWith("llamar"))
        ) {
            return AuraCommand.DialNumber(phone)
        }

        if (
            phone != null &&
            (
                text.startsWith("mensaje") ||
                text.startsWith("manda mensaje") ||
                text.startsWith("envia mensaje")
            )
        ) {
            return AuraCommand.ComposeSms(phone)
        }

        val openPrefixes = listOf(
            "abre la aplicacion ",
            "abre la app ",
            "abre ",
            "abrir ",
            "inicia ",
            "iniciar ",
            "ejecuta "
        )

        val prefix = openPrefixes.firstOrNull {
            text.startsWith(it)
        }

        if (prefix != null) {
            val appName = text.removePrefix(prefix).trim()
            if (appName.isNotBlank()) {
                return AuraCommand.OpenApp(appName)
            }
        }

        return null
    }
}
