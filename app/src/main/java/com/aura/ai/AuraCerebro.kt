
package com.aura.ai

import android.content.Context
import android.util.Log
import com.aura.ai.chat.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AuraCerebro(context: Context) {

    companion object {
        private const val TAG = "AuraCerebro"
        private const val TIMEOUT_MS = 45000L
        private const val MAX_HISTORY = 16

        private const val API_URL =
            "https://floral-flower-7178.alxekisderodriguez123.workers.dev/v1/chat"
    }

    private val appContext = context.applicationContext

    @Volatile
    var ultimoDetalle: String = ""
        private set

    suspend fun responder(
        pregunta: String,
        historialLocal: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {

        ultimoDetalle = ""

        try {
            withTimeout(TIMEOUT_MS) {

                val history = JSONArray()

                historialLocal
                    .takeLast(MAX_HISTORY)
                    .filter { it.text.isNotBlank() }
                    .forEach { message ->
                        history.put(
                            JSONObject().apply {
                                put(
                                    "role",
                                    if (message.isUser) "user" else "model"
                                )
                                put("text", message.text)
                            }
                        )
                    }

                val payload = JSONObject().apply {
                    put("question", pregunta.trim())
                    put("history", history)
                }

                val connection =
                    (URL(API_URL).openConnection() as HttpURLConnection)

                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 40000
                    connection.doOutput = true

                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=utf-8"
                    )

                    connection.setRequestProperty(
                        "Accept",
                        "application/json"
                    )

                    connection.outputStream.use { output ->
                        output.write(
                            payload.toString().toByteArray(Charsets.UTF_8)
                        )
                        output.flush()
                    }

                    val status = connection.responseCode

                    val responseStream =
                        if (status in 200..299) {
                            connection.inputStream
                        } else {
                            connection.errorStream
                        }

                    val responseText = responseStream
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()

                    val response = try {
                        JSONObject(responseText)
                    } catch (error: Exception) {
                        throw IOException(
                            "El servidor devolvió una respuesta no válida."
                        )
                    }

                    if (status !in 200..299) {
                        val serverError = response.optString(
                            "error",
                            "Error del servidor."
                        )

                        ultimoDetalle = "HTTP $status: $serverError"

                        throw IOException(
                            errorMessage(status, serverError)
                        )
                    }

                    val answer = response.optString("text", "").trim()

                    if (answer.isBlank()) {
                        throw IOException(
                            "El servidor no devolvió una respuesta."
                        )
                    }

                    val model = response.optString("model", "Gemini")
                    val search = response.optBoolean("search", false)

                    ultimoDetalle = model +
                        if (search) {
                            " · Google Search"
                        } else {
                            " · Sin búsqueda web"
                        }

                    answer

                } finally {
                    connection.disconnect()
                }
            }

        } catch (error: TimeoutCancellationException) {
            ultimoDetalle = "Tiempo de espera agotado"
            "La respuesta está tardando demasiado. Inténtalo otra vez."

        } catch (error: Exception) {
            Log.e(TAG, "Error conectando con Aura Cloud", error)

            if (ultimoDetalle.isBlank()) {
                ultimoDetalle =
                    "${error.javaClass.simpleName}: ${error.message.orEmpty()}"
            }

            error.message
                ?.takeIf { it.isNotBlank() }
                ?: "No pude conectar con mi motor de inteligencia."
        }
    }

    private fun errorMessage(
        status: Int,
        serverError: String
    ): String {
        return when (status) {
            400 -> "La solicitud enviada al servidor no es válida."
            413 -> "La conversación es demasiado extensa."
            429 -> "Se alcanzó el límite de solicitudes. Espera un momento."
            500 -> "El servidor de Aura tiene un problema de configuración."
            502 -> {
                if (serverError.contains("Gemini", ignoreCase = true)) {
                    "No pude obtener una respuesta de Gemini. Inténtalo más tarde."
                } else {
                    "El servidor de Aura no pudo completar la solicitud."
                }
            }
            else -> "Error de conexión con Aura Cloud (HTTP $status)."
        }
    }
}
