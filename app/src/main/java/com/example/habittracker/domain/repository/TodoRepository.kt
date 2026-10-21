package com.example.habittracker.domain.repository

import com.example.habittracker.domain.model.TodoTask
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TodoRepository {
    fun getTasksForDate(date: LocalDate): Flow<List<TodoTask>>
    suspend fun saveTask(task: TodoTask): Long
    suspend fun toggleTaskCompletion(taskId: Long)
    suspend fun deleteTask(taskId: Long)
}
