package com.example.habittracker

import com.example.habittracker.domain.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HabitReminderSchedulerTest {

    @Test
    fun parseReminderTimeString_parses12HourAnd24HourFormats() {
        val time12 = parseTime("08:30 AM")
        val time24 = parseTime("20:15")

        assertNotNull(time12)
        assertEquals(LocalTime.of(8, 30), time12)

        assertNotNull(time24)
        assertEquals(LocalTime.of(20, 15), time24)
    }

    @Test
    fun invalidReminderTimeString_returnsNull() {
        val invalidTime = parseTime("invalid-time")
        assertNull(invalidTime)
    }

    @Test
    fun habitReminderStatus_evaluatesCorrectActiveFlag() {
        val activeHabitWithReminder = Habit(
            id = 1,
            title = "Morning Water",
            isReminderEnabled = true,
            reminderTimeString = "09:00 AM",
            isArchived = false
        )

        val archivedHabit = activeHabitWithReminder.copy(isArchived = true)
        val disabledReminderHabit = activeHabitWithReminder.copy(isReminderEnabled = false)

        assertTrue(shouldScheduleReminder(activeHabitWithReminder))
        assertFalse(shouldScheduleReminder(archivedHabit))
        assertFalse(shouldScheduleReminder(disabledReminderHabit))
    }

    private fun shouldScheduleReminder(habit: Habit): Boolean {
        return habit.isReminderEnabled && !habit.isArchived && !habit.reminderTimeString.isNullOrBlank()
    }

    private fun parseTime(timeString: String?): LocalTime? {
        if (timeString.isNullOrBlank()) return null
        return try {
            val formatter12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
            LocalTime.parse(timeString, formatter12)
        } catch (e1: Exception) {
            try {
                val formatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
                LocalTime.parse(timeString, formatter24)
            } catch (e2: Exception) {
                null
            }
        }
    }
}
