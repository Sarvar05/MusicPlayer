package com.example.m.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.example.m.R

class SongSelectionAdapter(
    context: Context,
    private val songs: List<String>,
    private val selectedSongs: MutableSet<String>
) : ArrayAdapter<String>(context, 0, songs) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_song_selection, parent, false)
        val song = getItem(position)
        val checkBox = view.findViewById<CheckBox>(R.id.song_checkbox)
        val songNameTextView = view.findViewById<TextView>(R.id.song_title)

        songNameTextView.text = song
        checkBox.isChecked = selectedSongs.contains(song)

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedSongs.add(song!!)
            } else {
                selectedSongs.remove(song)
            }
        }

        return view
    }
}
