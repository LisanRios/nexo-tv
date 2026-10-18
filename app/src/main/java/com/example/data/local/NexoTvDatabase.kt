package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        FavoriteEntity::class,
        CategoryEntity::class,
        SitePolicyEntity::class,
        AgendaSourceEntity::class,
        SportEventEntity::class,
        EventLinkEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NexoTvDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun sitePolicyDao(): SitePolicyDao
    abstract fun agendaSourceDao(): AgendaSourceDao
    abstract fun agendaDao(): AgendaDao

    companion object {
        @Volatile
        private var INSTANCE: NexoTvDatabase? = null

        fun getInstance(context: Context): NexoTvDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexoTvDatabase::class.java,
                    "nexotv.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
