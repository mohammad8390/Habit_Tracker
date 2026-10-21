package com.example.habittracker.domain.model

data class HabitWithProgress(
    val habit: Habit,
    val completedCount: Int = 0,
    val isCompleted: Boolean = false,
    val isFrozen: Boolean = false,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
) {
    val progressFraction: Float
        get() = if (habit.targetCount > 0) {
            (completedCount.toFloat() / habit.targetCount.toFloat()).coerceIn(0f, 1f)
        } else 0f
}
