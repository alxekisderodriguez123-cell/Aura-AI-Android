package com.aura.ai.actions

import android.content.Context
import android.util.Log

class AuraActionEngine(context: Context) {

    private val app = AuraAppActions(context.applicationContext)
    private val clock = AuraClockActions(context.applicationContext)
    private val maps = AuraMapsActions(context.applicationContext)
    private val media = AuraMediaActions(context.applicationContext)
    private val device = AuraDeviceActions(context.applicationContext)
    private val appCheckToken = AuraAppCheckToken(context.applicationContext)

    fun execute(command: AuraCommand): String {

        return try {
            when (command) {

                is AuraCommand.OpenApp ->
                    app.open(command.name)

                is AuraCommand.StartTimer ->
                    clock.startTimer(command.seconds)

                is AuraCommand.SetAlarm ->
                    clock.setAlarm(command.hour, command.minute)

                is AuraCommand.Navigate ->
                    maps.navigate(command.destination)

                is AuraCommand.SearchMaps ->
                    maps.search(command.query)

                is AuraCommand.SearchYouTube ->
                    media.youtube(command.query, command.play)

                is AuraCommand.SearchYouTubeMusic ->
                    media.youtubeMusic(command.query, command.play)

                is AuraCommand.SearchSpotify ->
                    media.spotify(command.query, command.play)

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

                AuraCommand.CopyAppCheckToken ->
                    "Estoy solicitando el token de App Check."

                is AuraCommand.Dial ->
                    app.dial(command.number)

                is AuraCommand.Sms ->
                    app.sms(command.number)
            }

        } catch (e: SecurityException) {
            Log.e("AuraActions", "Permiso denegado", e)
            "Android bloqueó esta acción por falta de permisos."

        } catch (e: Exception) {
            Log.e("AuraActions", "Error ejecutando comando", e)
            "No pude completar la acción: ${e.message ?: "error desconocido"}"
        }
    }

    fun copyAppCheckToken(onResult: (String) -> Unit) {
        appCheckToken.copyDebugToken(onResult)
    }
}
