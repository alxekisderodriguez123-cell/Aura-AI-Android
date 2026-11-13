package com.aura.ai.actions

import java.text.Normalizer
import java.util.Locale

data class CallContact(
    val contactName: String
) : AuraCommand()

data class FindContact(
    val contactName: String
) : AuraCommand()

data class GetContactNumber(
    val contactName: String
) : AuraCommand()

data class WhatsAppContact(
    val contactName: String,
    val message: String? = null
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

    private fun cleanName(value: String): String {
        return value.trim()
            .removePrefix("a ")
            .removePrefix("al ")
            .trim()
    }

    fun parse(rawText: String): AuraCommand? {
        var text = normalize(rawText)

        if (text.isBlank()) return null

        // Eliminar frases de activación.
        text = text
            .replace(
                Regex("^(oye aura|hey aura|ey aura|hola aura|aura)\\s*"),
                ""
            )
            .replace(
                Regex("^(ahora|por favor)\\s*"),
                ""
            )
            .trim()

        if (text.isBlank()) return null

        // WhatsApp o mensaje con contenido incluido.
        val whatsapp = Regex(
            """^(?:manda|mandar|envia|enviar|mandale|enviale|escribe|escribeme|escribile)\s+(?:un\s+)?(?:whatsapp|mensaje(?:\s+por\s+whatsapp)?)\s+(?:a|para)\s+(.+?)(?:\s+(?:diciendo|que diga|con el mensaje)\s+(.+))?$"""
        ).find(text)

        if (whatsapp != null) {
            val name = cleanName(
                whatsapp.groupValues[1]
            )

            val message = whatsapp.groupValues
                .getOrNull(2)
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            if (name.isNotBlank()) {
                return WhatsAppContact(
                    contactName = name,
                    message = message
                )
            }
        }

        // Variante: "Escribe a mi Mami por WhatsApp".
        val whatsappAlternative = Regex(
            """^(?:escribe|escribeme|mandale|enviale)\s+(?:a|para)\s+(.+?)\s+por\s+whatsapp(?:\s+(?:diciendo|que diga)\s+(.+))?$"""
        ).find(text)

        if (whatsappAlternative != null) {
            val name = cleanName(
                whatsappAlternative.groupValues[1]
            )

            val message = whatsappAlternative.groupValues
                .getOrNull(2)
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            if (name.isNotBlank()) {
                return WhatsAppContact(name, message)
            }
        }

        // Consultar el número de un contacto.
        val phoneQuery = Regex(
            """^(?:cual es el numero de|cual es el telefono de|dime el numero de|dime el telefono de|que numero tiene|que telefono tiene|numero de|telefono de)\s+(.+)$"""
        ).find(text)

        if (phoneQuery != null) {
            val name = cleanName(
                phoneQuery.groupValues[1]
            )

            if (name.isNotBlank()) {
                return GetContactNumber(name)
            }
        }

        // Buscar personas.
        val findWithA = Regex(
            """^(?:busca|encuentra|localiza)\s+a\s+(.+?)(?:\s+en mis contactos)?$"""
        ).find(text)

        val findInContacts = Regex(
            """^(?:busca|encuentra|localiza)\s+(.+?)\s+(?:en|de)\s+mis contactos$"""
        ).find(text)

        val findMatch = findWithA ?: findInContacts

        if (findMatch != null) {
            val name = cleanName(
                findMatch.groupValues[1]
            )

            if (name.isNotBlank()) {
                return FindContact(name)
            }
        }

        // Llamadas directas.
        // Se conserva el comportamiento que ya funciona.
        val call = Regex(
            """^(?:llama|llamar|haz una llamada|realiza una llamada)\s+(?:a|al)\s+(.+)$"""
        ).find(text)

        if (call != null) {
            val name = cleanName(
                call.groupValues[1]
            )

            val isPhoneNumber =
                name.matches(
                    Regex("""\+?[\d\s()-]{7,20}""")
                )

            if (name.isNotBlank() && !isPhoneNumber) {
                return CallContact(name)
            }
        }

        return null
    }
}
