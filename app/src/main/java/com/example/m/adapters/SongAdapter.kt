package com.example.m.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import com.example.m.R


class SongAdapter(
    context: Context,
    private val songs: List<String>
) : ArrayAdapter<String>(context, R.layout.item_song_selection, songs) {

    private val selectedSongs = mutableSetOf<String>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_song_selection, parent, false)

        val songTitleTextView: TextView = view.findViewById(R.id.song_title)
        val songCheckBox: CheckBox = view.findViewById(R.id.song_checkbox)

        val songName = getItem(position) ?: return view
        songTitleTextView.text = songName

        // Update CheckBox state based on whether the song is selected
        songCheckBox.isChecked = selectedSongs.contains(songName)

        // Set listener to update selectedSongs when CheckBox state changes
        songCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedSongs.add(songName)
            } else {
                selectedSongs.remove(songName)
            }
        }

        return view
    }

    fun getSelectedSongs(): Set<String> {
        return selectedSongs
    }
}
