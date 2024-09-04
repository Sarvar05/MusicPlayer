package com.example.m.songData

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao


interface RecentSongDao {
    @Query("SELECT * FROM recent_songs ORDER BY id DESC LIMIT 15")
    fun getRecentSongs(): LiveData<List<RecentSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSong(song: RecentSong)
}
