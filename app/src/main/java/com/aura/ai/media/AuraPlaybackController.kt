package com.aura.ai.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

data class AuraQueueItem(
    val title: String,
    val artist: String
)

class AuraPlaybackController(context: Context) {

    private val appContext = context.applicationContext

    var title by mutableStateOf("Aura Music")
        private set

    var artist by mutableStateOf("Tu biblioteca musical")
        private set

    var album by mutableStateOf("")
        private set

    var artworkUri by mutableStateOf<Uri?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var position by mutableStateOf(0L)
        private set

    var duration by mutableStateOf(0L)
        private set

    var queue by mutableStateOf<List<AuraQueueItem>>(emptyList())
        private set

    var currentIndex by mutableStateOf(0)
        private set

    var connected by mutableStateOf(false)
        private set

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            refresh()
        }

        override fun onMediaMetadataChanged(
            mediaMetadata: androidx.media3.common.MediaMetadata
        ) {
            refresh()
        }

        override fun onIsPlayingChanged(
            isPlaying: Boolean
        ) {
            refresh()
        }

        override fun onPlaybackStateChanged(
            playbackState: Int
        ) {
            refresh()
        }

        override fun onTimelineChanged(
            timeline: androidx.media3.common.Timeline,
            reason: Int
        ) {
            refresh()
        }
    }

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(
            appContext,
            ComponentName(
                appContext,
                AuraPlaybackService::class.java
            )
        )

        val future = MediaController.Builder(
            appContext,
            token
        ).buildAsync()

        future.addListener(
            {
                try {
                    val mediaController = future.get()
                    controller = mediaController
                    connected = true

                    mediaController.addListener(listener)
                    refresh()
                } catch (_: Exception) {
                    connected = false
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun refresh() {
        val current = controller ?: return

        val metadata = current.mediaMetadata

        title = metadata.title?.toString()
            ?: "Sin reproducción"

        artist = metadata.artist?.toString()
            ?: "Aura Music"

        album = metadata.albumTitle?.toString().orEmpty()
        artworkUri = metadata.artworkUri

        isPlaying = current.isPlaying
        position = current.currentPosition.coerceAtLeast(0L)

        duration = current.duration
            .takeIf { it > 0L && it != androidx.media3.common.C.TIME_UNSET }
            ?: 0L

        currentIndex = current.currentMediaItemIndex
            .coerceAtLeast(0)

        queue = (0 until current.mediaItemCount).map { index ->
            val item = current.getMediaItemAt(index)

            AuraQueueItem(
                title = item.mediaMetadata.title?.toString()
                    ?: "Audio ${index + 1}",
                artist = item.mediaMetadata.artist?.toString()
                    ?: "Artista desconocido"
            )
        }
    }

    fun togglePlayback() {
        val current = controller ?: return

        if (current.isPlaying) {
            current.pause()
        } else {
            current.play()
        }

        refresh()
    }

    fun next() {
        controller?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNextMediaItem()
            }
        }
        refresh()
    }

    fun previous() {
        controller?.let {
            if (it.currentPosition > 3000L) {
                it.seekTo(0L)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPreviousMediaItem()
            }
        }
        refresh()
    }

    fun seekTo(value: Long) {
        controller?.seekTo(value.coerceAtLeast(0L))
        refresh()
    }

    fun playQueueItem(index: Int) {
        val current = controller ?: return

        if (index in 0 until current.mediaItemCount) {
            current.seekTo(index, 0L)
            current.play()
        }

        refresh()
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        connected = false
    }
}
