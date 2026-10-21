package com.example.habittracker.ui.screens.dashboard

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitWithProgress
import java.time.LocalDate

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val habits: List<HabitWithProgress> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val overallStreak: Int = 0,
    val isLoading: Boolean = false,
    val isFormSheetOpen: Boolean = false,
    val habitToEdit: Habit? = null,
    val habitForTimer: Habit? = null,
    val isTimerOpen: Boolean = false,
    val userGreeting: String = "Good day"
) {
    val isAnyHabitFrozen: Boolean
        get() = habits.any { it.isFrozen }
}
