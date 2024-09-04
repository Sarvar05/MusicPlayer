package com.example.m.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import com.example.m.R
class SongPickerDialogFragment : DialogFragment() {

    private lateinit var listView: ListView
    private lateinit var songsAdapter: ArrayAdapter<String>
    private var onSongSelectedListener: ((String) -> Unit)? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_song_picker_dialog, container, false)
        listView = view.findViewById(R.id.listView)

        val songs = arguments?.getStringArrayList("songs") ?: arrayListOf()
        songsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, songs)
        listView.adapter = songsAdapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedSong = songs[position]
            onSongSelectedListener?.invoke(selectedSong)
            dismiss()
        }

        return view
    }

    fun setOnSongSelectedListener(listener: (String) -> Unit) {
        onSongSelectedListener = listener
    }

    companion object {
        fun newInstance(songs: ArrayList<String>) = SongPickerDialogFragment().apply {
            arguments = Bundle().apply {
                putStringArrayList("songs", songs)
            }
        }
    }
}
