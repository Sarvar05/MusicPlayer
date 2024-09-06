package com.example.m.fragments

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import com.example.m.R
import com.example.m.adapters.SongAdapter
import com.example.m.models.Playlist
import com.example.m.models.PlaylistViewModel


class SongSelectionFragment : Fragment() {

    private lateinit var songsListView: ListView
    private lateinit var songAdapter: SongAdapter
    private lateinit var addButton: Button
    private var selectedPlaylist: Playlist? = null



    companion object {
        fun newInstance(playlist: Playlist): SongSelectionFragment {
            val fragment = SongSelectionFragment()
            val args = Bundle()
            args.putParcelable("playlist", playlist)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedPlaylist = it.getParcelable("playlist")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_song_selection, container, false)
        songsListView = view.findViewById(R.id.song_list_view)
        addButton = view.findViewById(R.id.add_songs_button)


        view.findViewById<Button>(R.id.add_songs_button)?.setOnClickListener {
            onDone()
        }

        val songs = getAvailableSongs()
        songAdapter = SongAdapter(requireContext(), songs)
        songsListView.adapter = songAdapter

        addButton.setOnClickListener {
            val selectedSongs = songAdapter.getSelectedSongs()
            if (selectedSongs.isNotEmpty()) {
                val result = Bundle().apply {
                    putStringArray("selected_songs", selectedSongs.toTypedArray())
                }
                parentFragmentManager.setFragmentResult("request_key", result)
                parentFragmentManager.popBackStack()

            } else {
                Toast.makeText(requireContext(), "No songs selected", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    private fun getAvailableSongs(): List<String> {
        val songs = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Audio.Media.TITLE)
        val cursor = requireContext().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
            while (it.moveToNext()) {
                val title = it.getString(titleIndex)
                if (title != null) {
                    songs.add(title)
                }
            }
        }

        return songs
    }

    private fun addSelectedSongsToPlaylist() {
        val selectedSongs = songAdapter.getSelectedSongs()
        selectedPlaylist?.let { playlist ->

            for (songName in selectedSongs) {
                val songUri = getSongUri(songName)

            }
        }
    }

    private fun getSongUri(songName: String): Uri? {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.TITLE}=?"
        val selectionArgs = arrayOf(songName)

        val cursor = requireContext().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            }
        }

        return null
    }



    private fun onDone() {
        val playlist = selectedPlaylist ?: return
        val result = Bundle()
        result.putParcelable("playlist", playlist)
        parentFragmentManager.setFragmentResult("playlist_request_key", result)


        parentFragmentManager.popBackStack()
    }
}
