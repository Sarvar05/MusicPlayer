package com.example.m.songData

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecentSong::class], version = 1)
abstract class AppDatabaseResent : RoomDatabase() {
    abstract fun recentSongDao(): RecentSongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabaseResent? = null

        fun getDatabase(context: Context): AppDatabaseResent {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabaseResent::class.java,
                    "recent_song_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
