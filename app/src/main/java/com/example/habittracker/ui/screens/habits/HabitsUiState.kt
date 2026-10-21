package com.example.habittracker.ui.screens.habits

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory

data class HabitsUiState(
    val activeHabits: List<Habit> = emptyList(),
    val archivedHabits: List<Habit> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: HabitCategory? = null,
    val selectedTab: HabitTab = HabitTab.ACTIVE,
    val isFormSheetOpen: Boolean = false,
    val habitToEdit: Habit? = null,
    val habitToDelete: Habit? = null,
    val isDeleteDialogShowing: Boolean = false
)

enum class HabitTab {
    ACTIVE,
    ARCHIVED
}
