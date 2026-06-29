package com.novelchat.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.novelchat.data.model.*

@Database(
    entities = [
        Novel::class,
        Role::class,
        Chapter::class,
        Segment::class,
        Message::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NovelDatabase : RoomDatabase() {

    abstract fun novelDao(): NovelDao

    companion object {
        @Volatile
        private var INSTANCE: NovelDatabase? = null

        fun getInstance(context: Context): NovelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovelDatabase::class.java,
                    "novelchat.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
