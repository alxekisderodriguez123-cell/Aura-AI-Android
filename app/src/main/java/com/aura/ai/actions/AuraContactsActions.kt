package com.aura.ai.actions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import java.text.Normalizer
import java.util.Locale

class AuraContactsActions(
    private val context: Context
) {

    data class Contact(
        val name: String,
        val number: String
    )

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun findContact(name: String): Contact? {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            throw SecurityException(
                "Necesito permiso para consultar tus contactos. Abre Aura y concede el permiso de Contactos."
            )
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val contacts = mutableListOf<Contact>()

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->

            val nameIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

            val numberIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            while (cursor.moveToNext()) {
                val contactName = cursor.getString(nameIndex)
                    ?: continue

                val number = cursor.getString(numberIndex)
                    ?: continue

                contacts.add(
                    Contact(
                        name = contactName,
                        number = number
                    )
                )
            }
        }

        val query = normalize(name)

        return contacts.firstOrNull {
            normalize(it.name) == query
        } ?: contacts.firstOrNull {
            normalize(it.name).startsWith(query)
        } ?: contacts.firstOrNull {
            normalize(it.name).contains(query)
        }
    }

    fun callContact(name: String): String {
        val contact = findContact(name)
            ?: return "No encontré a $name en tus contactos."

        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            throw SecurityException(
                "Necesito permiso para realizar llamadas. Abre Aura y concede el permiso de Teléfono."
            )
        }

        val intent = Intent(
            Intent.ACTION_CALL,
            Uri.parse("tel:${Uri.encode(contact.number)}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)

        return "Llamando a ${contact.name}."
    }

    fun prepareWhatsApp(
        number: String,
        message: String
    ): String {
        if (message.isBlank()) {
            return "No recibí el mensaje que quieres enviar."
        }

        var internationalNumber = number
            .replace(Regex("[^0-9+]"), "")

        if (internationalNumber.startsWith("00")) {
            internationalNumber =
                "+" + internationalNumber.removePrefix("00")
        }

        if (!internationalNumber.startsWith("+")) {
            return "El número de ese contacto no tiene código internacional. Añádelo en Contactos para poder abrir WhatsApp correctamente."
        }

        val digits = internationalNumber.removePrefix("+")

        if (digits.length !in 8..15) {
            return "El número del contacto no parece válido para WhatsApp."
        }

        val uri = Uri.parse(
            "https://wa.me/$digits?text=${Uri.encode(message)}"
        )

        val whatsappIntent = Intent(
            Intent.ACTION_VIEW,
            uri
        )
            .setPackage("com.whatsapp")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(whatsappIntent)
            "Abrí WhatsApp con el mensaje preparado. Revísalo y pulsa Enviar cuando quieras."
        } catch (_: Exception) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                uri
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(browserIntent)

            "No encontré WhatsApp instalado. Abrí el enlace para continuar."
        }
    }
}
