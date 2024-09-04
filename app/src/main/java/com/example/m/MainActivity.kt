package com.example.m

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.m.adapters.ViewPagerAdapter
import com.example.m.fragments.HomeFragment
import com.example.m.fragments.Recent2Fragment
import com.example.m.fragments.RecentFragment
import com.example.m.fragments.ResearchFragment
import com.example.m.models.MediaPlayerService1
import com.example.m.models.MediaPlayerService
import com.example.m.models.PlaylistViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private val STORAGE_PERMISSION_CODE = 1
    private val REQUEST_CODE = 1001
    private lateinit var playlistViewModel: PlaylistViewModel
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    var mediaPlayerService: MediaPlayerService? = null



    private val miniPlayerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val songName = intent.getStringExtra("SONG_NAME")
            songName?.let {
                updateMiniPlayer(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
        }



        loadThemePreference()

        setContentView(R.layout.activity_main)

        val filter = IntentFilter("ACTION_UPDATE_MINI_PLAYER")
        registerReceiver(miniPlayerReceiver, filter)


        val isPlaying = intent.getBooleanExtra("IS_PLAYING", false)
        val songName = intent.getStringExtra("SONG_NAME")
        val songUri = intent.getStringExtra("SONG_URI")?.let { Uri.parse(it) }

        if (isPlaying && songUri != null) {
            MediaPlayerService.startService(this, songUri, MediaPlayerService.ACTION_RESUME)
        }


        if (isPlaying && songName != null) {
            updateMiniPlayer(songName)
        }

        playlistViewModel = ViewModelProvider(this).get(PlaylistViewModel::class.java)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        fragmentContainer = findViewById(R.id.fragment_container)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        setupViewPagerAndTabs()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
        }

        createNotificationChannel()

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHomeFragment()

                    true
                }
                R.id.nav_search -> {
                    showResearchFragment()
                    true
                }
                else -> false
            }
        }



        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                showUiElements()
            }
        }

        val optionsButton: ImageButton = findViewById(R.id.options_button)
        optionsButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.theme_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.light_theme -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        saveThemePreference(false)
                        recreate()
                        true
                    }
                    R.id.dark_theme -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        saveThemePreference(true)
                        recreate()
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(miniPlayerReceiver)
    }

    private fun updateMiniPlayer(songName: String) {
        val fragment = supportFragmentManager.findFragmentByTag("HOME_FRAGMENT_TAG") as? HomeFragment
        fragment?.updateMiniPlayer(songName)
    }

    private fun setupViewPagerAndTabs() {
        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = (viewPager.adapter as ViewPagerAdapter).getPageTitle(position)
        }.attach()
    }

    private fun showResearchFragment() {

        viewPager.visibility = View.GONE
        tabLayout.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE


        supportFragmentManager.beginTransaction()
            .setCustomAnimations( R.anim.fade_in,R.anim.fade_out)
            .replace(R.id.fragment_container, ResearchFragment())
            .addToBackStack(null)
            .commit()
    }


    private fun showHomeFragment() {
        showUiElements()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
            .replace(R.id.fragment_container, HomeFragment(), "HOME_FRAGMENT_TAG")
            .addToBackStack(null)
            .commit()
    }


    fun hideUiElements() {
        viewPager.visibility = View.GONE
        tabLayout.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        bottomNavigationView.visibility = View.GONE
    }

    fun showUiElements() {
        viewPager.visibility = View.VISIBLE
        tabLayout.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE
        bottomNavigationView.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            showUiElements()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        showUiElements()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MediaPlayerService.CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun saveThemePreference(isDarkMode: Boolean) {
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("DARK_MODE", isDarkMode)
            apply()
        }
    }

    private fun loadThemePreference() {
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val researchFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (researchFragment is ResearchFragment) {
                    researchFragment.loadSongs()
                }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onStart() {
        super.onStart()

        val intent = Intent(this, MediaPlayerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaPlayerService = null
        }
    }
    private fun animateViewVisibility(view: View, show: Boolean) {
        Log.d("MainActivity", "Animating view visibility: ${view.id}, show: $show")
        val animation = if (show) {
            AnimationUtils.loadAnimation(this, R.anim.fade_in)
        } else {
            AnimationUtils.loadAnimation(this, R.anim.fade_out)
        }
        view.startAnimation(animation)
        view.visibility = if (show) View.VISIBLE else View.GONE
    }



    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

}
