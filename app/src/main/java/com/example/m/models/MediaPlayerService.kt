package com.example.m.models

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.m.R
class MediaPlayerService : Service() {
    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> get() = _isPlaying
    var mediaPlayer: MediaPlayer? = null
    var currentSongUri: Uri? = null

    companion object {
        const val CHANNEL_ID = "media_playback_channel"
        const val ACTION_PLAY = "com.example.yourapp.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.yourapp.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.yourapp.ACTION_RESUME"
        const val ACTION_STOP = "com.example.yourapp.ACTION_STOP"
        const val EXTRA_URI = "com.example.yourapp.EXTRA_URI"

        fun startService(context: Context, songUri: Uri?, action: String) {
            val intent = Intent(context, MediaPlayerService::class.java).apply {
                this.action = action
                putExtra(EXTRA_URI, songUri)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MediaPlayerService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        return MediaPlayerBinder()
    }

    inner class MediaPlayerBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        val uri = intent.getParcelableExtra<Uri>(EXTRA_URI)

        when (action) {
            ACTION_PLAY -> uri?.let { playSong(it) }
            ACTION_PAUSE -> pauseSong()
            ACTION_RESUME -> resumeSong()
            ACTION_STOP -> stopSong()
        }


        val notification = createNotification()
        startForeground(1, notification)

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Media Player")
            .setContentText("Playing music...")
            .setSmallIcon(R.drawable.ic_music_note)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return notificationBuilder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun setPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun playSong(songUri: Uri) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, songUri)
                prepare()
                start()
            }
        } else {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(applicationContext, songUri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        }
        currentSongUri = songUri
        _isPlaying.value = true
    }


    fun pauseSong() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    fun resumeSong() {
        mediaPlayer?.start()
        _isPlaying.value = true
    }

    fun stopSong() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSong()
    }
}
