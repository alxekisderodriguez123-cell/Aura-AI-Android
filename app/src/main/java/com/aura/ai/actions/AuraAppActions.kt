
package com.aura.ai.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.Normalizer
import java.util.Locale

class AuraAppActions(private val context: Context) {

    fun open(name: String): String {
        require(name.isNotBlank())

        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = pm.queryIntentActivities(launcher, 0)
        val query = normalize(name)

        val selected = apps.firstOrNull {
            normalize(it.loadLabel(pm).toString()) == query
        } ?: apps.filter {
            normalize(it.loadLabel(pm).toString()).contains(query)
        }.minByOrNull {
            it.loadLabel(pm).length
        } ?: return "No encontré la aplicación $name."

        val packageName = selected.activityInfo.packageName
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
            ?: return "Encontré $name, pero no pude abrirla."

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        return "Abriendo ${selected.loadLabel(pm)}."
    }

    fun dial(number: String): String {
        val intent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:${Uri.encode(number)}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
        return "Abriendo el marcador con el número $number."
    }

    fun sms(number: String): String {
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:${Uri.encode(number)}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
        return "Abriendo mensajes para el número $number."
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{Mn}+"), "")
            .trim()
}
