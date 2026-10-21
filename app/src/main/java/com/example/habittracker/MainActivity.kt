package com.example.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habittracker.data.local.db.HabitDatabase
import com.example.habittracker.data.notification.HabitReminderScheduler
import com.example.habittracker.data.notification.NotificationHelper
import com.example.habittracker.data.repository.HabitRepositoryImpl
import com.example.habittracker.data.repository.TodoRepositoryImpl
import com.example.habittracker.ui.navigation.NavGraph
import com.example.habittracker.ui.screens.dashboard.DashboardViewModel
import com.example.habittracker.ui.screens.settings.SettingsViewModel
import com.example.habittracker.ui.screens.statistics.StatisticsViewModel
import com.example.habittracker.ui.screens.todo.TodoViewModel
import com.example.habittracker.ui.theme.HabitTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Stop continuous alarm ringtone if opened via notification click
        NotificationHelper.stopLoopingAlarm()
        NotificationHelper.createNotificationChannel(applicationContext)

        val database = HabitDatabase.getDatabase(applicationContext)
        val reminderScheduler = HabitReminderScheduler(applicationContext)
        val repository = HabitRepositoryImpl(database.habitDao(), reminderScheduler, applicationContext)
        val todoRepository = TodoRepositoryImpl(database.todoDao())

        setContent {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return when {
                        modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
                        modelClass.isAssignableFrom(TodoViewModel::class.java) -> TodoViewModel(todoRepository, applicationContext) as T
                        modelClass.isAssignableFrom(StatisticsViewModel::class.java) -> StatisticsViewModel(repository) as T
                        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository, applicationContext, todoRepository) as T
                        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                    }
                }
            }

            val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
            val todoViewModel: TodoViewModel = viewModel(factory = factory)
            val statisticsViewModel: StatisticsViewModel = viewModel(factory = factory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

            val settingsUiState by settingsViewModel.uiState.collectAsState()

            HabitTrackerTheme(themeMode = settingsUiState.themeMode) {
                NavGraph(
                    dashboardViewModel = dashboardViewModel,
                    todoViewModel = todoViewModel,
                    statisticsViewModel = statisticsViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NotificationHelper.stopLoopingAlarm()
    }
}
