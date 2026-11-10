package com.aura.ai.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class AuraPlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_PLAY_QUEUE =
            "com.aura.ai.media.PLAY_QUEUE"

        private const val EXTRA_URIS = "aura_uris"
        private const val EXTRA_TITLES = "aura_titles"
        private const val EXTRA_ARTISTS = "aura_artists"
        private const val EXTRA_ALBUMS = "aura_albums"
        private const val EXTRA_ARTWORKS = "aura_artworks"

        fun play(
            context: Context,
            tracks: List<AuraTrack>,
            startIndex: Int = 0
        ) {
            if (tracks.isEmpty()) return

            val safeIndex = startIndex.coerceIn(
                0,
                tracks.lastIndex
            )

            val intent = Intent(
                context,
                AuraPlaybackService::class.java
            ).apply {
                action = ACTION_PLAY_QUEUE

                putStringArrayListExtra(
                    EXTRA_URIS,
                    ArrayList(tracks.map { it.uri.toString() })
                )

                putStringArrayListExtra(
                    EXTRA_TITLES,
                    ArrayList(tracks.map { it.title })
                )

                putStringArrayListExtra(
                    EXTRA_ARTISTS,
                    ArrayList(tracks.map { it.artist })
                )

                putStringArrayListExtra(
                    EXTRA_ALBUMS,
                    ArrayList(tracks.map { it.album })
                )

                putStringArrayListExtra(
                    EXTRA_ARTWORKS,
                    ArrayList(
                        tracks.map {
                            it.albumArtUri?.toString().orEmpty()
                        }
                    )
                )

                putExtra("aura_start_index", safeIndex)
            }

            ContextCompat.startForegroundService(
                context,
                intent
            )
        }
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }

        player = exoPlayer

        mediaSession = MediaSession.Builder(
            this,
            exoPlayer
        ).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (intent?.action == ACTION_PLAY_QUEUE) {
            loadQueue(intent)
        }

        return super.onStartCommand(
            intent,
            flags,
            startId
        )
    }

    private fun loadQueue(intent: Intent) {
        val uris = intent.getStringArrayListExtra(EXTRA_URIS)
            ?: return

        val titles = intent.getStringArrayListExtra(EXTRA_TITLES)
            ?: return

        val artists = intent.getStringArrayListExtra(EXTRA_ARTISTS)
            ?: return

        val albums = intent.getStringArrayListExtra(EXTRA_ALBUMS)
            ?: return

        val artworks = intent.getStringArrayListExtra(EXTRA_ARTWORKS)
            ?: return

        val count = minOf(
            uris.size,
            titles.size,
            artists.size,
            albums.size,
            artworks.size
        )

        if (count == 0) return

        val items = (0 until count).map { index ->
            val artwork = artworks[index]
                .takeIf { it.isNotBlank() }
                ?.let(Uri::parse)

            val metadata = MediaMetadata.Builder()
                .setTitle(titles[index])
                .setArtist(artists[index])
                .setAlbumTitle(albums[index])
                .apply {
                    if (artwork != null) {
                        setArtworkUri(artwork)
                    }
                }
                .build()

            MediaItem.Builder()
                .setMediaId(uris[index])
                .setUri(Uri.parse(uris[index]))
                .setMediaMetadata(metadata)
                .build()
        }

        val startIndex = intent.getIntExtra(
            "aura_start_index",
            0
        ).coerceIn(0, items.lastIndex)

        player?.apply {
            setMediaItems(
                items,
                startIndex,
                0L
            )
            prepare()
            play()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = player

        if (
            currentPlayer == null ||
            !currentPlayer.playWhenReady ||
            currentPlayer.mediaItemCount == 0
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null

        player?.release()
        player = null

        super.onDestroy()
    }
}
