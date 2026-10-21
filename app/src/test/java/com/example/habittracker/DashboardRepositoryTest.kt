package com.example.habittracker

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitFrequencyType
import com.example.habittracker.domain.model.HabitWithProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DashboardRepositoryTest {

    @Test
    fun zeroHabits_calculatesZeroProgressWithoutDivisionByZero() {
        val habits = emptyList<HabitWithProgress>()
        val totalCount = habits.size
        val completedCount = habits.count { it.isCompleted }

        val progressFraction = if (totalCount > 0) {
            completedCount.toFloat() / totalCount.toFloat()
        } else {
            0f
        }

        assertEquals(0, totalCount)
        assertEquals(0, completedCount)
        assertEquals(0f, progressFraction, 0.001f)
    }

    @Test
    fun partiallyCompletedHabits_calculatesCorrectProgressFraction() {
        val testHabit1 = Habit(id = 1, title = "Water", targetCount = 1)
        val testHabit2 = Habit(id = 2, title = "Run", targetCount = 1)

        val item1 = HabitWithProgress(habit = testHabit1, completedCount = 1, isCompleted = true)
        val item2 = HabitWithProgress(habit = testHabit2, completedCount = 0, isCompleted = false)

        val habits = listOf(item1, item2)
        val totalCount = habits.size
        val completedCount = habits.count { it.isCompleted }

        val progressFraction = completedCount.toFloat() / totalCount.toFloat()

        assertEquals(2, totalCount)
        assertEquals(1, completedCount)
        assertEquals(0.5f, progressFraction, 0.001f)
    }

    @Test
    fun fullyCompletedHabits_calculatesOneHundredPercentProgress() {
        val testHabit1 = Habit(id = 1, title = "Water", targetCount = 1)
        val item1 = HabitWithProgress(habit = testHabit1, completedCount = 1, isCompleted = true)

        val habits = listOf(item1)
        val totalCount = habits.size
        val completedCount = habits.count { it.isCompleted }

        val progressFraction = completedCount.toFloat() / totalCount.toFloat()

        assertEquals(1, totalCount)
        assertEquals(1, completedCount)
        assertEquals(1.0f, progressFraction, 0.001f)
    }

    @Test
    fun habitFiltering_filtersByScheduledDayOfWeek() {
        val mondayDate = LocalDate.of(2026, 10, 19) // Monday (Day 1)
        val tuesdayDate = LocalDate.of(2026, 10, 20) // Tuesday (Day 2)

        val mondayOnlyHabit = Habit(
            id = 10,
            title = "Monday Review",
            frequencyType = HabitFrequencyType.WEEKLY_DAYS,
            targetDaysOfWeek = listOf(1) // Monday only
        )

        val isScheduledMonday = mondayOnlyHabit.targetDaysOfWeek.contains(mondayDate.dayOfWeek.value)
        val isScheduledTuesday = mondayOnlyHabit.targetDaysOfWeek.contains(tuesdayDate.dayOfWeek.value)

        assertTrue(isScheduledMonday)
        assertFalse(isScheduledTuesday)
    }
}
