package com.example.m.FavoriteDataBase

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavouriteViewModel(application: Application) : AndroidViewModel(application) {

    private val _favouriteSongs = MutableLiveData<List<String>>(emptyList())
    val favouriteSongs: LiveData<List<String>> = _favouriteSongs

    private val _favouriteSongUris = MutableLiveData<List<String>>(emptyList())
    val favouriteSongUris: LiveData<List<String>> = _favouriteSongUris

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("Favourites", Context.MODE_PRIVATE)

    init {
        loadFavourites()
    }

    private fun loadFavourites() {
        val json = sharedPreferences.getString("favouriteSongsMap", null)
        if (json != null) {
            val type = object : TypeToken<Map<String, String>>() {}.type
            val favouriteMap: Map<String, String> = Gson().fromJson(json, type)

            _favouriteSongs.value = favouriteMap.keys.toList()
            _favouriteSongUris.value = favouriteMap.values.toList()
        }
    }

    fun addFavourite(songTitle: String, uri: String) {
        val updatedSongs = _favouriteSongs.value?.toMutableList() ?: mutableListOf()
        val updatedUris = _favouriteSongUris.value?.toMutableList() ?: mutableListOf()

        if (!updatedSongs.contains(songTitle)) {
            updatedSongs.add(songTitle)
            updatedUris.add(uri)

            _favouriteSongs.value = updatedSongs
            _favouriteSongUris.value = updatedUris

            saveFavourites(updatedSongs.zip(updatedUris).toMap())
        }
    }

    private fun saveFavourites(favouriteMap: Map<String, String>) {
        val json = Gson().toJson(favouriteMap)
        sharedPreferences.edit().putString("favouriteSongsMap", json).apply()
    }

    fun removeFavourite(songTitle: String) {
        val updatedSongs = _favouriteSongs.value?.toMutableList() ?: mutableListOf()
        val updatedUris = _favouriteSongUris.value?.toMutableList() ?: mutableListOf()

        val index = updatedSongs.indexOf(songTitle)
        if (index != -1) {
            updatedSongs.removeAt(index)
            updatedUris.removeAt(index)

            _favouriteSongs.value = updatedSongs
            _favouriteSongUris.value = updatedUris

            saveFavourites(updatedSongs.zip(updatedUris).toMap())
        }
    }
}
