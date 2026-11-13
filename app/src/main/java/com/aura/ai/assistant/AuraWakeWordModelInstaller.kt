package com.aura.ai.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream

object AuraWakeWordModelInstaller {

    private const val TAG = "AuraModelInstaller"

    private const val MODEL_URL =
        "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"

    private const val EXPECTED_SHA256 =
        "09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f"

    private const val MAX_ARCHIVE_BYTES =
        80L * 1024L * 1024L

    private const val MAX_EXTRACTED_BYTES =
        300L * 1024L * 1024L

    private val executor =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    fun ensureModel(
        context: Context,
        callback: (File?) -> Unit
    ) {
        val appContext = context.applicationContext

        executor.execute {
            var result: File? = null

            try {
                val destination = File(
                    appContext.filesDir,
                    "vosk-model-es"
                )

                if (isValidModel(destination)) {
                    result = destination
                } else {
                    destination.deleteRecursively()
                    result = installModel(
                        appContext,
                        destination
                    )
                }

            } catch (error: Exception) {
                Log.e(
                    TAG,
                    "No se pudo instalar el modelo",
                    error
                )
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

            downloadWithRetries(partialFile)

            val actualHash = sha256(partialFile)

            if (!actualHash.equals(EXPECTED_SHA256, true)) {
                throw SecurityException(
                    "La verificación SHA-256 del modelo no coincide."
                )
            }

            if (!partialFile.renameTo(zipFile)) {
                throw IllegalStateException(
                    "No se pudo preparar el archivo ZIP."
                )
            }

            if (!stagingDirectory.mkdirs() &&
                !stagingDirectory.isDirectory
            ) {
                throw IllegalStateException(
                    "No se pudo crear la carpeta temporal."
                )
            }

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
                destination.deleteRecursively()

                throw IllegalStateException(
                    "La instalación final no es válida."
                )
            }

            Log.i(
                TAG,
                "Modelo español instalado y verificado."
            )

            return destination

        } finally {
            zipFile.delete()
            partialFile.delete()

            if (!isValidModel(destination)) {
                stagingDirectory.deleteRecursively()
            }
        }
    }

    private fun downloadWithRetries(
        destination: File
    ) {
        var lastError: Exception? = null

        for (attempt in 1..3) {
            destination.delete()

            try {
                downloadModel(destination)

                if (destination.length() < 10L * 1024L * 1024L) {
                    throw IllegalStateException(
                        "El archivo descargado es demasiado pequeño."
                    )
                }

                return

            } catch (error: Exception) {
                lastError = error

                Log.w(
                    TAG,
                    "Falló la descarga. Intento $attempt de 3.",
                    error
                )

                destination.delete()

                if (attempt < 3) {
                    Thread.sleep(attempt * 1500L)
                }
            }
        }

        throw IllegalStateException(
            "No se pudo descargar el modelo después de tres intentos.",
            lastError
        )
    }

    private fun downloadModel(destination: File) {
        val connection = URL(MODEL_URL)
            .openConnection() as HttpURLConnection

        connection.instanceFollowRedirects = true
        connection.connectTimeout = 30000
        connection.readTimeout = 120000
        connection.requestMethod = "GET"

        connection.setRequestProperty(
            "User-Agent",
            "Aura-Android/1.0"
        )

        try {
            connection.connect()

            if (
                connection.responseCode !=
                HttpURLConnection.HTTP_OK
            ) {
                throw IllegalStateException(
                    "Error HTTP ${connection.responseCode}"
                )
            }

            val contentLength =
                connection.contentLengthLong

            if (contentLength > MAX_ARCHIVE_BYTES) {
                throw IllegalStateException(
                    "El ZIP supera el límite permitido."
                )
            }

            var totalBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->

                    val buffer = ByteArray(16384)

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

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")

        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(16384)

            while (true) {
                val count = input.read(buffer)

                if (count == -1) break

                digest.update(buffer, 0, count)
            }
        }

        return digest.digest().joinToString("") {
            "%02x".format(it)
        }
    }

    private fun extractModel(
        zipFile: File,
        destination: File
    ) {
        val root = destination.canonicalFile
        val rootPrefix = root.path + File.separator

        var extractedBytes = 0L

        ZipInputStream(
            zipFile.inputStream().buffered()
        ).use { zip ->

            var entry = zip.nextEntry

            while (entry != null) {

                val entryName = entry.name
                    .replace('\\', '/')

                val separator = entryName.indexOf('/')

                if (
                    separator >= 0 &&
                    separator < entryName.length - 1
                ) {
                    val relativePath =
                        entryName.substring(separator + 1)

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

                                val buffer = ByteArray(16384)

                                while (true) {
                                    val count = zip.read(buffer)

                                    if (count == -1) break

                                    extractedBytes += count

                                    if (
                                        extractedBytes >
                                        MAX_EXTRACTED_BYTES
                                    ) {
                                        throw IllegalStateException(
                                            "El modelo extraído supera el límite."
                                        )
                                    }

                                    output.write(
                                        buffer,
                                        0,
                                        count
                                    )
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

    private fun isValidModel(directory: File): Boolean {
        return File(
            directory,
            "am/final.mdl"
        ).isFile &&
            File(
                directory,
                "conf/mfcc.conf"
            ).isFile &&
            File(
                directory,
                "graph/words.txt"
            ).isFile
    }
}
