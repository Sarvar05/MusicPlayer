package com.example.m.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.m.songData.Song

class MusicViewModel : ViewModel() {

    private val _songList = MutableLiveData<List<Song>>()
    val songList: LiveData<List<Song>> get() = _songList

    fun setSongs(songs: List<Song>) {
        _songList.value = songs
    }
}
