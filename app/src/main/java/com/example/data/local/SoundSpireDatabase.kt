package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Artist
import com.example.data.model.Comment
import com.example.data.model.Review
import com.example.data.model.UserProfile

@Database(
    entities = [UserProfile::class, Artist::class, Review::class, Comment::class],
    version = 1,
    exportSchema = false
)
abstract class SoundSpireDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun artistDao(): ArtistDao
    abstract fun reviewDao(): ReviewDao
    abstract fun commentDao(): CommentDao

    companion object {
        @Volatile
        private var INSTANCE: SoundSpireDatabase? = null

        fun getDatabase(context: Context): SoundSpireDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundSpireDatabase::class.java,
                    "soundspire_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
