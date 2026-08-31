package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PostEntity::class,
        StoryEntity::class,
        AlbumEntity::class,
        AlbumItemEntity::class,
        SourceConnectionEntity::class,
        CommentEntity::class,
        StoryHighlightEntity::class,
        PostTagEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MemoirDatabase : RoomDatabase() {
    abstract fun memoirDao(): MemoirDao

    companion object {
        @Volatile
        private var INSTANCE: MemoirDatabase? = null

        fun getDatabase(context: Context): MemoirDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoirDatabase::class.java,
                    "memoir_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
