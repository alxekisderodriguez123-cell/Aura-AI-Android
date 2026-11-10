
package com.aura.ai

import android.content.Context
import android.util.Log
import com.aura.ai.chat.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private data class AuraAIConfig(
    val model: String,
    val googleSearch: Boolean
)

class AuraCerebro(context: Context) {

    companion object {
        private const val TAG = "AuraAI"
        private const val TIMEOUT_MS = 30000L
        private const val MAX_HISTORY = 16

        private val CONFIGS = listOf(
            AuraAIConfig("gemini-3.5-flash-lite", true),
            AuraAIConfig("gemini-3.5-flash-lite", false),
            AuraAIConfig("gemini-3.1-flash-lite", false)
        )
    }

    private val appContext = context.applicationContext

    @Volatile
    var ultimoDetalle: String = ""
        private set

    suspend fun responder(
        pregunta: String,
        historialLocal: List<ChatMessage>
    ): String = withContext(Dispatchers.Default) {

        ultimoDetalle = ""

        try {
            withTimeout(TIMEOUT_MS) {

                val history = historialLocal
                    .takeLast(MAX_HISTORY)
                    .filter { it.text.isNotBlank() }
                    .map { message ->
                        content(
                            if (message.isUser) "user" else "model"
                        ) {
                            text(message.text)
                        }
                    }

                var ultimoError: Exception? = null

                for (config in CONFIGS) {
                    try {
                        val model = crearModelo(config)
                        val chat = model.startChat(history)
                        val result = chat.sendMessage(pregunta)

                        val answer = limpiar(
                            result.text.orEmpty()
                        )

                        ultimoDetalle = config.model +
                            if (config.googleSearch) {
                                " · Google Search"
                            } else {
                                " · Sin búsqueda web"
                            }

                        return@withTimeout if (answer.isBlank()) {
                            "No encontré una respuesta para eso."
                        } else {
                            answer
                        }

                    } catch (e: CancellationException) {
                        throw e

                    } catch (e: Exception) {
                        Log.w(
                            TAG,
                            "Falló ${config.model}, Search=${config.googleSearch}",
                            e
                        )

                        ultimoError = e

                        if (!esRecuperable(e, config)) {
                            throw e
                        }
                    }
                }

                throw ultimoError
                    ?: IllegalStateException("No hay modelos disponibles")
            }

        } catch (e: TimeoutCancellationException) {
            ultimoDetalle = "Tiempo de espera agotado"
            "La respuesta está tardando demasiado. Inténtalo otra vez."

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Log.e(TAG, "Error de inteligencia", e)

            ultimoDetalle =
                "${e.javaClass.simpleName}: ${e.message.orEmpty().take(160)}"

            mensajeError(e)
        }
    }

    private fun crearModelo(
        config: AuraAIConfig
    ) = Firebase.ai(
        backend = GenerativeBackend.googleAI()
    ).generativeModel(
        modelName = config.model,

        generationConfig = generationConfig {
            maxOutputTokens = 600
            temperature = 0.7f
        },

        tools = if (config.googleSearch) {
            listOf(Tool.googleSearch())
        } else {
            null
        },

        systemInstruction = content {
            text(instrucciones(config.googleSearch))
        }
    )

    private fun instrucciones(
        conBusqueda: Boolean
    ): String {

        val internet = if (conBusqueda) {
            """
            Puedes utilizar Google Search para consultar información actual.
            Busca información cuando sea necesario y distingue los datos
            confirmados de las estimaciones.
            """.trimIndent()
        } else {
            """
            No tienes búsqueda web disponible en este momento.
            No inventes información actual ni afirmes haber consultado Internet.
            """.trimIndent()
        }

        return """
            Eres Aura, una asistente personal inteligente de Android.

            PERSONALIDAD:
            - Hablas en español natural y claro.
            - Eres inteligente, conversacional y directa.
            - No respondes como un robot ni repites frases innecesarias.
            - Puedes mantener el contexto de la conversación.
            - Recuerdas los mensajes anteriores que recibes en el historial.
            - No inventas recuerdos ni datos personales.

            RESPUESTAS:
            - Responde de forma natural y útil.
            - Adapta la extensión a la pregunta.
            - No uses Markdown excesivo.
            - No repitas la pregunta del usuario.
            - Si no sabes algo, dilo claramente.

            INTERNET:
            $internet

            ACCIONES DEL TELÉFONO:
            - Las acciones locales son ejecutadas por el motor de Aura.
            - No afirmes que has abierto aplicaciones, activado ajustes,
              creado alarmas o reproducido música si no se te ha informado
              que la acción se ejecutó.
            - Si una acción no está disponible, explica la limitación.

            IDENTIDAD:
            Tu nombre es Aura.
            Eres el asistente personal de este dispositivo Android.
        """.trimIndent()
    }

    private fun esRecuperable(
        exception: Exception,
        config: AuraAIConfig
    ): Boolean {

        val error = textoError(exception)

        if (
            "app check" in error ||
            "appcheck" in error ||
            "app_check" in error ||
            "unknownhost" in error ||
            "unable to resolve" in error ||
            "network" in error ||
            "timeout" in error ||
            "ioexception" in error
        ) {
            return false
        }

        val cuota =
            "quota" in error ||
            "429" in error ||
            "exhausted" in error

        return if (cuota) {
            config.googleSearch
        } else {
            true
        }
    }

    private fun mensajeError(exception: Exception): String {

        val error = textoError(exception)

        return when {
            "app check" in error ||
            "appcheck" in error ||
            "app_check" in error ->
                "Necesito que registres el token de App Check en Firebase."

            "quota" in error ||
            "429" in error ||
            "exhausted" in error ->
                "Alcancé el límite gratuito de solicitudes. Inténtalo más tarde."

            "404" in error ||
            "not found" in error ||
            "no longer available" in error ->
                "El modelo de inteligencia no está disponible."

            "403" in error ||
            "permission" in error ||
            "servicedisabled" in error ||
            "disabled" in error ->
                "Firebase rechazó la solicitud. Revisa la configuración de Aura."

            "apikey" in error ||
            "api key" in error ||
            "api_key" in error ->
                "Firebase rechazó la configuración de la clave de acceso."

            "location" in error ->
                "El servicio de inteligencia no está disponible en esta ubicación."

            "unknownhost" in error ||
            "unable to resolve" in error ||
            "network" in error ||
            "connect" in error ||
            "ioexception" in error ->
                "No tengo conexión a Internet en este momento."

            else ->
                "No pude conectar con mi motor de inteligencia."
        }
    }

    private fun textoError(exception: Exception): String =
        "${exception.javaClass.simpleName} ${exception.message.orEmpty()}"
            .lowercase()

    private fun limpiar(text: String): String =
        text
            .replace(Regex("(?m)^\\s*[-•]\\s+"), "")
            .replace(Regex("[*_`#>~]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
