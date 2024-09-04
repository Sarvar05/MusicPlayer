package com.example.m.songData

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_songs")
data class RecentSong(
    @PrimaryKey val id: String,
    val title: String
)
