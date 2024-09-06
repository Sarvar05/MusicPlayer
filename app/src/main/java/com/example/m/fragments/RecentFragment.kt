package com.example.m.fragments


import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.m.MainActivity
import com.example.m.R
import com.example.m.models.Playlist
import com.example.m.models.PlaylistViewModel
import com.example.m.playlist.PlaylistViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton


class RecentFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var fab: FloatingActionButton
    private lateinit var playlistAdapter: ArrayAdapter<String>
    private lateinit var viewModel: PlaylistViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("RecentFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_recent, container, false)
        listView = view.findViewById(R.id.listView)
        fab = view.findViewById(R.id.fab)
        val factory = PlaylistViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory).get(PlaylistViewModel::class.java)
        setupListView()
        setupFab()
        return view
    }

    private fun setupListView() {
        Log.d("RecentFragment", "Setting up ListView")
        playlistAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            viewModel.playlists.value?.map { it.name } ?: emptyList()
        )
        listView.adapter = playlistAdapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val playlist = viewModel.playlists.value?.get(position)
            playlist?.let {
                Log.d("RecentFragment", "Opening existing playlist: ${it.name}")
                openPlaylist(it)
            } ?: Log.e("RecentFragment", "Playlist at position $position is null")
        }

        viewModel.playlists.observe(viewLifecycleOwner, Observer { playlists ->
            Log.d("RecentFragment", "Playlists updated: ${playlists.map { it.name }}")
            playlistAdapter.clear()
            playlistAdapter.addAll(playlists.map { it.name })
            playlistAdapter.notifyDataSetChanged()
        })
    }

    private fun setupFab() {
        fab.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun showCreatePlaylistDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Введите название плейлиста")

        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Создать") { dialog, _ ->
            val playlistName = input.text.toString()
            if (playlistName.isNotEmpty()) {
                addNewPlaylist(playlistName)
            } else {
                Toast.makeText(
                    context,
                    "Название плейлиста не может быть пустым",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }


    private fun addNewPlaylist(name: String) {
        val newPlaylist = Playlist(name)
        Log.d("RecentFragment", "Adding new playlist: $name")


        viewModel.addPlaylist(newPlaylist)
        Log.d("RecentFragment", "New playlist added: $newPlaylist")

        openPlaylist(newPlaylist)

    }

    private fun openSongSelectionFragment(playlist: Playlist) {
        Log.d("RecentFragment", "Attempting to open SongSelectionFragment for playlist: ${playlist.name}")
        val tag = "song_selection_${playlist.name}"
        val existingFragment = parentFragmentManager.findFragmentByTag(tag)

        if (existingFragment == null) {
            val fragment = SongSelectionFragment.newInstance(playlist)
            Log.d("RecentFragment", "Created SongSelectionFragment for playlist: ${playlist.name}")

            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment, tag)
            transaction.addToBackStack(tag)
            transaction.commit()


            (activity as? MainActivity)?.hideUiElements()
        } else {
            Log.d("RecentFragment", "SongSelectionFragment already exists for playlist: ${playlist.name}")
            parentFragmentManager.popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }




    private fun openPlaylist(playlist: Playlist) {
        Log.d("RecentFragment", "Attempting to open playlist: ${playlist.name}")
        val tag = "playlist_${playlist.name}"
        val existingFragment = parentFragmentManager.findFragmentByTag(tag)

        if (existingFragment == null) {
            val fragment = PlaylistFragment.newInstance(playlist)
            Log.d("RecentFragment", "Created PlaylistFragment for playlist: ${playlist.name}")

            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment, tag)
            transaction.addToBackStack(tag)
            transaction.commit()


            (activity as? MainActivity)?.hideUiElements()
        } else {
            Log.d("RecentFragment", "PlaylistFragment already exists for playlist: ${playlist.name}")
            parentFragmentManager.popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }





}