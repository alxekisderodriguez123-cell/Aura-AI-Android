package com.aura.ai.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aura.ai.MainActivity
import com.aura.ai.R

class AuraPlaybackService : Service() {

    companion object {
        const val ACTION_PLAY = "com.aura.ai.media.PLAY"
        const val ACTION_PAUSE = "com.aura.ai.media.PAUSE"
        const val ACTION_RESUME = "com.aura.ai.media.RESUME"
        const val ACTION_STOP = "com.aura.ai.media.STOP"

        const val EXTRA_URI = "track_uri"
        const val EXTRA_TITLE = "track_title"
        const val EXTRA_ARTIST = "track_artist"

        private const val CHANNEL_ID = "aura_music"
        private const val NOTIFICATION_ID = 4102

        fun play(
            context: Context,
            uri: Uri,
            title: String,
            artist: String
        ) {
            val intent = Intent(
                context,
                AuraPlaybackService::class.java
            ).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_URI, uri.toString())
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) =
            send(context, ACTION_PAUSE)

        fun resume(context: Context) =
            send(context, ACTION_RESUME)

        fun stop(context: Context) =
            send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            val intent = Intent(
                context,
                AuraPlaybackService::class.java
            ).setAction(action)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private var player: MediaPlayer? = null
    private var currentTitle = "Aura"
    private var currentArtist = "Reproducción local"
    private var isPrepared = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val uri = intent.getStringExtra(EXTRA_URI)
                currentTitle = intent.getStringExtra(EXTRA_TITLE)
                    ?: "Audio"
                currentArtist = intent.getStringExtra(EXTRA_ARTIST)
                    ?: "Desconocido"

                if (uri.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(
                    NOTIFICATION_ID,
                    buildNotification()
                )

                startPlayback(Uri.parse(uri))
            }

            ACTION_PAUSE -> {
                if (player?.isPlaying == true) {
                    player?.pause()
                    updateNotification()
                }
            }

            ACTION_RESUME -> {
                if (isPrepared) {
                    player?.start()
                    updateNotification()
                }
            }

            ACTION_STOP -> {
                stopPlayback()
            }

            else -> {
                if (player == null) {
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startPlayback(uri: Uri) {
        releasePlayer()

        isPrepared = false

        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                setDataSource(this@AuraPlaybackService, uri)

                setOnPreparedListener { mediaPlayer ->
                    isPrepared = true
                    mediaPlayer.start()
                    updateNotification()
                }

                setOnCompletionListener {
                    stopPlayback()
                }

                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    true
                }

                prepareAsync()
            }
        } catch (_: Exception) {
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        releasePlayer()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayer() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }

        try {
            player?.release()
        } catch (_: Exception) {
        }

        player = null
        isPrepared = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Música de Aura",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Reproducción de música local"
            setShowBadge(false)
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = packageManager
            .getLaunchIntentForPackage(packageName)

        val contentPendingIntent = openIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )
        }

        val playing = player?.isPlaying == true

        val controlAction = if (playing) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pausar",
                servicePendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Reanudar",
                servicePendingIntent(ACTION_RESUME)
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(controlAction)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener",
                servicePendingIntent(ACTION_STOP)
            )
            .build()
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, AuraPlaybackService::class.java)
            .setAction(action)

        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotification() {
        val manager = getSystemService(
            NotificationManager::class.java
        )
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
