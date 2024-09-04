package com.example.m.models

import android.media.MediaPlayer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayerViewModel : ViewModel() {

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _currentSongIndex = MutableLiveData<Int>()
    val currentSongIndex: LiveData<Int> get() = _currentSongIndex

    private val songList = mutableListOf<String>()

    init {
        _isPlaying.value = false
    }

    fun playPrevious() {
        val currentIndex = _currentSongIndex.value ?: return
        if (currentIndex > 0) {
            _currentSongIndex.value = currentIndex - 1
            playSongAtIndex(currentIndex - 1)
        }
    }

    fun playNext() {
        val currentIndex = _currentSongIndex.value ?: return
        if (currentIndex < songList.size - 1) {
            _currentSongIndex.value = currentIndex + 1
            playSongAtIndex(currentIndex + 1)
        }
    }

    fun pauseSong() {

        _isPlaying.value = false
    }

    fun resumeSong() {

        _isPlaying.value = true
    }

    private fun playSongAtIndex(index: Int) {
        val uri = songList.getOrNull(index) ?: return

    }
}
