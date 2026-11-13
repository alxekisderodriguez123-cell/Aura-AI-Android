package com.aura.ai.actions

import java.text.Normalizer
import java.util.Locale

data class CallContact(
    val contactName: String
) : AuraCommand()

data class WhatsAppContact(
    val contactName: String
) : AuraCommand()

object AuraContactCommandParser {

    private fun normalize(value: String): String =
        Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[¿?¡!.,;:]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun parse(rawText: String): AuraCommand? {
        var text = normalize(rawText)

        text = text
            .replace(Regex("^oye aura\\s*"), "")
            .replace(Regex("^hey aura\\s*"), "")
            .replace(Regex("^ey aura\\s*"), "")
            .replace(Regex("^hola aura\\s*"), "")
            .replace(Regex("^aura\\s*"), "")

        val whatsapp = Regex(
            """^(?:manda|envia|enviar|escribe|escribeme|mandale|enviale)\s+(?:un\s+)?(?:whatsapp|mensaje\s+por\s+whatsapp)\s+(?:a|para)\s+(.+)$"""
        ).find(text)

        if (whatsapp != null) {
            val name = whatsapp.groupValues[1].trim()
            if (name.isNotBlank()) {
                return WhatsAppContact(name)
            }
        }

        val call = Regex(
            """^(?:llama|llamar|haz una llamada|realiza una llamada)\s+(?:a|al)\s+(.+)$"""
        ).find(text)

        if (call != null) {
            val name = call.groupValues[1].trim()

            // Los números telefónicos siguen usando el comando anterior.
            val isPhoneNumber =
                name.matches(Regex("""\+?[\d\s()-]{7,20}"""))

            if (name.isNotBlank() && !isPhoneNumber) {
                return CallContact(name)
            }
        }

        return null
    }
}
