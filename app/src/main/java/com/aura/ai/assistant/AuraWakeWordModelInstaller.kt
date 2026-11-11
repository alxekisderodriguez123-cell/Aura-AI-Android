package com.aura.ai.assistant

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
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

    private val executor = Executors.newSingleThreadExecutor()

    fun ensureModel(
        context: Context,
        callback: (File?) -> Unit
    ) {
        val appContext = context.applicationContext

        executor.execute {
            try {
                val modelDirectory = File(
                    appContext.filesDir,
                    "vosk-model-es"
                )

                val requiredFile = File(
                    modelDirectory,
                    "am/final.mdl"
                )

                if (requiredFile.exists()) {
                    android.os.Handler(
                        android.os.Looper.getMainLooper()
                    ).post {
                        callback(modelDirectory)
                    }
                    return@execute
                }

                modelDirectory.deleteRecursively()
                modelDirectory.mkdirs()

                val zipFile = File(
                    appContext.cacheDir,
                    "vosk-spanish-model.zip"
                )

                downloadModel(zipFile)

                extractModel(
                    zipFile,
                    modelDirectory
                )

                zipFile.delete()

                if (!requiredFile.exists()) {
                    throw IllegalStateException(
                        "El modelo descargado está incompleto."
                    )
                }

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {
                    callback(modelDirectory)
                }

            } catch (error: Exception) {
                Log.e(TAG, "Error instalando modelo", error)

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {
                    callback(null)
                }
            }
        }
    }

    private fun downloadModel(destination: File) {
        val connection = URL(MODEL_URL)
            .openConnection() as HttpURLConnection

        connection.connectTimeout = 20000
        connection.readTimeout = 60000
        connection.requestMethod = "GET"

        try {
            connection.connect()

            if (connection.responseCode != 200) {
                throw IllegalStateException(
                    "Error HTTP ${connection.responseCode}"
                )
            }

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractModel(
        zipFile: File,
        destination: File
    ) {
        ZipInputStream(
            BufferedInputStream(zipFile.inputStream())
        ).use { zip ->

            var entry = zip.nextEntry

            while (entry != null) {
                val name = entry.name

                val separator = name.indexOf('/')

                if (separator >= 0 && separator < name.length - 1) {
                    val relativePath = name.substring(separator + 1)

                    val outputFile = File(
                        destination,
                        relativePath
                    )

                    val safeRoot = destination.canonicalPath +
                        File.separator

                    if (
                        outputFile.canonicalPath.startsWith(safeRoot)
                    ) {
                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()

                            FileOutputStream(outputFile).use { output ->
                                zip.copyTo(output)
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
