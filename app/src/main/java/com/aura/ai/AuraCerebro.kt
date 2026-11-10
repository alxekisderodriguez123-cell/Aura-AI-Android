package com.aura.ai

import android.content.Context
import android.util.Log
import com.aura.ai.chat.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private data class AuraModelConfig(
    val name: String,
    val search: Boolean
)

class AuraCerebro(context: Context) {

    companion object {
        private const val TAG = "AuraCerebro"
        private const val TIMEOUT_MS = 35000L
        private const val MAX_HISTORY = 16

        private val MODELS = listOf(
            AuraModelConfig("gemini-3.5-flash-lite", true),
            AuraModelConfig("gemini-3.5-flash-lite", false),
            AuraModelConfig("gemini-3.1-flash-lite", false)
        )
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

                var lastError: Exception? = null

                for (config in MODELS) {
                    try {
                        val model = Firebase.ai(
                            backend = GenerativeBackend.googleAI()
                        ).generativeModel(
                            modelName = config.name,
                            generationConfig = generationConfig {
                                maxOutputTokens = 700
                                temperature = 0.7f
                            },
                            tools = if (config.search) {
                                listOf(Tool.googleSearch())
                            } else {
                                null
                            },
                            systemInstruction = content {
                                text(systemPrompt(config.search))
                            }
                        )

                        val chat = model.startChat(history)
                        val result = chat.sendMessage(pregunta)
                        val answer = clean(result.text.orEmpty())

                        ultimoDetalle = config.name +
                            if (config.search) {
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
                        lastError = e

                        Log.e(
                            TAG,
                            "Falló ${config.name}, búsqueda=${config.search}",
                            e
                        )

                        if (!canRetry(e)) {
                            throw e
                        }
                    }
                }

                throw lastError
                    ?: IllegalStateException("No hay modelos disponibles")
            }

        } catch (e: TimeoutCancellationException) {
            ultimoDetalle = "Tiempo de espera agotado"
            "La respuesta está tardando demasiado. Inténtalo otra vez."

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Log.e(TAG, "Error de Gemini", e)

            ultimoDetalle =
                "${e.javaClass.simpleName}: ${e.message.orEmpty().take(180)}"

            errorMessage(e)
        }
    }

    private fun systemPrompt(searchAvailable: Boolean): String {
        val internetInstructions = if (searchAvailable) {
            """
            Puedes consultar información actual mediante Google Search.
            Cuando la pregunta requiera información reciente, utiliza la búsqueda.
            Distingue los hechos confirmados de las estimaciones.
            """.trimIndent()
        } else {
            """
            No tienes búsqueda web disponible en esta solicitud.
            No afirmes haber consultado Internet.
            """.trimIndent()
        }

        return """
            Eres Aura, una asistente personal inteligente de Android.

            IDENTIDAD:
            - Tu nombre es Aura.
            - Eres la asistente personal del usuario.
            - Hablas español natural, claro y conversacional.

            COMPORTAMIENTO:
            - Responde directamente.
            - Mantén el contexto de los mensajes anteriores.
            - No repitas innecesariamente lo que dice el usuario.
            - No inventes recuerdos, datos ni resultados.
            - No respondas como un robot.
            - Adapta la extensión de la respuesta a la pregunta.

            ACCIONES:
            - Las acciones del teléfono son ejecutadas por el motor local de Aura.
            - No afirmes que abriste una aplicación, configuraste una alarma,
              iniciaste una ruta o reprodujiste música si el motor local
              no confirmó esa acción.
            - Si no puedes realizar una acción, explica la limitación.
            - No inventes permisos ni capacidades de Android.

            INTERNET:
            $internetInstructions
        """.trimIndent()
    }

    private fun canRetry(exception: Exception): Boolean {
        val error = exceptionText(exception)

        return !(
            "app check" in error ||
            "appcheck" in error ||
            "app_check" in error ||
            "unknownhost" in error ||
            "unable to resolve" in error ||
            "network" in error ||
            "ioexception" in error ||
            "timeout" in error
        )
    }

    private fun errorMessage(exception: Exception): String {
        val error = exceptionText(exception)

        return when {
            "app check" in error ||
            "appcheck" in error ||
            "app_check" in error ->
                "Necesito que registres el token de App Check de Aura en Firebase."

            "quota" in error ||
            "429" in error ||
            "exhausted" in error ->
                "Alcancé el límite gratuito de solicitudes. Puedes seguir usando mis comandos locales."

            "404" in error ||
            "not found" in error ->
                "El modelo de inteligencia no está disponible actualmente."

            "403" in error ||
            "permission" in error ||
            "servicedisabled" in error ->
                "Firebase rechazó la solicitud. Revisa la configuración del proyecto."

            "apikey" in error ||
            "api key" in error ->
                "Firebase rechazó la configuración de acceso."

            "unknownhost" in error ||
            "unable to resolve" in error ||
            "network" in error ||
            "connect" in error ||
            "ioexception" in error ->
                "No tengo conexión a Internet. Mis comandos locales siguen disponibles."

            else ->
                "No pude conectar con mi motor de inteligencia."
        }
    }

    private fun exceptionText(exception: Exception): String =
        "${exception.javaClass.simpleName} ${exception.message.orEmpty()}"
            .lowercase()

    private fun clean(value: String): String =
        value
            .replace(Regex("(?m)^\\s*[-•]\\s+"), "")
            .replace(Regex("[*_`#>~]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
