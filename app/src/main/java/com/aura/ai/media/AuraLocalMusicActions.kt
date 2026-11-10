package com.aura.ai.media

import android.content.Context
import com.aura.ai.actions.AuraMediaActions

class AuraLocalMusicActions(context: Context) {

    private val appContext = context.applicationContext
    private val library = AuraMusicLibrary(appContext)
    private val externalMedia = AuraMediaActions(appContext)

    fun playLocalOrFallback(
        query: String,
        play: Boolean
    ): String {
        if (query.isBlank()) {
            return "No pude identificar qué canción quieres escuchar."
        }

        if (!play) {
            return externalMedia.youtubeMusic(query, false)
        }

        if (!library.hasPermission()) {
            return externalMedia.youtubeMusic(query, true)
        }

        val tracks = library.search(
            query = query,
            limit = 30
        )

        if (tracks.isEmpty()) {
            return externalMedia.youtubeMusic(query, true)
        }

        AuraPlaybackService.play(
            context = appContext,
            tracks = tracks
        )

        val first = tracks.first()

        return if (tracks.size == 1) {
            "Reproduciendo ${first.title} de ${first.artist} desde tu biblioteca."
        } else {
            "Reproduciendo ${first.title} de ${first.artist}. " +
                "Encontré ${tracks.size} canciones y las añadí a la cola de Aura."
        }
    }
}
