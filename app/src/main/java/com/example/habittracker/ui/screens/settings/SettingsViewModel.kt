package com.example.habittracker.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.BackupRestoreManager
import com.example.habittracker.data.local.datastore.FirstDayOfWeekPref
import com.example.habittracker.data.local.datastore.ThemeMode
import com.example.habittracker.data.local.datastore.UserPreferencesRepository
import com.example.habittracker.domain.repository.HabitRepository
import com.example.habittracker.domain.repository.TodoRepository
import com.example.habittracker.util.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(
    private val repository: HabitRepository,
    private val context: Context,
    private val todoRepository: TodoRepository? = null
) : ViewModel() {

    private val userPrefsRepo = UserPreferencesRepository(context)
    private val backupRestoreManager = BackupRestoreManager(repository, todoRepository)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefsRepo.themeModeFlow.collect { theme ->
                _uiState.update { it.copy(themeMode = theme) }
            }
        }

        viewModelScope.launch {
            userPrefsRepo.firstDayOfWeekFlow.collect { firstDay ->
                _uiState.update { it.copy(firstDayOfWeek = firstDay) }
            }
        }

        viewModelScope.launch {
            userPrefsRepo.isHapticEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isHapticEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPrefsRepo.isNotificationsEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isNotificationPermissionGranted = enabled) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPrefsRepo.setThemeMode(mode)
        }
    }

    fun setFirstDayOfWeek(firstDay: FirstDayOfWeekPref) {
        viewModelScope.launch {
            userPrefsRepo.setFirstDayOfWeek(firstDay)
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepo.setHapticEnabled(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepo.setNotificationsEnabled(enabled)
        }
    }

    fun checkNotificationPermission() {
        viewModelScope.launch {
            val isEnabled = userPrefsRepo.isNotificationsEnabledFlow.firstOrNull() ?: true
            _uiState.update { it.copy(isNotificationPermissionGranted = isEnabled) }
        }
    }

    fun promptExportData() {
        viewModelScope.launch {
            val json = backupRestoreManager.exportDataToJson()
            _uiState.update { it.copy(isExportDialogShowing = true, exportJsonString = json) }
        }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(isExportDialogShowing = false, exportJsonString = null) }
    }

    fun promptImportDialog() {
        _uiState.update { it.copy(isImportDialogShowing = true, importInputText = "") }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(isImportDialogShowing = false, importInputText = "") }
    }

    fun updateImportInputText(text: String) {
        _uiState.update { it.copy(importInputText = text) }
    }

    fun confirmImportData() {
        viewModelScope.launch {
            val textToImport = _uiState.value.importInputText
            val (count, message) = backupRestoreManager.importDataFromJson(textToImport)
            _uiState.update {
                it.copy(
                    isImportDialogShowing = false,
                    statusMessage = message
                )
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            val sampleHabits = SampleData.getSampleHabits()
            sampleHabits.forEach { habit ->
                repository.saveHabit(habit)
            }
            _uiState.update { it.copy(statusMessage = "5 sample habits successfully loaded!") }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun promptResetData() {
        _uiState.update { it.copy(isResetConfirmShowing = true) }
    }

    fun dismissResetData() {
        _uiState.update { it.copy(isResetConfirmShowing = false) }
    }

    fun confirmResetData() {
        viewModelScope.launch {
            val allHabits = repository.getAllHabits().firstOrNull() ?: emptyList()
            allHabits.forEach { habit ->
                repository.deleteHabit(habit.id)
            }
            if (todoRepository != null) {
                val todoTasks = todoRepository.getTasksForDate(LocalDate.now()).firstOrNull() ?: emptyList()
                todoTasks.forEach { task ->
                    todoRepository.deleteTask(task.id)
                }
            }
            dismissResetData()
            _uiState.update { it.copy(statusMessage = "All app data cleared successfully!") }
        }
    }
}
