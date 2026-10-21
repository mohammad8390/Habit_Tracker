package com.example.habittracker.domain.model

import java.time.LocalDate

data class HabitLog(
    val id: Long = 0,
    val habitId: Long,
    val dateEpochDay: Long,
    val completedCount: Int,
    val isCompleted: Boolean,
    val isFrozen: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay)
}
