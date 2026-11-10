package com.aura.ai.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat

class AuraMusicLibrary(context: Context) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        return ContextCompat.checkSelfPermission(
            appContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun search(query: String, limit: Int = 30): List<AuraTrack> {
        if (!hasPermission() || query.isBlank()) {
            return emptyList()
        }

        val tracks = mutableListOf<AuraTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        val selection = """
            ${MediaStore.Audio.Media.IS_MUSIC} != 0
            AND (
                ${MediaStore.Audio.Media.TITLE} LIKE ?
                OR ${MediaStore.Audio.Media.ARTIST} LIKE ?
                OR ${MediaStore.Audio.Media.ALBUM} LIKE ?
            )
        """.trimIndent()

        val searchTerm = "%${query.trim()}%"

        val sortOrder =
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            appContext.contentResolver.query(
                collection,
                projection,
                selection,
                arrayOf(searchTerm, searchTerm, searchTerm),
                sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                )
                val titleColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                )
                val artistColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST
                )
                val albumColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ALBUM
                )
                val durationColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DURATION
                )

                while (cursor.moveToNext() && tracks.size < limit) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = android.content.ContentUris.withAppendedId(
                        collection,
                        id
                    )

                    tracks.add(
                        AuraTrack(
                            id = id,
                            title = cursor.getString(titleColumn)
                                ?: "Canción desconocida",
                            artist = cursor.getString(artistColumn)
                                ?.takeUnless { it == "<unknown>" }
                                ?: "Artista desconocido",
                            album = cursor.getString(albumColumn)
                                ?: "Álbum desconocido",
                            uri = contentUri,
                            duration = cursor.getLong(durationColumn)
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }

        return tracks
    }
}
