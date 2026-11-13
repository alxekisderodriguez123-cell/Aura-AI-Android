package com.aura.ai.actions

import android.content.Context
import android.util.Log
import com.aura.ai.media.AuraLocalMusicActions

class AuraActionEngine(context: Context) {

    private val app =
        AuraAppActions(context.applicationContext)

    private val clock =
        AuraClockActions(context.applicationContext)

    private val maps =
        AuraMapsActions(context.applicationContext)

    private val media =
        AuraMediaActions(context.applicationContext)

    private val localMusic =
        AuraLocalMusicActions(context.applicationContext)

    private val device =
        AuraDeviceActions(context.applicationContext)

    private val contacts =
        AuraContactsActions(context.applicationContext)

    private var pendingWhatsAppNumber: String? = null

    fun hasPendingWhatsAppMessage(): Boolean {
        return pendingWhatsAppNumber != null
    }

    fun completePendingWhatsAppMessage(
        message: String
    ): String {

        val number = pendingWhatsAppNumber
            ?: return "No tengo ningún mensaje de WhatsApp pendiente."

        pendingWhatsAppNumber = null

        return contacts.prepareWhatsApp(
            number = number,
            message = message
        )
    }

    fun execute(command: AuraCommand): String {
        return try {

            when (command) {

                is AuraCommand.OpenApp ->
                    app.open(command.name)

                is AuraCommand.StartTimer ->
                    clock.startTimer(command.seconds)

                is AuraCommand.SetAlarm ->
                    clock.setAlarm(
                        command.hour,
                        command.minute
                    )

                is AuraCommand.Navigate ->
                    maps.navigate(command.destination)

                is AuraCommand.SearchMaps ->
                    maps.search(command.query)

                is AuraCommand.SearchYouTube ->
                    media.youtube(
                        command.query,
                        command.play
                    )

                is AuraCommand.SearchYouTubeMusic ->
                    localMusic.playLocalOrFallback(
                        command.query,
                        command.play
                    )

                is AuraCommand.SearchSpotify ->
                    media.spotify(
                        command.query,
                        command.play
                    )

                is AuraCommand.SearchGoogle ->
                    media.google(command.query)

                is AuraCommand.Volume ->
                    device.volume(command.operation)

                is AuraCommand.Brightness ->
                    device.brightness(command.operation)

                AuraCommand.Wifi ->
                    device.wifi()

                AuraCommand.Bluetooth ->
                    device.bluetooth()

                AuraCommand.Internet ->
                    device.internet()

                AuraCommand.OpenSettings ->
                    device.settings()

                is AuraCommand.Dial ->
                    app.dial(command.number)

                is AuraCommand.Sms ->
                    app.sms(command.number)

                // Contactos.
                is FindContact ->
                    contacts.describeContact(
                        command.contactName
                    )

                is GetContactNumber ->
                    contacts.getContactNumber(
                        command.contactName
                    )

                // Llamadas directas: comportamiento conservado.
                is CallContact ->
                    contacts.callContact(
                        command.contactName
                    )

                // WhatsApp.
                is WhatsAppContact -> {

                    val contact = contacts.findContact(
                        command.contactName
                    ) ?: return "No encontré a ${command.contactName} en tus contactos."

                    val message = command.message

                    if (!message.isNullOrBlank()) {

                        contacts.prepareWhatsApp(
                            number = contact.number,
                            message = message
                        )

                    } else {

                        pendingWhatsAppNumber =
                            contact.number

                        "Encontré a ${contact.name}. ¿Qué mensaje quieres enviarle?"
                    }
                }
            }

        } catch (error: SecurityException) {

            Log.e(
                "AuraActions",
                "Permiso denegado",
                error
            )

            error.message
                ?: "Android bloqueó esta acción por falta de permisos."

        } catch (error: Exception) {

            Log.e(
                "AuraActions",
                "Error ejecutando comando",
                error
            )

            "No pude completar la acción: ${
                error.message ?: "error desconocido"
            }"
        }
    }
}
