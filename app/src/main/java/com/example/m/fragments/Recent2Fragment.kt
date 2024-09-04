package com.example.m.fragments


import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.m.MainActivity
import com.example.m.R
import com.example.m.models.MediaPlayerViewModel
import com.example.m.models.RecentSongsViewModel
import com.google.android.material.imageview.ShapeableImageView

class Recent2Fragment : Fragment() {

    private lateinit var recentSongsListView: ListView
    private lateinit var recentSongsViewModel: RecentSongsViewModel
    private lateinit var mediaPlayerViewModel: MediaPlayerViewModel
    private lateinit var playButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var songTitleTextView: TextView
    private lateinit var playerLayout: RelativeLayout
    private lateinit var albumCoverImageView: ShapeableImageView
    private var currentSongIndex = -1
    private var isPlaying = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recent2, container, false)


        recentSongsListView = view.findViewById(R.id.recentListView)
        songTitleTextView = view.findViewById(R.id.song_title)
        playButton = view.findViewById(R.id.play_button)
        prevButton = view.findViewById(R.id.prev_button)
        nextButton = view.findViewById(R.id.next_button)
        playerLayout = view.findViewById(R.id.playerLayout)
        albumCoverImageView = view.findViewById(R.id.album_cover)


        recentSongsViewModel = ViewModelProvider(this).get(RecentSongsViewModel::class.java)
        mediaPlayerViewModel = ViewModelProvider(requireActivity()).get(MediaPlayerViewModel::class.java)


        recentSongsViewModel.recentSongs.observe(viewLifecycleOwner) { recentSongs ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, recentSongs.map { it.title })
            recentSongsListView.adapter = adapter
        }


        recentSongsListView.setOnItemClickListener { _, _, position, _ ->
            playSong(position)
        }


        playButton.setOnClickListener {
            if (isPlaying) {
                mediaPlayerViewModel.pauseSong()
                playButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayerViewModel.resumeSong()
                playButton.setImageResource(android.R.drawable.ic_media_pause)
            }
            isPlaying = !isPlaying
        }

        prevButton.setOnClickListener {
            if (currentSongIndex > 0) {
                playSong(currentSongIndex - 1)
            }
        }

        nextButton.setOnClickListener {
            val size = recentSongsViewModel.recentSongs.value?.size ?: 0
            if (currentSongIndex < size - 1) {
                playSong(currentSongIndex + 1)
            }
        }

        playerLayout.setOnClickListener {
            if (currentSongIndex >= 0) {
                openPlayerFragment()
            }
        }

        songTitleTextView.isSelected = true


        mediaPlayerViewModel.currentSongUri.observe(viewLifecycleOwner) { uriString ->
            if (uriString != null) {
                songTitleTextView.text = getSongName(uriString)
                currentSongIndex = recentSongsViewModel.recentSongs.value?.indexOfFirst { it.id == uriString } ?: -1
                loadAlbumCover(uriString)
            }
        }

        mediaPlayerViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
            isPlaying = playing ?: false
            playButton.setImageResource(
                if (isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }

        return view
    }

    private fun playSong(position: Int) {
        currentSongIndex = position
        val song = recentSongsViewModel.recentSongs.value?.get(position) ?: return
        val songUri = getSongUri(song.title)

        playerLayout.visibility = View.VISIBLE
        if (songUri == null) {
            Log.e("Recent2Fragment", "Invalid URI: ${song.title}")
            return
        }

        mediaPlayerViewModel.playSong(songUri, requireContext())
        songTitleTextView.text = song.title
        playButton.setImageResource(android.R.drawable.ic_media_pause)
        isPlaying = true
        loadAlbumCover(songUri.toString())
    }

    private fun loadAlbumCover(uriString: String) {
        val uri = Uri.parse(uriString)
        val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
        val cursor: Cursor? = requireContext().contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val albumId = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                albumCoverImageView.setImageURI(albumArtUri)
            }
        }
    }

    private fun openPlayerFragment() {
        val songUriString = recentSongsViewModel.recentSongs.value?.get(currentSongIndex)?.id
        if (songUriString != null) {
            val fragment = PlayerFragment.newInstance(getSongName(songUriString), songUriString, null)
            (activity as MainActivity).hideUiElements()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun getSongUri(songName: String): Uri? {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.TITLE} LIKE ?"
        val selectionArgs = arrayOf("%$songName%")
        val cursor: Cursor? = requireContext().contentResolver.query(
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

    private fun getSongName(uriString: String): String {
        val uri = Uri.parse(uriString)
        var songName = "Unknown"
        val projection = arrayOf(MediaStore.Audio.Media.TITLE)
        val cursor: Cursor? = requireContext().contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                if (titleIndex != -1) {
                    songName = it.getString(titleIndex)
                }
            }
        }
        return songName
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}