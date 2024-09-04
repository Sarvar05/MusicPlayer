package com.example.m.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlaylistViewModel : ViewModel() {

    private val _playlists = MutableLiveData<MutableList<Playlist>>(mutableListOf())
    val playlists: LiveData<MutableList<Playlist>> get() = _playlists

    fun addPlaylist(playlist: Playlist) {
        val updatedPlaylists = _playlists.value?.toMutableList() ?: mutableListOf()
        updatedPlaylists.add(playlist)
        _playlists.value = updatedPlaylists
    }

    fun addSongToPlaylist(playlist: Playlist, song: String) {
        val updatedPlaylists = _playlists.value?.toMutableList() ?: mutableListOf()
        val index = updatedPlaylists.indexOfFirst { it.name == playlist.name }
        if (index != -1) {
            val currentPlaylist = updatedPlaylists[index]
            val updatedPlaylist = currentPlaylist.copy(
                songs = (currentPlaylist.songs as MutableList).apply { add(song) }
            )
            updatedPlaylists[index] = updatedPlaylist
            _playlists.value = updatedPlaylists
        }
    }

    fun addSongsToPlaylist(playlist: Playlist, songs: List<String>) {
        val updatedPlaylists = _playlists.value?.toMutableList() ?: mutableListOf()
        val index = updatedPlaylists.indexOfFirst { it.name == playlist.name }
        if (index != -1) {
            val currentPlaylist = updatedPlaylists[index]
            val updatedPlaylist = currentPlaylist.copy(
                songs = (currentPlaylist.songs as MutableList).apply { addAll(songs) }
            )
            updatedPlaylists[index] = updatedPlaylist
            _playlists.value = updatedPlaylists
        }
    }
}