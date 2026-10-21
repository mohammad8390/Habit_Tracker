package com.example.habittracker.domain.model

import java.time.LocalDate

data class Habit(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val category: HabitCategory = HabitCategory.GENERAL,
    val colorHex: String = category.colorHex,
    val iconName: String = category.iconName,
    val frequencyType: HabitFrequencyType = HabitFrequencyType.DAILY,
    val targetDaysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // ISO 1..7 (Mon..Sun)
    val targetCount: Int = 1,
    val unit: String? = "times",
    val isReminderEnabled: Boolean = false,
    val reminderTimeString: String? = null, // Comma-separated, e.g. "09:00 AM, 02:00 PM, 08:00 PM"
    val isArchived: Boolean = false,
    val createdAtEpochDay: Long = LocalDate.now().toEpochDay()
) {
    val reminderTimesList: List<String>
        get() = if (reminderTimeString.isNullOrBlank()) {
            emptyList()
        } else {
            reminderTimeString.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
}
