package com.example.habittracker.domain.model

import java.time.LocalDate

enum class StatsPeriod {
    WEEK,
    MONTH,
    LIFETIME
}

data class HabitStats(
    val completionRatePercent: Float = 0f,
    val totalCompletedOccurrences: Int = 0,
    val totalScheduledOccurrences: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val weeklyDayCounts: List<WeeklyDayBarData> = emptyList(),
    val categoryBreakdown: List<CategoryStatData> = emptyList(),
    val habitPerformanceList: List<HabitPerformanceData> = emptyList()
)

data class WeeklyDayBarData(
    val dayName: String,
    val date: LocalDate,
    val completedCount: Int,
    val scheduledCount: Int,
    val isToday: Boolean,
    val isFuture: Boolean
)

data class CategoryStatData(
    val category: HabitCategory,
    val completedCount: Int,
    val totalScheduledCount: Int,
    val completionRatePercent: Float
)

data class HabitPerformanceData(
    val habit: Habit,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val completionRatePercent: Float
)
