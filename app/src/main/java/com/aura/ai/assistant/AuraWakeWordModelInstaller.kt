
package com.aura.ai.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream

object AuraWakeWordModelInstaller {

    private const val TAG = "AuraModelInstaller"

    private const val MODEL_URL =
        "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"

    private const val MAX_ARCHIVE_BYTES = 150L * 1024L * 1024L
    private const val MAX_EXTRACTED_BYTES = 300L * 1024L * 1024L

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun ensureModel(
        context: Context,
        callback: (File?) -> Unit
    ) {
        val appContext = context.applicationContext

        executor.execute {
            var result: File? = null

            try {
                val modelDirectory = File(
                    appContext.filesDir,
                    "vosk-model-es"
                )

                if (isValidModel(modelDirectory)) {
                    result = modelDirectory
                } else {
                    result = installModel(appContext, modelDirectory)
                }

            } catch (error: Exception) {
                Log.e(TAG, "Error instalando modelo", error)
            }

            mainHandler.post {
                callback(result)
            }
        }
    }

    private fun installModel(
        context: Context,
        destination: File
    ): File {
        val zipFile = File(
            context.cacheDir,
            "vosk-spanish-model.zip"
        )

        val partialFile = File(
            context.cacheDir,
            "vosk-spanish-model.zip.part"
        )

        val stagingDirectory = File(
            context.filesDir,
            "vosk-model-es-installing"
        )

        try {
            zipFile.delete()
            partialFile.delete()
            stagingDirectory.deleteRecursively()

            downloadModel(
                partialFile
            )

            if (!partialFile.renameTo(zipFile)) {
                throw IllegalStateException(
                    "No se pudo preparar el archivo ZIP."
                )
            }

            stagingDirectory.mkdirs()

            extractModel(
                zipFile,
                stagingDirectory
            )

            if (!isValidModel(stagingDirectory)) {
                throw IllegalStateException(
                    "El modelo extraído está incompleto."
                )
            }

            destination.deleteRecursively()

            if (!stagingDirectory.renameTo(destination)) {
                throw IllegalStateException(
                    "No se pudo instalar el modelo."
                )
            }

            if (!isValidModel(destination)) {
                throw IllegalStateException(
                    "La instalación final no es válida."
                )
            }

            Log.i(TAG, "Modelo español instalado correctamente")

            return destination

        } finally {
            zipFile.delete()
            partialFile.delete()

            if (!isValidModel(destination)) {
                stagingDirectory.deleteRecursively()
            }
        }
    }

    private fun isValidModel(directory: File): Boolean {
        return File(directory, "am/final.mdl").isFile &&
            File(directory, "conf/mfcc.conf").isFile &&
            File(directory, "graph/words.txt").isFile
    }

    private fun downloadModel(destination: File) {
        val connection = URL(MODEL_URL)
            .openConnection() as HttpURLConnection

        connection.connectTimeout = 20000
        connection.readTimeout = 60000
        connection.requestMethod = "GET"
        connection.setRequestProperty(
            "User-Agent",
            "Aura-Android"
        )

        try {
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException(
                    "Error HTTP ${connection.responseCode}"
                )
            }

            val contentLength = connection.contentLengthLong

            if (
                contentLength > MAX_ARCHIVE_BYTES
            ) {
                throw IllegalStateException(
                    "El archivo del modelo supera el límite permitido."
                )
            }

            var totalBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)

                    while (true) {
                        val count = input.read(buffer)

                        if (count == -1) break

                        totalBytes += count

                        if (totalBytes > MAX_ARCHIVE_BYTES) {
                            throw IllegalStateException(
                                "La descarga superó el límite permitido."
                            )
                        }

                        output.write(buffer, 0, count)
                    }

                    output.flush()
                    output.fd.sync()
                }
            }

            if (totalBytes <= 0L) {
                throw IllegalStateException(
                    "El archivo descargado está vacío."
                )
            }

        } finally {
            connection.disconnect()
        }
    }

    private fun extractModel(
        zipFile: File,
        destination: File
    ) {
        val root = destination.canonicalFile
        val rootPrefix = root.path + File.separator

        var extractedBytes = 0L

        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->

            var entry = zip.nextEntry

            while (entry != null) {
                val separator = entry.name.indexOf('/')

                if (separator >= 0 && separator < entry.name.length - 1) {
                    val relativePath = entry.name.substring(separator + 1)

                    if (relativePath.isNotBlank()) {
                        val outputFile = File(
                            destination,
                            relativePath
                        ).canonicalFile

                        if (!outputFile.path.startsWith(rootPrefix)) {
                            throw SecurityException(
                                "Ruta no válida dentro del ZIP."
                            )
                        }

                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()

                            FileOutputStream(outputFile).use { output ->
                                val buffer = ByteArray(8192)

                                while (true) {
                                    val count = zip.read(buffer)

                                    if (count == -1) break

                                    extractedBytes += count

                                    if (
                                        extractedBytes > MAX_EXTRACTED_BYTES
                                    ) {
                                        throw IllegalStateException(
                                            "El modelo extraído supera el límite."
                                        )
                                    }

                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                    }
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
}
