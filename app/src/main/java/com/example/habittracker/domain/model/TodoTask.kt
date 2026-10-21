package com.example.habittracker.domain.model

import java.time.LocalDate

data class TodoTask(
    val id: Long = 0,
    val title: String,
    val dueDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val isCompleted: Boolean = false,
    val isReminderEnabled: Boolean = false,
    val reminderTimeString: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val dueDate: LocalDate get() = LocalDate.ofEpochDay(dueDateEpochDay)
}
