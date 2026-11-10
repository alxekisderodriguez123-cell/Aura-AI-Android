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

        val track = library.search(query, limit = 20)
            .firstOrNull()

        if (track == null) {
            return externalMedia.youtubeMusic(query, true)
        }

        AuraPlaybackService.play(
            context = appContext,
            uri = track.uri,
            title = track.title,
            artist = track.artist
        )

        return "Reproduciendo ${track.title} de ${track.artist} desde tu biblioteca."
    }
}
