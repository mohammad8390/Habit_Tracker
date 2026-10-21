package com.example.habittracker.domain.model

enum class HabitFrequencyType {
    DAILY,
    WEEKLY_DAYS,
    TIMES_PER_WEEK
}

data class HabitFrequency(
    val type: HabitFrequencyType = HabitFrequencyType.DAILY,
    val selectedDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1 = Monday, 7 = Sunday
    val targetTimesPerWeek: Int = 7
)
