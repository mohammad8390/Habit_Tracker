package com.example.habittracker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.habittracker.domain.model.TodoTask

@Entity(tableName = "todo_tasks")
data class TodoTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dueDateEpochDay: Long,
    val isCompleted: Boolean,
    val isReminderEnabled: Boolean,
    val reminderTimeString: String?,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): TodoTask {
        return TodoTask(
            id = id,
            title = title,
            dueDateEpochDay = dueDateEpochDay,
            isCompleted = isCompleted,
            isReminderEnabled = isReminderEnabled,
            reminderTimeString = reminderTimeString,
            createdAtEpochMs = createdAtEpochMs
        )
    }

    companion object {
        fun fromDomainModel(task: TodoTask): TodoTaskEntity {
            return TodoTaskEntity(
                id = task.id,
                title = task.title,
                dueDateEpochDay = task.dueDateEpochDay,
                isCompleted = task.isCompleted,
                isReminderEnabled = task.isReminderEnabled,
                reminderTimeString = task.reminderTimeString,
                createdAtEpochMs = task.createdAtEpochMs
            )
        }
    }
}
