package com.aura.ai.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import java.io.File

class AuraAppCheckToken(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "AuraAppCheckToken"

        private val UUID_PATTERN = Regex(
            "[0-9a-fA-F]{8}-" +
                "[0-9a-fA-F]{4}-" +
                "[0-9a-fA-F]{4}-" +
                "[0-9a-fA-F]{4}-" +
                "[0-9a-fA-F]{12}"
        )
    }

    fun copyDebugToken(onResult: (String) -> Unit) {

        try {
            FirebaseAppCheck.getInstance()
                .getAppCheckToken(false)
                .addOnCompleteListener { task ->

                    val debugToken = findDebugToken()

                    if (debugToken.isNullOrBlank()) {
                        Log.w(
                            TAG,
                            "No se encontró el secreto de depuración."
                        )

                        onResult(
                            "No encontré el secreto de App Check. " +
                                "Comprueba que Firebase App Check Debug esté inicializado " +
                                "y revisa el registro de depuración."
                        )
                        return@addOnCompleteListener
                    }

                    try {
                        val clipboard = appContext.getSystemService(
                            Context.CLIPBOARD_SERVICE
                        ) as ClipboardManager

                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "Aura App Check Debug Token",
                                debugToken
                            )
                        )

                        onResult(
                            "Token de depuración copiado. " +
                                "Ahora puedes pegarlo en la sección de App Check de Firebase."
                        )

                    } catch (e: Exception) {
                        Log.e(TAG, "Error copiando token", e)
                        onResult("No pude copiar el token al portapapeles.")
                    }
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error solicitando App Check", e)
            onResult(
                "No pude solicitar el token de App Check: " +
                    (e.message ?: "error desconocido")
            )
        }
    }

    private fun findDebugToken(): String? {
        val directory = File(
            appContext.applicationInfo.dataDir,
            "shared_prefs"
        )

        val files = directory.listFiles()
            ?: return null

        for (file in files) {
            if (!file.isFile || !file.name.endsWith(".xml")) {
                continue
            }

            try {
                val content = file.readText()

                val match = UUID_PATTERN.find(content)

                if (match != null) {
                    return match.value
                }

            } catch (e: Exception) {
                Log.w(TAG, "No se pudo leer ${file.name}", e)
            }
        }

        return null
    }
}
