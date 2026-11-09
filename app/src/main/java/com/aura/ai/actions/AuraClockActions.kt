
package com.aura.ai.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.util.Locale

class AuraClockActions(private val context: Context) {

    fun startTimer(seconds: Int): String {
        require(seconds in 1..86400)

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Temporizador de Aura")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remaining = seconds % 60

        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("$hours ${if (hours == 1) "hora" else "horas"}")
        if (minutes > 0) parts.add("$minutes ${if (minutes == 1) "minuto" else "minutos"}")
        if (remaining > 0) parts.add("$remaining ${if (remaining == 1) "segundo" else "segundos"}")

        return "Temporizador iniciado: ${parts.joinToString(" y ")}."
    }

    fun setAlarm(hour: Int, minute: Int): String {
        require(hour in 0..23 && minute in 0..59)

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Alarma de Aura")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)

        val time = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            hour,
            minute
        )

        return "He enviado al reloj la orden para configurar la alarma a las $time."
    }
}
