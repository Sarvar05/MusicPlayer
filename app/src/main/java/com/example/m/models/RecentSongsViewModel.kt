package com.example.m.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.m.songData.AppDatabaseResent
import com.example.m.songData.RecentSong
import com.example.m.songData.RecentSongDao
import kotlinx.coroutines.launch

class RecentSongsViewModel(application: Application) : AndroidViewModel(application) {

    private val recentSongDao: RecentSongDao =
        AppDatabaseResent.getDatabase(application).recentSongDao()
    private val _recentSongs = MutableLiveData<List<RecentSong>>()
    val recentSongs: LiveData<List<RecentSong>> get() = _recentSongs

    init {

        loadRecentSongs()
    }


    private fun loadRecentSongs() {
        viewModelScope.launch {
            recentSongDao.getRecentSongs().observeForever { songs ->
                _recentSongs.value = songs
            }

        }
        _recentSongs.value = emptyList()
    }

    fun addRecentSong(song: RecentSong) {
        viewModelScope.launch {
            val updatedList = _recentSongs.value?.toMutableList() ?: mutableListOf()

            val existingIndex = updatedList.indexOfFirst { it.id == song.id }
            if (existingIndex != -1) {
                updatedList.removeAt(existingIndex)
            }

            updatedList.add(0, song)

            if (updatedList.size > 15) {
                updatedList.removeAt(updatedList.size - 1)
            }


            _recentSongs.postValue(updatedList)
            recentSongDao.insertRecentSong(song)
        }
    }



}