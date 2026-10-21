package com.example.habittracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.habittracker.ui.screens.dashboard.DashboardScreen
import com.example.habittracker.ui.screens.dashboard.DashboardViewModel
import com.example.habittracker.ui.screens.habits.HabitsViewModel
import com.example.habittracker.ui.screens.settings.SettingsScreen
import com.example.habittracker.ui.screens.settings.SettingsViewModel
import com.example.habittracker.ui.screens.statistics.StatisticsScreen
import com.example.habittracker.ui.screens.statistics.StatisticsViewModel
import com.example.habittracker.ui.screens.todo.TodoScreen
import com.example.habittracker.ui.screens.todo.TodoViewModel

@Composable
fun NavGraph(
    dashboardViewModel: DashboardViewModel,
    todoViewModel: TodoViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            AppBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(ScreenRoute.Dashboard.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ScreenRoute.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ScreenRoute.Dashboard.route) {
                val uiState by dashboardViewModel.uiState.collectAsState()

                DashboardScreen(
                    uiState = uiState,
                    onSelectDate = dashboardViewModel::selectDate,
                    onToggleHabitCompletion = dashboardViewModel::toggleHabitCompletion,
                    onToggleFreeze = dashboardViewModel::toggleStreakFreeze,
                    onToggleFreezeAll = dashboardViewModel::toggleAllStreakFreeze,
                    onHabitClick = { habitId ->
                        val habit = uiState.habits.find { it.habit.id == habitId }?.habit
                        if (habit != null) {
                            dashboardViewModel.openEditHabitSheet(habit)
                        }
                    },
                    onStartTimer = dashboardViewModel::openTimer,
                    onTimerFinished = dashboardViewModel::onTimerFinished,
                    onCloseTimer = dashboardViewModel::closeTimer,
                    onAddHabitClick = dashboardViewModel::openCreateHabitSheet,
                    onSaveHabit = dashboardViewModel::saveHabit,
                    onDeleteHabit = dashboardViewModel::deleteHabit,
                    onArchiveHabit = dashboardViewModel::archiveHabit,
                    onCloseFormSheet = dashboardViewModel::closeFormSheet,
                    onSettingsClick = {
                        navController.navigate(ScreenRoute.Settings.route)
                    },
                    onLoadSampleData = dashboardViewModel::loadSampleData
                )
            }

            composable(ScreenRoute.Habits.route) {
                val todoUiState by todoViewModel.uiState.collectAsState()

                TodoScreen(
                    uiState = todoUiState,
                    onSelectDate = todoViewModel::selectDate,
                    onToggleTaskCompletion = todoViewModel::toggleTaskCompletion,
                    onDeleteTask = todoViewModel::deleteTask,
                    onAddTaskClick = todoViewModel::openCreateForm,
                    onEditTaskClick = todoViewModel::openEditForm,
                    onSaveTask = todoViewModel::saveTask,
                    onCloseForm = todoViewModel::closeForm
                )
            }

            composable(ScreenRoute.Statistics.route) {
                val statsUiState by statisticsViewModel.uiState.collectAsState()

                StatisticsScreen(
                    uiState = statsUiState,
                    onPeriodSelect = statisticsViewModel::selectPeriod,
                    onDateSelect = statisticsViewModel::selectDate,
                    onYearMonthChange = statisticsViewModel::changeYearMonth
                )
            }

            composable(ScreenRoute.Settings.route) {
                val settingsUiState by settingsViewModel.uiState.collectAsState()

                SettingsScreen(
                    uiState = settingsUiState,
                    onThemeChange = settingsViewModel::setThemeMode,
                    onFirstDayOfWeekChange = settingsViewModel::setFirstDayOfWeek,
                    onHapticToggle = settingsViewModel::setHapticEnabled,
                    onNotificationToggle = settingsViewModel::setNotificationsEnabled,
                    onCheckPermission = settingsViewModel::checkNotificationPermission,
                    onPromptExport = settingsViewModel::promptExportData,
                    onDismissExport = settingsViewModel::dismissExportDialog,
                    onPromptImport = settingsViewModel::promptImportDialog,
                    onDismissImport = settingsViewModel::dismissImportDialog,
                    onImportTextChange = settingsViewModel::updateImportInputText,
                    onConfirmImport = settingsViewModel::confirmImportData,
                    onLoadSampleData = settingsViewModel::loadSampleData,
                    onClearStatusMessage = settingsViewModel::clearStatusMessage,
                    onPromptReset = settingsViewModel::promptResetData,
                    onDismissReset = settingsViewModel::dismissResetData,
                    onConfirmReset = settingsViewModel::confirmResetData
                )
            }
        }
    }
}
