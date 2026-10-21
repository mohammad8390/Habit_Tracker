package com.example.habittracker.data.repository

import com.example.habittracker.data.local.db.dao.TodoDao
import com.example.habittracker.data.local.db.entity.TodoTaskEntity
import com.example.habittracker.domain.model.TodoTask
import com.example.habittracker.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TodoRepositoryImpl(
    private val todoDao: TodoDao
) : TodoRepository {

    override fun getTasksForDate(date: LocalDate): Flow<List<TodoTask>> {
        return todoDao.getTasksForDate(date.toEpochDay()).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun saveTask(task: TodoTask): Long {
        return todoDao.insertOrUpdateTask(TodoTaskEntity.fromDomainModel(task))
    }

    override suspend fun toggleTaskCompletion(taskId: Long) {
        todoDao.toggleTaskCompletion(taskId)
    }

    override suspend fun deleteTask(taskId: Long) {
        todoDao.deleteTask(taskId)
    }
}
