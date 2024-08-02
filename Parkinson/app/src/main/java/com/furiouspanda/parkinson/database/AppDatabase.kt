package com.harsh.parkinson.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ResultTremor::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resultDao(): ResultDao

    companion object {
        private const val DATABASE_NAME = "app_database"

        // Singleton instance of the database
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .build()
        }
    }
}
