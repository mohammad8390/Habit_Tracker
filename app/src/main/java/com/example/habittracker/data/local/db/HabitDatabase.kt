package com.example.habittracker.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.habittracker.data.local.db.dao.HabitDao
import com.example.habittracker.data.local.db.dao.TodoDao
import com.example.habittracker.data.local.db.entity.HabitEntity
import com.example.habittracker.data.local.db.entity.HabitLogEntity
import com.example.habittracker.data.local.db.entity.TodoTaskEntity

@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, TodoTaskEntity::class],
    version = 3,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        fun getDatabase(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit_tracker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
