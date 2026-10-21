package com.example.habittracker.util

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitFrequencyType
import java.time.LocalDate

object SampleData {

    fun getSampleHabits(): List<Habit> {
        val todayEpochDay = LocalDate.now().toEpochDay()
        val startEpochDay = todayEpochDay - 14 // Created 2 weeks ago

        return listOf(
            Habit(
                id = 0,
                title = "Drink 2L Water",
                description = "Stay hydrated throughout the day",
                category = HabitCategory.HEALTH,
                colorHex = "#059669",
                iconName = "LocalDrink",
                frequencyType = HabitFrequencyType.DAILY,
                targetCount = 4,
                unit = "glasses",
                isReminderEnabled = true,
                reminderTimeString = "09:00",
                createdAtEpochDay = startEpochDay
            ),
            Habit(
                id = 0,
                title = "30 Min Morning Run",
                description = "Cardio session at the local park",
                category = HabitCategory.FITNESS,
                colorHex = "#E11D48",
                iconName = "DirectionsRun",
                frequencyType = HabitFrequencyType.WEEKLY_DAYS,
                targetDaysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
                targetCount = 1,
                unit = "session",
                isReminderEnabled = true,
                reminderTimeString = "07:00",
                createdAtEpochDay = startEpochDay
            ),
            Habit(
                id = 0,
                title = "Read 15 Pages",
                description = "Non-fiction books or technical docs",
                category = HabitCategory.LEARNING,
                colorHex = "#4F46E5",
                iconName = "MenuBook",
                frequencyType = HabitFrequencyType.DAILY,
                targetCount = 15,
                unit = "pages",
                isReminderEnabled = false,
                createdAtEpochDay = startEpochDay
            ),
            Habit(
                id = 0,
                title = "10 Min Meditation",
                description = "Mindful breathing and relaxation",
                category = HabitCategory.MINDFULNESS,
                colorHex = "#D97706",
                iconName = "SelfImprovement",
                frequencyType = HabitFrequencyType.DAILY,
                targetCount = 1,
                unit = "session",
                isReminderEnabled = true,
                reminderTimeString = "21:30",
                createdAtEpochDay = startEpochDay
            ),
            Habit(
                id = 0,
                title = "Track Daily Expenses",
                description = "Log all personal expenditures in budget app",
                category = HabitCategory.FINANCE,
                colorHex = "#0D9488",
                iconName = "AccountBalanceWallet",
                frequencyType = HabitFrequencyType.DAILY,
                targetCount = 1,
                unit = "time",
                isReminderEnabled = false,
                createdAtEpochDay = startEpochDay
            )
        )
    }
}
