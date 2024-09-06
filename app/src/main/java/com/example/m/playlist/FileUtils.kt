package com.example.m.playlist

import android.content.Context
import com.example.m.models.Playlist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.lang.reflect.Type

object FileUtils {

    private const val FILE_NAME = "playlists.json"


    fun savePlaylists(context: Context, playlists: List<Playlist>) {
        val gson = Gson()
        val json = gson.toJson(playlists)
        val file = File(context.filesDir, FILE_NAME)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos).use { writer ->
                writer.write(json)
            }
        }
    }


    fun loadPlaylists(context: Context): List<Playlist> {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            val gson = Gson()
            val json = file.readText()
            val type: Type = object : TypeToken<List<Playlist>>() {}.type
            return gson.fromJson(json, type)
        }
        return emptyList()
    }
}
