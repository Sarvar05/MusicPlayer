package com.example.m.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.m.MainActivity
import com.example.m.R
import com.example.m.models.MediaPlayerViewModel
import com.example.m.models.PlayerViewModel

class PlayerFragment : Fragment() {

    companion object {
        private const val ARG_SONG_NAME = "songName"
        private const val ARG_SONG_URI = "songUri"
        private const val ARG_ALBUM_COVER = "albumCover"

        fun newInstance(
            songName: String, songUri: String, albumCover: Bitmap?
        ): PlayerFragment {
            val fragment = PlayerFragment()
            val args = Bundle().apply {
                putString(ARG_SONG_NAME, songName)
                putString(ARG_SONG_URI, songUri)
                putParcelable(ARG_ALBUM_COVER, albumCover)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var mediaPlayerViewModel: MediaPlayerViewModel
    private lateinit var albumArtImageView: ImageView
    private lateinit var songNameTextView: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var audioManager: AudioManager
    private lateinit var tenSecBack: ImageButton
    private lateinit var tenSecNext: ImageButton
    private var seekBarUpdateHandler: Handler? = null
    private var seekBarUpdateRunnable: Runnable? = null
    private var songUri: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player, container, false)

        playerViewModel = ViewModelProvider(requireActivity()).get(PlayerViewModel::class.java)
        mediaPlayerViewModel = ViewModelProvider(requireActivity()).get(MediaPlayerViewModel::class.java)

        albumArtImageView = view.findViewById(R.id.album_art)
        songNameTextView = view.findViewById(R.id.song_title)
        playPauseButton = view.findViewById(R.id.play_button)
        nextButton = view.findViewById(R.id.next_button)
        previousButton = view.findViewById(R.id.back_button)
        seekBar = view.findViewById(R.id.progress_bar)
        currentTime = view.findViewById(R.id.current_time)
        totalTime = view.findViewById(R.id.total_time)
        volumeSeekBar = view.findViewById(R.id.volume_seek_bar)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tenSecBack = view.findViewById(R.id.back_10)
        tenSecNext = view.findViewById(R.id.next_10)

        arguments?.let {
            songNameTextView.text = it.getString(ARG_SONG_NAME)
            songUri = it.getString(ARG_SONG_URI) ?: ""
            val albumCover = it.getParcelable<Bitmap>(ARG_ALBUM_COVER)
            albumArtImageView.setImageBitmap(albumCover ?: getDefaultAlbumCover())
        }

        songNameTextView.isSelected = true

        setupUiControls()
        observeViewModel()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            (activity as MainActivity).showUiElements()
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun setupUiControls() {
        playPauseButton.setOnClickListener {
            if (mediaPlayerViewModel.isPlaying.value == true) {
                mediaPlayerViewModel.pauseSong()
                playPauseButton.setImageResource(R.drawable.ic_playe)
                stopSeekBarUpdates()
            } else {
                mediaPlayerViewModel.resumeSong()
                playPauseButton.setImageResource(R.drawable.ic_pause)
                startSeekBarUpdates()
            }
        }

        previousButton.setOnClickListener {
            mediaPlayerViewModel.playPrevious()
        }

        tenSecBack.setOnClickListener {
            mediaPlayerViewModel.rewind(10000)
        }

        tenSecNext.setOnClickListener {
            mediaPlayerViewModel.forward(10000)
        }

        nextButton.setOnClickListener {
            mediaPlayerViewModel.playNext()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayerViewModel.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volume = progress / volumeSeekBar.max.toFloat()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun observeViewModel() {
        mediaPlayerViewModel.currentSongName.observe(viewLifecycleOwner) { songName ->
            songNameTextView.text = songName ?: "Unknown"
        }

        mediaPlayerViewModel.currentPosition.observe(viewLifecycleOwner) { position ->
            seekBar.progress = position
            currentTime.text = formatTime(position)
        }

        mediaPlayerViewModel.songDuration.observe(viewLifecycleOwner) { duration ->
            seekBar.max = duration
            totalTime.text = formatTime(duration)
        }

        mediaPlayerViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            playPauseButton.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_playe)
            if (isPlaying) {
                startSeekBarUpdates()
            } else {
                stopSeekBarUpdates()
            }
        }

        mediaPlayerViewModel.albumArt.observe(viewLifecycleOwner) { albumArt ->
            albumArtImageView.setImageBitmap(albumArt ?: getDefaultAlbumCover())
        }
    }

    private fun startSeekBarUpdates() {
        stopSeekBarUpdates()

        seekBarUpdateHandler = Handler(Looper.getMainLooper())
        seekBarUpdateRunnable = object : Runnable {
            override fun run() {
                mediaPlayerViewModel.updatePosition()
                seekBarUpdateHandler?.postDelayed(this, 1000L)
            }
        }
        seekBarUpdateHandler?.post(seekBarUpdateRunnable!!)
    }

    private fun stopSeekBarUpdates() {
        seekBarUpdateHandler?.removeCallbacks(seekBarUpdateRunnable!!)
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun getDefaultAlbumCover(): Bitmap {
        return BitmapFactory.decodeResource(resources, R.drawable.ic_main)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSeekBarUpdates()
    }
}
