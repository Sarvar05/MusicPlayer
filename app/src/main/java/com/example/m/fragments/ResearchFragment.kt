package com.example.m.fragments

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ContextMenu
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.m.MainActivity
import com.example.m.R
import com.example.m.models.MediaPlayerViewModel
import com.example.m.models.RecentSongsViewModel
import com.example.m.songData.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class ResearchFragment : Fragment() {

    private lateinit var mediaPlayerViewModel: MediaPlayerViewModel
    private lateinit var recentSongsViewModel: RecentSongsViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var songListView: ListView
    private lateinit var songTitleTextView: TextView
    private lateinit var playButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var playerLayout: RelativeLayout
    private lateinit var searchView: SearchView
    private lateinit var albumCoverImageView: ImageView
    private var songList: ArrayList<String> = ArrayList()
    private var songUriList: ArrayList<String> = ArrayList()
    private var favouriteSongs: MutableSet<String> = mutableSetOf()
    private var currentSongIndex = -1
    private var currentPlayerPosition = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_research, container, false)

        songListView = view.findViewById(R.id.searchResultsListView)
        songTitleTextView = view.findViewById(R.id.song_title)
        playButton = view.findViewById(R.id.play_button)
        prevButton = view.findViewById(R.id.prev_button)
        nextButton = view.findViewById(R.id.next_button)
        playerLayout = view.findViewById(R.id.playerLayout)
        albumCoverImageView = playerLayout.findViewById(R.id.album_cover)
        searchView = view.findViewById(R.id.searchView)



        sharedPreferences = requireContext().getSharedPreferences("Favourites", Context.MODE_PRIVATE)
        mediaPlayerViewModel = ViewModelProvider(requireActivity()).get(MediaPlayerViewModel::class.java)
        recentSongsViewModel = ViewModelProvider(requireActivity()).get(RecentSongsViewModel::class.java)

        songTitleTextView.isSelected = true


        loadSongs()
        loadFavourites()


        val songAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, songList)
        songListView.adapter = songAdapter


        songListView.setOnItemClickListener { _, _, position, _ ->
            playSong(position)
        }



        registerForContextMenu(songListView)


        playButton.setOnClickListener {
            if (mediaPlayerViewModel.isPlaying.value == true) {
                mediaPlayerViewModel.pauseSong()
                playButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayerViewModel.resumeSong()
                playButton.setImageResource(android.R.drawable.ic_media_pause)
            }
        }


        prevButton.setOnClickListener {
            if (currentSongIndex > 0) {
                playSong(currentSongIndex - 1)
            }
        }


        nextButton.setOnClickListener {
            if (currentSongIndex < songList.size - 1) {
                playSong(currentSongIndex + 1)
            }
        }




        playerLayout.setOnClickListener {
            if (currentPlayerPosition >= 0) {
                openPlayerFragment()
            } else {
                Log.d("ResearchFragment", "No song is playing.")
            }
        }


        mediaPlayerViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            playButton.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        }

        mediaPlayerViewModel.currentSongName.observe(viewLifecycleOwner) { songName ->
            songTitleTextView.text = songName
        }

        mediaPlayerViewModel.currentSongUri.observe(viewLifecycleOwner) { uri ->
            val position = songUriList.indexOf(uri.toString())
            if (position != -1) {
                songTitleTextView.text = songList[position]
                currentSongIndex = position
                currentPlayerPosition = position
                updateMiniPlayer()
            }
        }

        // Handle fragment arguments (if any)
        arguments?.let {
            val songName = it.getString("SONG_NAME")
            val songUri = it.getString("SONG_URI")
            val isPlaying = it.getBoolean("IS_PLAYING", false)

            if (!songName.isNullOrEmpty() && !songUri.isNullOrEmpty()) {
                mediaPlayerViewModel.playSong(Uri.parse(songUri), requireContext())
                songTitleTextView.text = songName
                currentSongIndex = songUriList.indexOf(songUri)
                currentPlayerPosition = currentSongIndex
                mediaPlayerViewModel.setPlaying(isPlaying)
            }
        }
        searchView.requestFocus()
        searchView.isIconified = false
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)



        return view
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        requireActivity().menuInflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        return when (item.itemId) {
            R.id.remove_from_favourites -> {
                removeFromApp(info.position)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun openPlayerFragment() {
        val albumCover = getAlbumCover(songUriList[currentPlayerPosition])
        val fragment = PlayerFragment.newInstance(
            songName = songTitleTextView.text.toString(),
            songUri = songUriList[currentPlayerPosition],
            albumCover = albumCover
        )
        (activity as MainActivity).hideUiElements()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun loadSongs() {
        val songResolver = requireContext().contentResolver
        val songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val songCursor: Cursor? = songResolver.query(songUri, null, null, null, null)

        if (songCursor != null && songCursor.moveToFirst()) {
            val titleColumn = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val idColumn = songCursor.getColumnIndex(MediaStore.Audio.Media._ID)

            do {
                val thisId = songCursor.getLong(idColumn)
                val thisTitle = songCursor.getString(titleColumn)
                songList.add(thisTitle)
                songUriList.add(
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        thisId
                    ).toString()
                )
            } while (songCursor.moveToNext())
        }
    }

    private fun addCurrentSongToFavorites() {
        val currentUri = mediaPlayerViewModel.currentSongUri.value
        val currentTitle = mediaPlayerViewModel.currentSongName.value

        if (currentUri != null && currentTitle != null) {
            val song = Song(title = currentTitle, uri = currentUri)
            mediaPlayerViewModel.addSongToFavorites(song)
            Toast.makeText(requireContext(), "Song added to favorites", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No song is currently playing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFavourites() {
        val favouriteSetJson = sharedPreferences.getString("favouriteSongsSet", "")
        if (!favouriteSetJson.isNullOrEmpty()) {
            val gson = Gson()
            val type = object : TypeToken<Set<Song>>() {}.type
            val favouriteSet: Set<Song> = gson.fromJson(favouriteSetJson, type)
            favouriteSongs.addAll(favouriteSet.map { it.uri })
        }
    }

    private fun playSong(position: Int) {
        try {
            val songUri = songUriList[position]
            mediaPlayerViewModel.playSong(Uri.parse(songUri), requireContext())
            songTitleTextView.text = songList[position]
            currentSongIndex = position
            currentPlayerPosition = position
            playerLayout.visibility = View.VISIBLE
            updateMiniPlayer()
        } catch (e: Exception) {
            Log.e("ResearchFragment", "Error playing song or updating recent songs", e)
        }
    }

    private fun removeFromApp(position: Int) {
        val songUri = songUriList[position]

        try {
            val contentResolver = requireContext().contentResolver
            val deleteUri = Uri.parse(songUri)
            val rowsDeleted = contentResolver.delete(deleteUri, null, null)

            if (rowsDeleted > 0) {
                Log.i("ResearchFragment", "Song deleted from MediaStore: $songUri")
            } else {
                Log.w("ResearchFragment", "Song not found for deletion: $songUri")
            }

            // Remove song from list
            songList.removeAt(position)
            songUriList.removeAt(position)

            val adapter = songListView.adapter as ArrayAdapter<*>
            adapter.notifyDataSetChanged()

            Toast.makeText(requireContext(), "Song removed from app", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ResearchFragment", "Error removing song from app", e)
        }
    }

    private fun updateMiniPlayer() {
        val albumCover = getAlbumCover(songUriList[currentPlayerPosition])
        if (albumCover != null) {
            albumCoverImageView.setImageBitmap(albumCover)
        } else {

            albumCoverImageView.setImageDrawable(requireContext().getDrawable(R.drawable.ic_main))
        }
    }

    private fun getAlbumCover(uri: String): Bitmap? {
        val mediaUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val cursor = requireContext().contentResolver.query(mediaUri, null, null, null, null)
        cursor?.use {
            val albumIdColumn = it.getColumnIndex(MediaStore.Audio.Albums._ID)
            while (it.moveToNext()) {
                val albumId = it.getLong(albumIdColumn)
                val coverUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                return try {
                    val inputStream = requireContext().contentResolver.openInputStream(coverUri)
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

}

