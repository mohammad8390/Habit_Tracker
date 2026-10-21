package com.example.habittracker.ui.screens.todo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.notification.HabitReminderScheduler
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.TodoTask
import com.example.habittracker.domain.repository.TodoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModel(
    private val repository: TodoRepository,
    private val context: Context
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedDate
                .flatMapLatest { date ->
                    repository.getTasksForDate(date)
                }
                .collect { taskList ->
                    _uiState.update { currentState ->
                        currentState.copy(tasks = taskList)
                    }
                }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId)
            HabitReminderScheduler(context).cancelReminder(taskId + 90000)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            HabitReminderScheduler(context).cancelReminder(taskId + 90000)
        }
    }

    fun openCreateForm() {
        _uiState.update { it.copy(isFormOpen = true, taskToEdit = null) }
    }

    fun openEditForm(task: TodoTask) {
        _uiState.update { it.copy(isFormOpen = true, taskToEdit = task) }
    }

    fun closeForm() {
        _uiState.update { it.copy(isFormOpen = false, taskToEdit = null) }
    }

    fun saveTask(title: String, isReminderEnabled: Boolean, reminderTime: String?) {
        viewModelScope.launch {
            val currentTask = _uiState.value.taskToEdit
            val newTask = (currentTask ?: TodoTask(title = title.trim())).copy(
                title = title.trim(),
                dueDateEpochDay = _selectedDate.value.toEpochDay(),
                isReminderEnabled = isReminderEnabled,
                reminderTimeString = if (isReminderEnabled) reminderTime else null
            )
            val taskId = repository.saveTask(newTask)

            if (isReminderEnabled && reminderTime != null) {
                val tempHabit = Habit(
                    id = taskId + 90000,
                    title = "To-Do: ${newTask.title}",
                    isReminderEnabled = true,
                    reminderTimeString = reminderTime
                )
                HabitReminderScheduler(context).scheduleOrUpdateReminder(tempHabit)
            } else {
                HabitReminderScheduler(context).cancelReminder(taskId + 90000)
            }

            closeForm()
        }
    }
}
