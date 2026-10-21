package com.example.habittracker.ui.navigation

sealed class ScreenRoute(val route: String) {
    object Dashboard : ScreenRoute("dashboard")
    object Habits : ScreenRoute("habits")
    object Statistics : ScreenRoute("statistics")
    object Settings : ScreenRoute("settings")
    object HabitDetail : ScreenRoute("habit_detail/{habitId}") {
        fun createRoute(habitId: Long) = "habit_detail/$habitId"
    }
}
