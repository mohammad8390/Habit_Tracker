package com.example.habittracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.local.db.entity.TodoTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todo_tasks WHERE dueDateEpochDay = :dateEpochDay ORDER BY isCompleted ASC, createdAtEpochMs DESC")
    fun getTasksForDate(dateEpochDay: Long): Flow<List<TodoTaskEntity>>

    @Query("SELECT * FROM todo_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TodoTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTask(task: TodoTaskEntity): Long

    @Query("UPDATE todo_tasks SET isCompleted = NOT isCompleted WHERE id = :taskId")
    suspend fun toggleTaskCompletion(taskId: Long)

    @Query("DELETE FROM todo_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)
}
