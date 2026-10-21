package com.example.habittracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.repository.HabitRepository
import com.example.habittracker.util.SampleData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _uiState = MutableStateFlow(DashboardUiState(userGreeting = computeGreeting()))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedDate
                .flatMapLatest { date ->
                    repository.getHabitsWithProgressForDate(date)
                }
                .collect { habitList ->
                    val completed = habitList.count { it.isCompleted }
                    val total = habitList.size
                    val maxStreak = habitList.maxOfOrNull { it.currentStreak } ?: 0

                    _uiState.update { currentState ->
                        currentState.copy(
                            habits = habitList,
                            completedCount = completed,
                            totalCount = total,
                            overallStreak = maxStreak,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun toggleHabitCompletion(habitId: Long) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, _selectedDate.value)
        }
    }

    fun toggleStreakFreeze(habitId: Long) {
        viewModelScope.launch {
            repository.toggleStreakFreeze(habitId, _selectedDate.value)
        }
    }

    fun toggleAllStreakFreeze() {
        viewModelScope.launch {
            val currentHabits = _uiState.value.habits
            if (currentHabits.isEmpty()) return@launch

            val isAnyFrozen = currentHabits.any { it.isFrozen }
            val targetHabits = if (isAnyFrozen) {
                currentHabits.filter { it.isFrozen }
            } else {
                currentHabits.filter { !it.isCompleted }
            }

            targetHabits.forEach { habitWithProgress ->
                repository.toggleStreakFreeze(habitWithProgress.habit.id, _selectedDate.value)
            }
        }
    }

    fun openTimer(habit: Habit) {
        _uiState.update { it.copy(isTimerOpen = true, habitForTimer = habit) }
    }

    fun closeTimer() {
        _uiState.update { it.copy(isTimerOpen = false, habitForTimer = null) }
    }

    fun onTimerFinished() {
        val habit = _uiState.value.habitForTimer
        if (habit != null) {
            viewModelScope.launch {
                repository.setHabitCompletionCount(habit.id, _selectedDate.value, habit.targetCount)
                closeTimer()
            }
        }
    }

    fun openCreateHabitSheet() {
        _uiState.update { it.copy(isFormSheetOpen = true, habitToEdit = null) }
    }

    fun openEditHabitSheet(habit: Habit) {
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

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            closeFormSheet()
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            repository.archiveHabit(habitId)
            closeFormSheet()
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            val sampleHabits = SampleData.getSampleHabits()
            sampleHabits.forEach { habit ->
                repository.saveHabit(habit)
            }
        }
    }

    private fun computeGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Good Morning ☀️"
            in 12..16 -> "Good Afternoon 🌤️"
            in 17..21 -> "Good Evening 🌙"
            else -> "Good Night ✨"
        }
    }
}
