package com.example.habittracker.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HabitsViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<HabitCategory?>(null)

    init {
        viewModelScope.launch {
            combine(
                repository.getAllActiveHabits(),
                repository.getArchivedHabits(),
                _searchQuery,
                _selectedCategory
            ) { activeList, archivedList, query, categoryFilter ->
                val filteredActive = filterHabits(activeList, query, categoryFilter)
                val filteredArchived = filterHabits(archivedList, query, categoryFilter)

                Pair(filteredActive, filteredArchived)
            }.collect { (active, archived) ->
                _uiState.update { state ->
                    state.copy(
                        activeHabits = active,
                        archivedHabits = archived
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategoryFilter(category: HabitCategory?) {
        _selectedCategory.value = category
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun selectTab(tab: HabitTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openCreateSheet() {
        _uiState.update { it.copy(isFormSheetOpen = true, habitToEdit = null) }
    }

    fun openEditSheet(habit: Habit) {
        _uiState.update { it.copy(isFormSheetOpen = true, habitToEdit = habit) }
    }

    fun closeFormSheet() {
        _uiState.update { it.copy(isFormSheetOpen = false, habitToEdit = null) }
    }

    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            repository.saveHabit(habit)
            closeFormSheet()
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            repository.archiveHabit(habitId)
        }
    }

    fun unarchiveHabit(habitId: Long) {
        viewModelScope.launch {
            repository.unarchiveHabit(habitId)
        }
    }

    fun promptDeleteHabit(habit: Habit) {
        _uiState.update { it.copy(habitToDelete = habit, isDeleteDialogShowing = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(habitToDelete = null, isDeleteDialogShowing = false) }
    }

    fun confirmDeleteHabit() {
        val habit = _uiState.value.habitToDelete ?: return
        viewModelScope.launch {
            repository.deleteHabit(habit.id)
            dismissDeleteDialog()
        }
    }

    private fun filterHabits(list: List<Habit>, query: String, category: HabitCategory?): List<Habit> {
        return list.filter { habit ->
            val matchesQuery = query.isBlank() ||
                    habit.title.contains(query, ignoreCase = true) ||
                    (habit.description?.contains(query, ignoreCase = true) == true)

            val matchesCategory = category == null || habit.category == category

            matchesQuery && matchesCategory
        }
    }
}
