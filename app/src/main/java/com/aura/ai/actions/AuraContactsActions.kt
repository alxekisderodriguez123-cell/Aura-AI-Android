
package com.aura.ai.actions

import android.Manifest
import android.content.ActivityNotFoundException
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
                "Necesito permiso de Contactos para buscar a esa persona."
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

            if (nameIndex < 0 || numberIndex < 0) {
                return@use
            }

            while (cursor.moveToNext()) {
                val contactName =
                    cursor.getString(nameIndex) ?: continue

                val number =
                    cursor.getString(numberIndex) ?: continue

                contacts.add(
                    Contact(
                        name = contactName,
                        number = number
                    )
                )
            }
        }

        val query = normalize(name)

        if (query.isBlank()) return null

        // Coincidencia exacta.
        contacts.firstOrNull {
            normalize(it.name) == query
        }?.let { return it }

        // Coincidencia por inicio del nombre.
        contacts.firstOrNull {
            normalize(it.name).startsWith(query)
        }?.let { return it }

        // Coincidencia parcial.
        return contacts.firstOrNull {
            normalize(it.name).contains(query)
        }
    }

    fun describeContact(name: String): String {
        val contact = findContact(name)
            ?: return "No encontré a $name en tus contactos."

        return "Encontré a ${contact.name} en tus contactos."
    }

    fun getContactNumber(name: String): String {
        val contact = findContact(name)
            ?: return "No encontré a $name en tus contactos."

        return "El número de ${contact.name} es ${contact.number}."
    }

    fun callContact(name: String): String {
        val contact = findContact(name)
            ?: return "No encontré a $name en tus contactos."

        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            throw SecurityException(
                "Necesito permiso de Teléfono para realizar llamadas."
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
            return "El número no tiene código internacional. Añádelo en Contactos para preparar el WhatsApp."
        }

        val digits = internationalNumber.removePrefix("+")

        if (digits.length !in 8..15) {
            return "El número de ese contacto no parece válido para WhatsApp."
        }

        val encodedMessage = Uri.encode(message)

        // Primero intentamos abrir WhatsApp directamente.
        val whatsappUri = Uri.parse(
            "whatsapp://send?phone=$digits&text=$encodedMessage"
        )

        val whatsappPackages = listOf(
            "com.whatsapp",
            "com.whatsapp.w4b"
        )

        for (packageName in whatsappPackages) {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    whatsappUri
                )
                    .setPackage(packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(intent)

                return "Abrí WhatsApp con el mensaje preparado para ${"el contacto"}. Revísalo y pulsa Enviar cuando quieras."

            } catch (_: ActivityNotFoundException) {
                // Probamos la siguiente variante.
            } catch (_: Exception) {
                // Probamos la siguiente variante.
            }
        }

        // Alternativa: enlace oficial de WhatsApp.
        val webUri = Uri.parse(
            "https://wa.me/$digits?text=$encodedMessage"
        )

        return try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                webUri
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)

            "Abrí el enlace de WhatsApp con el mensaje preparado. Revísalo y pulsa Enviar cuando quieras."

        } catch (error: Exception) {
            android.util.Log.e(
                "AuraContacts",
                "No se pudo abrir WhatsApp",
                error
            )

            "No pude abrir WhatsApp ni su enlace web. Comprueba que tengas conexión y que la aplicación esté instalada."
        }
    }
}
