package com.example.habittracker.ui.screens.todo

import com.example.habittracker.domain.model.TodoTask
import java.time.LocalDate

data class TodoUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<TodoTask> = emptyList(),
    val isFormOpen: Boolean = false,
    val taskToEdit: TodoTask? = null
) {
    val completedCount: Int get() = tasks.count { it.isCompleted }
    val totalCount: Int get() = tasks.size
}
