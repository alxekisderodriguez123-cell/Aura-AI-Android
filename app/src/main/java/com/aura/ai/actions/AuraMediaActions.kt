
package com.aura.ai.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

class AuraMediaActions(private val context: Context) {

    fun youtube(query: String, play: Boolean): String {
        require(query.isNotBlank())

        if (play) {
            val playIntent = Intent(
                MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
            ).apply {
                setPackage("com.google.android.youtube")
                putExtra(SearchManager.QUERY, query)
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/video")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(playIntent)
                return "Envié a YouTube la solicitud para reproducir $query. La aplicación puede pedirte elegir un resultado."
            } catch (_: Exception) {
            }
        }

        val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(searchIntent)
            "Buscando $query en YouTube."
        } catch (_: Exception) {
            openWeb(
                "https://www.youtube.com/results?search_query=${Uri.encode(query)}",
                "Abriendo los resultados de YouTube para $query."
            )
        }
    }

    fun youtubeMusic(query: String, play: Boolean): String {
        require(query.isNotBlank())

        val intent = Intent(
            MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
        ).apply {
            setPackage("com.google.android.apps.youtube.music")
            putExtra(SearchManager.QUERY, query)
            putExtra(
                MediaStore.EXTRA_MEDIA_FOCUS,
                "vnd.android.cursor.item/audio"
            )
            putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "")
            putExtra(MediaStore.EXTRA_MEDIA_TITLE, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            if (play) {
                "Envié a YouTube Music la solicitud de reproducción de $query."
            } else {
                "Buscando $query en YouTube Music."
            }
        } catch (_: Exception) {
            openWeb(
                "https://music.youtube.com/search?q=${Uri.encode(query)}",
                "Abriendo la búsqueda de $query en YouTube Music."
            )
        }
    }

    fun spotify(query: String, play: Boolean): String {
        require(query.isNotBlank())

        if (play) {
            val playIntent = Intent(
                MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
            ).apply {
                setPackage("com.spotify.music")
                putExtra(SearchManager.QUERY, query)
                putExtra(
                    MediaStore.EXTRA_MEDIA_FOCUS,
                    "vnd.android.cursor.item/audio"
                )
                putExtra(MediaStore.EXTRA_MEDIA_TITLE, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(playIntent)
                return "Envié a Spotify la solicitud de reproducción de $query."
            } catch (_: Exception) {
            }
        }

        val uri = Uri.parse("spotify:search:${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setPackage("com.spotify.music")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            "Buscando $query en Spotify."
        } catch (_: Exception) {
            openWeb(
                "https://open.spotify.com/search/${Uri.encode(query)}",
                "Abriendo la búsqueda de $query en Spotify."
            )
        }
    }

    fun google(query: String): String {
        require(query.isNotBlank())

        return openWeb(
            "https://www.google.com/search?q=${Uri.encode(query)}",
            "Buscando $query en Google."
        )
    }

    private fun openWeb(url: String, message: String): String {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
        return message
    }
}
