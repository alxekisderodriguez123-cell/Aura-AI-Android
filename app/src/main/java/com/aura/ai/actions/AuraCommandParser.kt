
package com.aura.ai.actions

import java.text.Normalizer
import java.util.Locale

sealed class AuraCommand {
    data class OpenApp(val name: String) : AuraCommand()
    data class StartTimer(val seconds: Int) : AuraCommand()
    data class SetAlarm(val hour: Int, val minute: Int) : AuraCommand()
    data class Navigate(val destination: String) : AuraCommand()
    data class SearchMaps(val query: String) : AuraCommand()
    data class SearchYouTube(val query: String, val play: Boolean) : AuraCommand()
    data class SearchYouTubeMusic(val query: String, val play: Boolean) : AuraCommand()
    data class SearchSpotify(val query: String, val play: Boolean) : AuraCommand()
    data class SearchGoogle(val query: String) : AuraCommand()
    data class Volume(val operation: String) : AuraCommand()
    data class Brightness(val operation: String) : AuraCommand()
    data object Wifi : AuraCommand()
    data object Bluetooth : AuraCommand()
    data object Internet : AuraCommand()
    data object OpenSettings : AuraCommand()
    data class Dial(val number: String) : AuraCommand()
    data class Sms(val number: String) : AuraCommand()
}

object AuraCommandParser {

    private fun normalize(value: String): String =
        Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[¿?¡!.,;:]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private val numberWords = mapOf(
        "un" to 1, "uno" to 1, "una" to 1,
        "dos" to 2, "tres" to 3, "cuatro" to 4,
        "cinco" to 5, "seis" to 6, "siete" to 7,
        "ocho" to 8, "nueve" to 9, "diez" to 10,
        "once" to 11, "doce" to 12, "trece" to 13,
        "catorce" to 14, "quince" to 15,
        "dieciseis" to 16, "diecisiete" to 17,
        "dieciocho" to 18, "diecinueve" to 19,
        "veinte" to 20, "veintiuno" to 21,
        "veintidos" to 22, "veintitres" to 23,
        "veinticuatro" to 24, "veinticinco" to 25,
        "veintiseis" to 26, "veintisiete" to 27,
        "veintiocho" to 28, "veintinueve" to 29,
        "treinta" to 30, "cuarenta" to 40,
        "cincuenta" to 50, "sesenta" to 60
    )

    private fun numberText(value: String): String {
        var result = value
        numberWords.forEach { (word, number) ->
            result = result.replace(
                Regex("\\b$word\\b"),
                number.toString()
            )
        }
        return result
    }

    fun parse(rawText: String): AuraCommand? {
        var text = normalize(rawText)
        if (text.isBlank()) return null

        text = text.replace(Regex("^oye aura\\s*"), "")
            .replace(Regex("^hey aura\\s*"), "")
            .replace(Regex("^aura\\s*"), "")

        // Navegación real: inicia la ruta en Google Maps.
        val route = Regex(
            """^(llevame|guiame|dirigeme|navega|quiero ir|como llego|dame la ruta|ruta a)\s+(?:(?:a la|al|a|hacia|hasta|para)\s+)?(.+)$"""
        ).find(text)

        if (route != null) {
            val destination = route.groupValues[2].trim()
            if (destination.isNotBlank()) {
                return AuraCommand.Navigate(destination)
            }
        }

        // Búsquedas de lugares sin iniciar navegación.
        val mapSearch = Regex(
            """^(busca|buscame|encuentra|muestrame|donde queda|donde esta)\s+(.+?)(?:\s+en (?:google )?maps?)?$"""
        ).find(text)

        if (mapSearch != null &&
            (text.contains("mapa") || text.contains("maps") ||
             text.startsWith("donde queda") || text.startsWith("donde esta"))
        ) {
            return AuraCommand.SearchMaps(mapSearch.groupValues[2].trim())
        }

        // Temporizadores: admite cifras y números escritos.
        val timerText = numberText(text)
        val timer = Regex(
            """(?:temporizador|timer|cuenta atras)(?:\s+de)?\s+(.+)"""
        ).find(timerText)

        if (timer != null) {
            var seconds = 0L
            val duration = timer.groupValues[1]

            Regex("""(\d+)\s*(horas?|h|minutos?|mins?|min|segundos?|segs?|seg)""")
                .findAll(duration)
                .forEach { match ->
                    val amount = match.groupValues[1].toLongOrNull() ?: 0L
                    val unit = match.groupValues[2]
                    seconds += when {
                        unit.startsWith("hora") || unit == "h" -> amount * 3600
                        unit.startsWith("min") -> amount * 60
                        else -> amount
                    }
                }

            if (seconds in 1..86400) {
                return AuraCommand.StartTimer(seconds.toInt())
            }
        }

        // Alarmas.
        if (text.contains("alarma") || text.contains("despiertame")) {
            val alarmText = numberText(text)
            val match = Regex(
                """(?:a las|para las|alarma a las)\s+(\d{1,2})(?::(\d{2}))?"""
            ).find(alarmText) ?: return null

            var hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: 0

            if (hour !in 0..23 || minute !in 0..59) return null

            val pm = text.contains("pm") ||
                text.contains("de la tarde") ||
                text.contains("de la noche")
            val am = text.contains("am") ||
                text.contains("de la manana") ||
                text.contains("de la madrugada")

            if (pm && hour in 1..11) hour += 12
            if (am && hour == 12) hour = 0

            return AuraCommand.SetAlarm(hour, minute)
        }

        // Búsqueda y reproducción multimedia.
        val mediaVerb = Regex(
            """^(pon|ponme|reproduce|reproduceme|toca|escucha|quiero escuchar|quiero oir|busca|buscame)\s+(.+)$"""
        ).find(text)

        if (mediaVerb != null) {
            val originalQuery = mediaVerb.groupValues[2].trim()
            val play = !text.startsWith("busca") &&
                !text.startsWith("buscame")

            val provider = when {
                originalQuery.contains("youtube music") ||
                    originalQuery.contains("ytmusic") ||
                    originalQuery.contains("yt music") -> "ytmusic"
                originalQuery.contains("spotify") -> "spotify"
                originalQuery.contains("youtube") -> "youtube"
                else -> null
            }

            val query = originalQuery
                .replace(Regex("""\b(en|por|desde|usando)\s+(youtube music|yt music|ytmusic|youtube|spotify)\b"""), "")
                .replace(Regex("""\b(youtube music|yt music|ytmusic|youtube|spotify)\b"""), "")
                .replace(Regex("""^(la cancion|el tema|la musica|un video|el video)\s+"""), "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (query.isNotBlank()) {
                return when (provider) {
                    "spotify" -> AuraCommand.SearchSpotify(query, play)
                    "ytmusic" -> AuraCommand.SearchYouTubeMusic(query, play)
                    "youtube" -> AuraCommand.SearchYouTube(query, play)
                    else -> AuraCommand.SearchYouTubeMusic(query, play)
                }
            }
        }

        // Búsqueda general en Google.
        val google = Regex(
            """^(busca|buscame|googlea|busca en google)\s+(.+)$"""
        ).find(text)

        if (google != null) {
            return AuraCommand.SearchGoogle(google.groupValues[2].trim())
        }

        // Volumen.
        if (text.contains("volumen") || text.contains("silencia") ||
            text.contains("silencio") || text.contains("mute")
        ) {
            val operation = when {
                text.contains("maximo") -> "max"
                text.contains("minimo") -> "min"
                text.contains("silencia") || text.contains("silencio") ||
                    text.contains("mute") -> "mute"
                text.contains("baja") || text.contains("reduce") ||
                    text.contains("disminuye") || text.contains("menos") -> "down"
                text.contains("sube") || text.contains("aumenta") ||
                    text.contains("mas") -> "up"
                else -> return null
            }
            return AuraCommand.Volume(operation)
        }

        // Brillo.
        if (text.contains("brillo")) {
            val operation = when {
                text.contains("maximo") -> "max"
                text.contains("minimo") -> "min"
                text.contains("baja") || text.contains("reduce") ||
                    text.contains("menos") -> "down"
                text.contains("sube") || text.contains("aumenta") ||
                    text.contains("mas") -> "up"
                else -> return null
            }
            return AuraCommand.Brightness(operation)
        }

        if (text.contains("bluetooth")) return AuraCommand.Bluetooth
        if (text.contains("wifi") || text.contains("wi fi")) return AuraCommand.Wifi
        if (text.contains("datos moviles") || text == "abre internet") {
            return AuraCommand.Internet
        }

        if (text == "abre ajustes" || text == "abre configuracion" ||
            text == "abre los ajustes"
        ) return AuraCommand.OpenSettings

        val phone = Regex("""\+?\d[\d\s-]{5,18}\d""")
            .find(text)?.value?.replace(Regex("[^\\d+]"), "")

        if (phone != null && text.startsWith("llama")) {
            return AuraCommand.Dial(phone)
        }

        if (phone != null &&
            (text.startsWith("mensaje") || text.startsWith("manda mensaje"))
        ) return AuraCommand.Sms(phone)

        val openPrefix = listOf(
            "abre la aplicacion ",
            "abre la app ",
            "abre ",
            "abrir ",
            "inicia ",
            "ejecuta ",
            "lanza "
        ).firstOrNull { text.startsWith(it) }

        if (openPrefix != null) {
            val app = text.removePrefix(openPrefix).trim()
            if (app.isNotBlank()) return AuraCommand.OpenApp(app)
        }

        return null
    }
}
