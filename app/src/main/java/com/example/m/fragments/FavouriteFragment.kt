package com.example.m.fragments
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.m.FavoriteDataBase.FavouriteViewModel
import com.example.m.MainActivity
import com.example.m.R
import com.example.m.adapters.SongAdapter
import com.example.m.models.MediaPlayerViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class FavouriteFragment : Fragment() {

    private lateinit var favListView: ListView
    private lateinit var songTitleTextView: TextView
    private lateinit var albumArtImageView: ShapeableImageView
    private lateinit var playerLayout: RelativeLayout
    private lateinit var mediaPlayerViewModel: MediaPlayerViewModel
    private lateinit var favouriteViewModel: FavouriteViewModel
    private lateinit var favouriteAdapter: ArrayAdapter<String>
    private val favouriteSongUris = mutableListOf<String>()
    private val favouriteSongs = mutableListOf<String>()
    private var currentSongIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favourite, container, false)

        favListView = view.findViewById(R.id.favListView)
        songTitleTextView = view.findViewById(R.id.song_title)
        albumArtImageView = view.findViewById(R.id.album_cover)
        playerLayout = view.findViewById(R.id.playerLayout)


        mediaPlayerViewModel = ViewModelProvider(requireActivity()).get(MediaPlayerViewModel::class.java)
        favouriteViewModel = ViewModelProvider(requireActivity()).get(FavouriteViewModel::class.java)

        favouriteAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, favouriteSongs)
        favListView.adapter = favouriteAdapter


        val playButton: ImageButton = view.findViewById(R.id.play_button)
        val prevButton: ImageButton = view.findViewById(R.id.prev_button)
        val nextButton: ImageButton = view.findViewById(R.id.next_button)

        playButton.setOnClickListener {
            togglePlayPause()
        }

        prevButton.setOnClickListener {
            playPreviousSong()
        }

        nextButton.setOnClickListener {
            playNextSong()
        }

        favListView.setOnItemClickListener { _, _, position, _ ->
            playFavouriteSong(position)
        }


        songTitleTextView.isSelected = true
        loadFavourites()

        mediaPlayerViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            playButton.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        }

        mediaPlayerViewModel.currentSongUri.observe(viewLifecycleOwner) { uriString ->
            updateCurrentSong(uriString)
        }

        playerLayout.setOnClickListener {
            openPlayerFragment()
        }

        favouriteViewModel.favouriteSongs.observe(viewLifecycleOwner) { songs ->
            favouriteSongs.clear()
            favouriteSongs.addAll(songs)
            favouriteAdapter.notifyDataSetChanged()
            Log.d("FavouriteFragment", "Favourite songs: $favouriteSongs")
            Log.d("FavouriteFragment", "Favourite song URIs: ${favouriteViewModel.favouriteSongUris.value}")
        }


        return view
    }

    private fun togglePlayPause() {
        val isPlaying = mediaPlayerViewModel.isPlaying.value ?: false
        if (isPlaying) {
            mediaPlayerViewModel.pauseSong()
        } else {
            mediaPlayerViewModel.resumeSong()
        }
    }

    private fun playPreviousSong() {
        if (currentSongIndex > 0) {
            playFavouriteSong(currentSongIndex - 1)
        }
    }

    private fun playNextSong() {
        if (currentSongIndex < favouriteSongs.size - 1) {
            playFavouriteSong(currentSongIndex + 1)
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

    private fun loadFavourites() {
        favouriteViewModel.favouriteSongs.observe(viewLifecycleOwner) { songs ->
            favouriteSongs.clear()
            favouriteSongs.addAll(songs)
            favouriteAdapter.notifyDataSetChanged()
        }
    }

    private fun playFavouriteSong(position: Int) {
        if (position < 0 || position >= favouriteSongs.size) {
            Log.e("FavouriteFragment", "Invalid position: $position")
            return
        }

        val songTitle = favouriteSongs[position]
        val songUri = getUriFromSongName(songTitle)

        if (songUri == null || !fileExists(Uri.parse(songUri))) {
            Log.e("FavouriteFragment", "URI not found for song: $songTitle")
            return
        }

        currentSongIndex = position
        songTitleTextView.text = songTitle
        playerLayout.visibility = View.VISIBLE

        mediaPlayerViewModel.playSong(Uri.parse(songUri), requireContext())
        mediaPlayerViewModel.updateNotification(songTitle, "Artist", R.drawable.ic_music_note)

        setAlbumArt(Uri.parse(songUri))
    }


    private fun updatePlayButton(isPlaying: Boolean) {
        val playButton: ImageButton = view?.findViewById(R.id.play_button) ?: return
        playButton.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun updateCurrentSong(uriString: String?) {
        uriString?.let {
            val songUri = Uri.parse(it)
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(requireContext(), songUri)
            val albumArt = metadataRetriever.embeddedPicture

            if (albumArt != null) {
                val bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)
                albumArtImageView.setImageBitmap(bitmap)
            } else {
                albumArtImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_main))
            }
        }
    }

    private fun setAlbumArt(uri: Uri) {
        try {
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(requireContext(), uri)
            val albumArt = metadataRetriever.embeddedPicture

            if (albumArt != null) {
                val bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)
                albumArtImageView.setImageBitmap(bitmap)
            } else {
                albumArtImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_main))
            }
        } catch (e: Exception) {
            Log.e("FavouriteFragment", "Error setting album art", e)
            albumArtImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_main))
        }
    }


    private fun fileExists(uri: Uri): Boolean {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            false
        }
    }




    private fun getUriFromSongName(songName: String): String? {
        val index = favouriteSongs.indexOf(songName)
        if (index != -1) {
            val uri = favouriteViewModel.favouriteSongUris.value?.get(index)
            Log.d("FavouriteFragment", "Found URI for $songName: $uri")
            return uri
        }
        Log.e("FavouriteFragment", "URI not found for $songName")
        return null
    }


}
