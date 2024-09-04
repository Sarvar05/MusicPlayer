package com.example.m.fragments

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.m.MainActivity
import com.example.m.models.PlaylistViewModel
import com.example.m.R
import com.example.m.models.MediaPlayerViewModel
import com.example.m.models.Playlist
import com.google.android.material.imageview.ShapeableImageView


class PlaylistFragment : Fragment() {

    private lateinit var playlist: Playlist
    private lateinit var songsListView: ListView
    private lateinit var songsAdapter: ArrayAdapter<String>
    private lateinit var songTitleTextView: TextView
    private lateinit var viewModel: PlaylistViewModel
    private lateinit var mediaPlayerViewModel: MediaPlayerViewModel
    private lateinit var albumArtImageView: ShapeableImageView
    private lateinit var addButton: ImageButton
    private var currentSongIndex: Int = -1

    @RequiresApi(Build.VERSION_CODES.Q)
    private val getAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val songName = getSongName(it)
            viewModel.addSongToPlaylist(playlist, songName)
            songsAdapter.notifyDataSetChanged()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            pickAudioFile()
        } else {
            Toast.makeText(requireContext(), "Permission required to access files", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlist = it.getParcelable("playlist") ?: throw IllegalArgumentException("Playlist is null")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_playlist, container, false)
        songsListView = view.findViewById(R.id.playlist_listview)
        songTitleTextView = view.findViewById(R.id.song_title)
        albumArtImageView = view.findViewById(R.id.album_cover)
        addButton = view.findViewById(R.id.addButton)
        view.findViewById<TextView>(R.id.playlist_title).text = playlist.name
        viewModel = ViewModelProvider(requireActivity()).get(PlaylistViewModel::class.java)
        mediaPlayerViewModel = ViewModelProvider(requireActivity()).get(MediaPlayerViewModel::class.java)

        songTitleTextView.isSelected = true
        setupListView()
        setupAddButton()

        view.findViewById<ImageButton>(R.id.prev_button).setOnClickListener { playPreviousSong() }
        view.findViewById<ImageButton>(R.id.play_button).setOnClickListener { togglePlayPause() }
        view.findViewById<ImageButton>(R.id.next_button).setOnClickListener { playNextSong() }

        view.findViewById<RelativeLayout>(R.id.playerLayout).setOnClickListener {
            openPlayerFragment()
        }

        mediaPlayerViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            view.findViewById<ImageButton>(R.id.play_button)
                .setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        }

        mediaPlayerViewModel.currentSongUri.observe(viewLifecycleOwner) { uriString ->
            uriString?.let {
                val songName = getSongName(Uri.parse(it))
                songTitleTextView.text = songName
                currentSongIndex = playlist.songs.indexOf(songName)
                setAlbumArt(Uri.parse(it))
            }
        }

        parentFragmentManager.setFragmentResultListener("request_key", viewLifecycleOwner) { key, bundle ->
            if (key == "request_key") {
                val selectedSongs = bundle.getStringArray("selected_songs")?.toList() ?: emptyList()
                if (selectedSongs.isNotEmpty()) {
                    viewModel.addSongsToPlaylist(playlist, selectedSongs)
                    songsAdapter.notifyDataSetChanged()
                }
            }
        }

        return view
    }

    private fun setupListView() {
        songsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, playlist.songs)
        songsListView.adapter = songsAdapter

        songsListView.setOnItemClickListener { _, _, position, _ ->
            currentSongIndex = position
            playSong(playlist.songs[position])
        }

        viewModel.playlists.observe(viewLifecycleOwner) {
            songsAdapter.notifyDataSetChanged()
        }
    }

    private fun setupAddButton() {
        addButton.setOnClickListener {
            val songSelectionFragment = SongSelectionFragment.newInstance(playlist)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, songSelectionFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun playSong(songName: String) {
        val songUri = try {
            getSongUri(songName)
        } catch (e: IllegalArgumentException) {
            return
        }

        mediaPlayerViewModel.playSong(songUri, requireContext())
        songTitleTextView.text = songName
        view?.findViewById<RelativeLayout>(R.id.playerLayout)?.visibility = View.VISIBLE
    }

    private fun getSongUri(songName: String): Uri {
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
        } ?: run {
            Log.e("PlaylistFragment", "Cursor is null for song: $songName")
        }

        throw IllegalArgumentException("Song not found: $songName")
    }

    private fun togglePlayPause() {
        mediaPlayerViewModel.mediaPlayer?.let {
            if (it.isPlaying) {
                mediaPlayerViewModel.pauseSong()
            } else {
                mediaPlayerViewModel.resumeSong()
            }
        }
    }

    private fun playNextSong() {
        if (playlist.songs.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % playlist.songs.size
            playSong(playlist.songs[currentSongIndex])
        }
    }

    private fun playPreviousSong() {
        if (playlist.songs.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex - 1 < 0) playlist.songs.size - 1 else currentSongIndex - 1
            playSong(playlist.songs[currentSongIndex])
        }
    }

    private fun openPlayerFragment() {
        val playerFragment = PlayerFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, playerFragment)
            .addToBackStack(null)
            .commit()
        (activity as? MainActivity)?.hideUiElements()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun pickAudioFile() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
            getAudio.launch("audio/*")
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun getSongName(uri: Uri): String {
        var songName = "Unknown"
        val projection = arrayOf(MediaStore.Audio.Media.TITLE)
        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                if (titleIndex != -1 && !it.isNull(titleIndex)) {
                    songName = it.getString(titleIndex)
                }
            }
        } ?: run {
            Log.e("PlaylistFragment", "Cursor is null for URI: $uri")
        }
        return songName
    }

    private fun setAlbumArt(songUri: Uri) {
        try {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(requireContext(), songUri)
            val albumArt = mediaMetadataRetriever.embeddedPicture
            if (albumArt != null) {
                val bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)
                albumArtImageView.setImageBitmap(bitmap)
            } else {
                albumArtImageView.setImageResource(R.drawable.ic_music_note)
            }
        } catch (e: Exception) {
            Log.e("PlaylistFragment", "Error setting album art", e)
        }
    }

    companion object {
        fun newInstance(playlist: Playlist) = PlaylistFragment().apply {
            arguments = Bundle().apply {
                putParcelable("playlist", playlist)
            }
        }
    }
}
