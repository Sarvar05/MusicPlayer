package com.example.m.models
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.m.songData.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException


class MediaPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentSongUri = MutableLiveData<String?>()
    val currentSongUri: LiveData<String?> get() = _currentSongUri

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _currentSongName = MutableLiveData<String?>()
    val currentSongName: LiveData<String?> get() = _currentSongName

    private val _currentPosition = MutableLiveData<Int>()
    val currentPosition: LiveData<Int> get() = _currentPosition

    private val _songDuration = MutableLiveData<Int>()
    val songDuration: LiveData<Int> get() = _songDuration

    private val _albumArt = MutableLiveData<Bitmap?>()
    val albumArt: LiveData<Bitmap?> get() = _albumArt

    private val _favouriteSongs = MutableLiveData<Set<Song>>()
    val favouriteSongs: LiveData<Set<Song>> get() = _favouriteSongs

    private val _songList = mutableListOf<Song>()
    private var currentIndex = 0

    var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false

    init {
        loadFavouritesFromPreferences()
    }

    fun updateNotification(title: String, artist: String, albumArtRes: Int) {
        val serviceIntent = Intent(getApplication(), MediaPlayerService1::class.java).apply {
            action = MediaPlayerService1.ACTION_UPDATE_NOTIFICATION
            putExtra("title", title)
            putExtra("artist", artist)
            putExtra("albumArtRes", albumArtRes)
        }
        ContextCompat.startForegroundService(getApplication(), serviceIntent)
    }

    fun setPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun setCurrentSongName(songName: String) {
        _currentSongName.value = songName
    }

    fun playSong(uri: Uri, context: Context) {
        Log.d("MediaPlayerViewModel", "Starting to play song with URI: $uri")

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    Log.d("MediaPlayerViewModel", "MediaPlayer is prepared")
                    isPrepared = true
                    _songDuration.value = mp.duration
                    mp.start()
                    _isPlaying.value = true
                    _currentSongUri.value = uri.toString()
                    _currentSongName.value = getSongNameFromUri(uri, context)
                    _albumArt.value = getAlbumArt(uri, context)
                    updatePosition()
                }

                setOnCompletionListener {
                    Log.d("MediaPlayerViewModel", "MediaPlayer completed playback")
                    _isPlaying.value = false
                    isPrepared = false
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayerViewModel", "Error occurred: what=$what, extra=$extra")
                    isPrepared = false
                    _isPlaying.value = false
                    true
                }

                prepareAsync()
                Log.d("MediaPlayerViewModel", "MediaPlayer is preparing asynchronously")
            } catch (e: IOException) {
                Log.e("MediaPlayerViewModel", "Error setting data source: ${e.message}")
            }
        }
    }

    fun addSongToFavorites(song: Song) {
        val currentFavourites = _favouriteSongs.value?.toMutableSet() ?: mutableSetOf()
        currentFavourites.add(song)
        Log.d("MediaPlayerViewModel", "Added song to favourites: ${song.title}")
        _favouriteSongs.value = currentFavourites
        saveFavouritesToPreferences(currentFavourites)
    }

    private fun saveFavouritesToPreferences(songs: Set<Song>) {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("Favourites", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(songs)
        Log.d("MediaPlayerViewModel", "Saving favourites: $json")
        editor.putString("favouriteSongsSet", json)
        editor.apply()
    }

    private fun loadFavouritesFromPreferences() {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("Favourites", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("favouriteSongsSet", null)
        Log.d("MediaPlayerViewModel", "Loading favourites: $json")
        val gson = Gson()
        val type = object : TypeToken<Set<Song>>() {}.type
        val songs = gson.fromJson<Set<Song>>(json, type) ?: emptySet()
        _favouriteSongs.value = songs
    }

    private fun getAlbumArt(uri: Uri, context: Context): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val art = retriever.embeddedPicture
            art?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        } catch (e: IllegalArgumentException) {
            Log.e("MediaPlayerViewModel", "Failed to retrieve album art: ${e.message}")
            null
        } finally {
            retriever.release()
        }
    }

    private fun getSongNameFromUri(uri: Uri, context: Context): String {
        val projection = arrayOf(MediaStore.Audio.Media.TITLE)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val title = cursor.getString(titleColumn)
                Log.d("MediaPlayerViewModel", "Song title from URI: $title")
                return title
            }
        }
        return "Unknown"
    }

    fun resumeSong() {
        mediaPlayer?.takeIf { isPrepared && !it.isPlaying }?.let { mp ->
            try {
                mp.start()
                _isPlaying.value = true
            } catch (e: IllegalStateException) {
                Log.e("MediaPlayerViewModel", "Error resuming MediaPlayer: ${e.message}")
                resetMediaPlayer()
            }
        } ?: Log.e("MediaPlayerViewModel", "Cannot resume: isPrepared=$isPrepared, isPlaying=${mediaPlayer?.isPlaying}")
    }

    fun pauseSong() {
        mediaPlayer?.let { mp ->
            if (isPrepared && mp.isPlaying) {
                try {
                    mp.pause()
                    _isPlaying.value = false
                } catch (e: IllegalStateException) {
                    Log.e("MediaPlayerViewModel", "Error pausing MediaPlayer: ${e.message}")
                    resetMediaPlayer()
                }
            } else {
                Log.e("MediaPlayerViewModel", "Cannot pause: isPrepared=$isPrepared, isPlaying=${mp.isPlaying}")
            }
        }
    }

    fun stopSong() {
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _currentSongUri.value = null
        _currentSongName.value = null
        isPrepared = false
    }

    fun seekTo(position: Int) {
        mediaPlayer?.let { mp ->
            if (isPrepared) {
                mp.seekTo(position)
                _currentPosition.value = position
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                pauseSong()
            } else {
                resumeSong()
            }
        }
    }

    fun playPrevious() {
        if (_songList.isNotEmpty()) {
            Log.d("MediaPlayerViewModel", "Current Index before: $currentIndex")

            currentIndex = (currentIndex - 1 + _songList.size) % _songList.size
            Log.d("MediaPlayerViewModel", "New Index: $currentIndex")

            val previousSong = _songList[currentIndex]
            _currentSongUri.value = previousSong.uri
            _currentSongName.value = previousSong.title
            playSong(Uri.parse(previousSong.uri), getApplication())
            _isPlaying.value = true

            Log.d("MediaPlayerViewModel", "Playing previous song: ${previousSong.title}")
        } else {
            Log.d("MediaPlayerViewModel", "No songs to play previous")
        }
    }

    fun playNext() {
        if (_songList.isNotEmpty()) {
            Log.d("MediaPlayerViewModel", "Current Index before: $currentIndex")


            currentIndex = (currentIndex + 1) % _songList.size
            Log.d("MediaPlayerViewModel", "New Index: $currentIndex")


            val nextSong = _songList[currentIndex]

            Log.d("MediaPlayerViewModel", "Playing next song: ${nextSong.title}")


            _currentSongUri.value = nextSong.uri
            _currentSongName.value = nextSong.title


            playSong(Uri.parse(nextSong.uri), getApplication())
            _isPlaying.value = true
        } else {
            Log.d("MediaPlayerViewModel", "No songs available to play next")
        }
    }

    fun rewind(milliseconds: Int) {
        mediaPlayer?.let {
            if (isPrepared) {
                val newPosition = (it.currentPosition - milliseconds).coerceAtLeast(0)
                it.seekTo(newPosition)
                _currentPosition.value = newPosition
                Log.d("MediaPlayerViewModel", "Rewound to position: $newPosition")
            }
        }
    }

    fun forward(milliseconds: Int) {
        mediaPlayer?.let {
            if (isPrepared) {
                val newPosition = (it.currentPosition + milliseconds).coerceAtMost(it.duration)
                it.seekTo(newPosition)
                _currentPosition.value = newPosition
                Log.d("MediaPlayerViewModel", "Forwarded to position: $newPosition")
            }
        }
    }

    fun updatePosition() {
        mediaPlayer?.let { mp ->
            if (isPrepared) {
                try {
                    _currentPosition.value = mp.currentPosition
                    if (mp.isPlaying) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            updatePosition()
                        }, 1000) // Update every second
                    } else {

                    }
                } catch (e: IllegalStateException) {
                    Log.e("MediaPlayerViewModel", "Error getting current position: ${e.message}")
                    resetMediaPlayer() // Safeguard by resetting the media player on error
                }
            } else {
                Log.e("MediaPlayerViewModel", "MediaPlayer is not prepared")
            }
        } ?: Log.e("MediaPlayerViewModel", "MediaPlayer is null")
    }

    private fun resetMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
        _isPlaying.value = false
        _currentSongUri.value = null
        _currentSongName.value = null
        Log.d("MediaPlayerViewModel", "MediaPlayer reset")
    }
}
