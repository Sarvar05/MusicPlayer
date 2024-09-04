package com.example.m.fragments

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.m.FavoriteDataBase.FavouriteViewModel
import com.example.m.MainActivity
import com.example.m.R
import com.example.m.adapters.SongAdapter
import com.example.m.models.MediaPlayerViewModel
import com.example.m.models.PlayerViewModel
import com.example.m.models.RecentSongsViewModel
import com.example.m.songData.RecentSong
import com.example.m.songData.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class HomeFragment : Fragment() {

    private lateinit var mediaPlayerViewModel: MediaPlayerViewModel
    private lateinit var recentSongsViewModel: RecentSongsViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var songListView: ListView
    private lateinit var songTitleTextView: TextView
    private lateinit var playButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var add_to_favorite: ImageButton
    private lateinit var playerLayout: RelativeLayout
    private lateinit var albumCoverImageView: ImageView
    private var songList: ArrayList<String> = ArrayList()
    private var songUriList: ArrayList<String> = ArrayList()
    private var favouriteSongs: MutableSet<String> = mutableSetOf()
    private var currentSongIndex = -1
    private var currentPlayerPosition = -1
    private lateinit var songAdapter: SongAdapter
    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var favouriteViewModel: FavouriteViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        playerViewModel = ViewModelProvider(requireActivity()).get(PlayerViewModel::class.java)

        songListView = view.findViewById(R.id.songListView)
        songTitleTextView = view.findViewById(R.id.song_title)
        playButton = view.findViewById(R.id.play_button)
        prevButton = view.findViewById(R.id.prev_button)
        nextButton = view.findViewById(R.id.next_button)
        add_to_favorite = view.findViewById(R.id.ic_add_to_favourite)
        playerLayout = view.findViewById(R.id.playerLayout)
        albumCoverImageView = playerLayout.findViewById(R.id.album_cover)


        sharedPreferences =
            requireContext().getSharedPreferences("Favourites", Context.MODE_PRIVATE)
        mediaPlayerViewModel =
            ViewModelProvider(requireActivity()).get(MediaPlayerViewModel::class.java)
        recentSongsViewModel =
            ViewModelProvider(requireActivity()).get(RecentSongsViewModel::class.java)
        favouriteViewModel =
            ViewModelProvider(requireActivity()).get(FavouriteViewModel::class.java)


        val addToFavouriteButton = view.findViewById<ImageButton>(R.id.ic_add_to_favourite)


       
        songTitleTextView.isSelected = true

      
        loadSongs()
        loadFavourites()

       
        val songAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, songList)
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
        add_to_favorite.setOnClickListener {
            addCurrentSongToFavorites()

        }

       
        playerLayout.setOnClickListener {
            if (currentPlayerPosition >= 0) {
                openPlayerFragment()
            } else {
                Log.d("HomeFragment", "No song is playing.")
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

        (activity as? MainActivity)?.hideUiElements()
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


    private fun loadSongs() {
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

    fun updateMiniPlayer(songName: String) {

    }


    private fun addCurrentSongToFavorites() {
        val currentUri = mediaPlayerViewModel.currentSongUri.value
        val currentTitle = mediaPlayerViewModel.currentSongName.value

        if (currentUri != null && currentTitle != null) {
            favouriteViewModel.addFavourite(currentTitle, currentUri.toString())
            Toast.makeText(requireContext(), "Song added to favorites", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No song is currently playing", Toast.LENGTH_SHORT)
                .show()
        }
    }


    fun isUriValid(uri: Uri): Boolean {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            return it.count > 0
        }
        return false
    }


    private fun loadFavourites() {
        val favouriteSetJson = sharedPreferences.getString("favouriteSongsSet", "")
        if (!favouriteSetJson.isNullOrEmpty()) {
            val gson = Gson()
            val type = object : TypeToken<Set<Song>>() {}.type
            val favouriteSet: Set<Song> = gson.fromJson(favouriteSetJson, type)
            favouriteSet.forEach { song ->
                if (isUriValid(Uri.parse(song.uri))) {
                    favouriteSongs.add(song.uri)
                } else {
                    Log.e("FavouriteFragment", "URI не найден: ${song.uri}")
                }
            }
        }
    }


    private fun playSong(position: Int) {
        try {
            val songName = songList[position]
            val songUri = songUriList[position]
            val recentSong = RecentSong(id = songUri, title = songName)


            mediaPlayerViewModel.playSong(Uri.parse(songUri), requireContext())
            songTitleTextView.text = songName
            currentSongIndex = position
            currentPlayerPosition = position

            recentSongsViewModel.addRecentSong(recentSong)


            playerLayout.visibility = View.VISIBLE
            updateMiniPlayer()

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error playing song or updating recent songs", e)
        }
    }


    private fun addToFavourites(position: Int) {
        val songUri = songUriList[position]
        val songName = songList[position]
        Log.d("HomeFragment", "Adding to favorites: $songUri")

        favouriteSongs.add(songUri)

        val editor = sharedPreferences.edit()
        val favouriteMap = favouriteSongs.associateBy({ it }, { songName })
        editor.putString("favouriteSongsMap", Gson().toJson(favouriteMap))
        editor.apply()

        Log.d(
            "HomeFragment",
            "Favorites after adding: ${sharedPreferences.getString("favouriteSongsMap", "")}"
        )
    }

    private fun removeFromApp(position: Int) {
        val songUri = songUriList[position]
        val songName = songList[position]

        try {
            val contentResolver = requireContext().contentResolver
            val deleteUri = Uri.parse(songUri)
            val rowsDeleted = contentResolver.delete(deleteUri, null, null)

            if (rowsDeleted > 0) {
                Log.i("HomeFragment", "Song deleted from MediaStore: $songUri")
            } else {
                Log.w("HomeFragment", "Song not found for deletion: $songUri")
            }


            songList.removeAt(position)
            songUriList.removeAt(position)

            val adapter = songListView.adapter as ArrayAdapter<*>
            adapter.notifyDataSetChanged()


            if (favouriteSongs.contains(songUri)) {
                removeFromFavourites(songUri)
            }

        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to delete song from MediaStore", e)
        }
    }

    private fun removeFromFavourites(songUri: String) {
        favouriteSongs.remove(songUri)

        val editor = sharedPreferences.edit()
        val favouriteMap = favouriteSongs.associateBy({ it }, { songUriList.indexOf(it) })
        editor.putString("favouriteSongsMap", Gson().toJson(favouriteMap))
        editor.apply()

        Log.d(
            "HomeFragment",
            "Favorites after removal: ${sharedPreferences.getString("favouriteSongsMap", "")}"
        )
    }

    private fun updateMiniPlayer() {
        val currentSongUri = songUriList[currentPlayerPosition]
        val albumCover = getAlbumCover(currentSongUri)
        albumCoverImageView.setImageBitmap(
            albumCover ?: requireContext().getDrawable(R.drawable.ic_main)?.toBitmap()
        )
    }

    private fun getAlbumCover(uri: String): Bitmap? {
        if (!isUriValid(Uri.parse(uri))) {
            Log.e("HomeFragment", "Invalid URI: $uri")
            return null
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(requireContext(), Uri.parse(uri))
            val art = retriever.embeddedPicture
            art?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error retrieving album art", e)
            null
        } finally {
            retriever.release()
        }
    }


}
