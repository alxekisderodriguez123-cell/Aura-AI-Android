
package com.aura.ai.actions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import java.text.Normalizer
import java.util.Locale

class AuraActionEngine(
    private val context: Context
) {

    fun execute(command: AuraCommand): String {
        return try {
            when (command) {
                is AuraCommand.OpenApp -> openApp(command.name)

                is AuraCommand.StartTimer -> startTimer(command.seconds)

                is AuraCommand.SetAlarm -> setAlarm(
                    command.hour,
                    command.minute
                )

                AuraCommand.OpenWifiPanel -> openPanel(
                    Settings.Panel.ACTION_WIFI,
                    "Abriendo los controles de Wi-Fi."
                )

                AuraCommand.OpenBluetoothPanel -> openPanel(
                    Settings.Panel.ACTION_BLUETOOTH,
                    "Abriendo los controles de Bluetooth."
                )

                AuraCommand.OpenInternetPanel -> openPanel(
                    Settings.Panel.ACTION_INTERNET_CONNECTIVITY,
                    "Abriendo los controles de conexión e Internet."
                )

                AuraCommand.VolumeUp -> changeVolume(
                    AudioManager.ADJUST_RAISE,
                    "He aumentado el volumen multimedia."
                )

                AuraCommand.VolumeDown -> changeVolume(
                    AudioManager.ADJUST_LOWER,
                    "He reducido el volumen multimedia."
                )

                AuraCommand.MuteVolume -> changeVolume(
                    AudioManager.ADJUST_MUTE,
                    "He silenciado el volumen multimedia."
                )

                AuraCommand.OpenDisplaySettings -> openSettings(
                    Settings.ACTION_DISPLAY_SETTINGS,
                    "Abriendo los ajustes de pantalla para controlar el brillo."
                )

                AuraCommand.OpenGeneralSettings -> openSettings(
                    Settings.ACTION_SETTINGS,
                    "Abriendo los ajustes del teléfono."
                )

                is AuraCommand.OpenMaps -> openMaps(command.place)

                is AuraCommand.DialNumber -> openDialer(command.number)

                is AuraCommand.ComposeSms -> openSms(command.number)
            }
        } catch (exception: SecurityException) {
            "Android no concedió permiso para realizar esa acción."
        } catch (exception: ActivityNotFoundException) {
            "No encontré una aplicación compatible para realizar esa acción."
        } catch (exception: Exception) {
            "No pude completar la acción: ${exception.message ?: "error desconocido"}"
        }
    }

    private fun openApp(query: String): String {
        val packageManager = context.packageManager

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val matches = packageManager.queryIntentActivities(
            launcherIntent,
            0
        )

        val normalizedQuery = normalize(query)

        val app = matches.firstOrNull { info ->
            normalize(
                info.loadLabel(packageManager).toString()
            ).contains(normalizedQuery)
        } ?: return "No encontré una aplicación llamada \"$query\"."

        val packageName = app.activityInfo.packageName
        val launchIntent = packageManager.getLaunchIntentForPackage(
            packageName
        ) ?: return "Encontré la aplicación, pero Android no permite abrirla."

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        return "Abriendo ${app.loadLabel(packageManager)}."
    }

    private fun startTimer(seconds: Int): String {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Temporizador de Aura")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)

        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        val duration = when {
            minutes > 0 && remainingSeconds > 0 ->
                "$minutes minutos y $remainingSeconds segundos"
            minutes > 0 ->
                "$minutes minutos"
            else ->
                "$remainingSeconds segundos"
        }

        return "Abrí el reloj con un temporizador de $duration. Confirma el inicio en la aplicación."
    }

    private fun setAlarm(hour: Int, minute: Int): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Alarma de Aura")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)

        val time = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            hour,
            minute
        )

        return "Abrí el reloj para configurar la alarma de las $time. Confirma y guarda la alarma."
    }

    private fun changeVolume(
        direction: Int,
        successMessage: String
    ): String {
        val audioManager = context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )

        return successMessage
    }

    private fun openPanel(
        action: String,
        successMessage: String
    ): String {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return successMessage
    }

    private fun openSettings(
        action: String,
        successMessage: String
    ): String {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return successMessage
    }

    private fun openMaps(place: String): String {
        val uri = if (place.isBlank()) {
            Uri.parse("geo:0,0")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(place)}")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)

        return if (place.isBlank()) {
            "Abriendo mapas."
        } else {
            "Buscando $place en mapas."
        }
    }

    private fun openDialer(number: String): String {
        val intent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:$number")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return "Abrí el marcador con el número $number. Revisa el número antes de llamar."
    }

    private fun openSms(number: String): String {
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:$number")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return "Abrí la aplicación de mensajes para escribir al número $number. El mensaje no se enviará sin que lo confirmes."
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{Mn}+"), "")
            .trim()
    }
}
