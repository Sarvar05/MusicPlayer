package com.example.m.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.m.songData.Song

class SharedViewModel : ViewModel() {
    val songList = MutableLiveData<List<Song>>()

}
