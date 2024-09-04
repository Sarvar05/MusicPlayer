package com.example.m.models

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.m.R


class MediaPlayerService1 : Service() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionCallback: MediaSessionCompat.Callback
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var trackList = listOf<String>()
    private var currentTrackIndex = 0

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "MediaService")
        mediaSessionCallback = object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                handleActionPlay()
            }

            override fun onPause() {
                super.onPause()
                handleActionPause()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                handleActionNext()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                handleActionPrev()
            }

            override fun onStop() {
                super.onStop()
                handleActionClose()
            }
        }
        mediaSession.setCallback(mediaSessionCallback)
        mediaSession.isActive = true
        initializeMediaPlayer()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener {
                handleActionNext()
            }
        }
    }

    private fun createNotification(): Notification {
        val metadata = mediaSession.controller.metadata
        val title = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "Unknown Title"
        val artist = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_main)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_main)
            .setLargeIcon(largeIcon)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .addAction(
                R.drawable.ic_back_musci,
                "Previous",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, MediaPlayerService1::class.java).apply { action = ACTION_PREV },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause_music else R.drawable.ic_play_music,
                if (isPlaying) "Pause" else "Play",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, MediaPlayerService1::class.java).apply {
                        action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                R.drawable.ic_next_music,
                "Next",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, MediaPlayerService1::class.java).apply { action = ACTION_NEXT },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                R.drawable.ic_cross,
                "Close",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, MediaPlayerService1::class.java).apply { action = ACTION_CLOSE },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun setMediaSessionMetadata(title: String, artist: String, albumArtRes: Int) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putBitmap(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                BitmapFactory.decodeResource(resources, albumArtRes)
            )
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun updateNotification(title: String, artist: String, albumArtRes: Int) {
        Log.d("MediaPlayerService", "Updating notification with title: $title, artist: $artist")
        setMediaSessionMetadata(title, artist, albumArtRes)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun playCurrentTrack() {
        val currentTrackUri = trackList.getOrNull(currentTrackIndex) ?: return
        Log.d("MediaPlayerService", "Playing track from URI: $currentTrackUri")
        try {
            mediaPlayer?.apply {
                reset()
                setDataSource(this@MediaPlayerService1, Uri.parse(currentTrackUri))
                prepare()
                start()
                this@MediaPlayerService1.isPlaying = true
            }
            val title = "Track ${currentTrackIndex + 1}"
            val artist = "Artist"
            updateNotification(title, artist, R.drawable.ic_music_note)
        } catch (e: Exception) {
            Log.e("MediaPlayerService", "Error playing track: ${e.message}", e)
        }
    }

    private fun handleActionPrev() {
        if (currentTrackIndex > 0) {
            currentTrackIndex--
            playCurrentTrack()
        } else {
            Log.d("MediaPlayerService", "No previous track available")
        }
    }

    private fun handleActionPause() {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            updateNotification(
                mediaSession.controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    ?: "Song Title",
                mediaSession.controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                    ?: "Artist",
                R.drawable.ic_music_note
            )
        }
    }

    private fun handleActionPlay() {
        if (!isPlaying) {
            mediaPlayer?.apply {
                if (currentTrackIndex < trackList.size) {
                    val currentTrackUri = trackList[currentTrackIndex]
                    reset()
                    setDataSource(this@MediaPlayerService1, Uri.parse(currentTrackUri))
                    prepare()
                }
                start()
                this@MediaPlayerService1.isPlaying = true
            }
            updateNotification(
                mediaSession.controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    ?: "Song Title",
                mediaSession.controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                    ?: "Artist",
                R.drawable.ic_music_note
            )
        }
    }

    private fun handleActionNext() {
        if (currentTrackIndex < trackList.size - 1) {
            currentTrackIndex++
            playCurrentTrack()
        } else {
            Log.d("MediaPlayerService", "No next track available")
        }
    }

    private fun handleActionClose() {
        try {
            mediaPlayer?.apply {
                stop()
                reset()
                release()
            }
            mediaPlayer = null
            mediaSession.release()
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.e("MediaPlayerService", "Ошибка при закрытии службы: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREV -> handleActionPrev()
            ACTION_PAUSE -> handleActionPause()
            ACTION_PLAY -> handleActionPlay()
            ACTION_NEXT -> handleActionNext()
            ACTION_CLOSE -> handleActionClose()
            ACTION_UPDATE_NOTIFICATION -> {
                val title = intent.getStringExtra("title")
                val artist = intent.getStringExtra("artist")
                val albumArtRes = intent.getIntExtra("albumArtRes", R.drawable.ic_music_note)
                if (title != null && artist != null) {
                    updateNotification(title, artist, albumArtRes)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.apply {
                stop()
                reset()
                release()
            }
            mediaPlayer = null
            mediaSession.release()
        } catch (e: Exception) {
            Log.e("MediaPlayerService", "Ошибка при освобождении ресурсов: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val CHANNEL_ID = "media_playback_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PREV = "action_prev"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_PLAY = "action_play"
        const val ACTION_NEXT = "action_next"
        const val ACTION_CLOSE = "action_close"
        const val ACTION_UPDATE_NOTIFICATION = "action_update_notification"
    }
}
