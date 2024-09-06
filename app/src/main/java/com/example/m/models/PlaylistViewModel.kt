package com.example.m.models

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.m.playlist.FileUtils

class PlaylistViewModel(private val context: Context) : ViewModel() {

    private val _playlists = MutableLiveData<MutableList<Playlist>>(mutableListOf())
    val playlists: LiveData<MutableList<Playlist>> get() = _playlists

    init {
        _playlists.value = FileUtils.loadPlaylists(context).toMutableList()
    }

    private fun savePlaylists() {
        _playlists.value?.let { FileUtils.savePlaylists(context, it) }
    }

    fun addPlaylist(playlist: Playlist) {
        val updatedPlaylists = _playlists.value?.toMutableList() ?: mutableListOf()
        updatedPlaylists.add(playlist)
        _playlists.value = updatedPlaylists
        savePlaylists()
    }

    fun addSongToPlaylist(playlist: Playlist, song: String) {
        val updatedPlaylists = _playlists.value?.map {
            if (it.name == playlist.name) {
                it.copy(songs = it.songs.toMutableList().apply { add(song) })
            } else {
                it
            }
        }?.toMutableList() ?: mutableListOf()

        _playlists.value = updatedPlaylists
        savePlaylists()
    }

    fun addSongsToPlaylist(playlist: Playlist, songs: List<String>) {
        val updatedPlaylists = _playlists.value?.map {
            if (it.name == playlist.name) {
                it.copy(songs = it.songs.toMutableList().apply { addAll(songs) })
            } else {
                it
            }
        }?.toMutableList() ?: mutableListOf()

        if (updatedPlaylists.none { it.name == playlist.name }) {
            val newPlaylist = playlist.copy(songs = songs.toMutableList())
            updatedPlaylists.add(newPlaylist)
        }

        _playlists.value = updatedPlaylists
        savePlaylists()
    }
}
