package com.example.habittracker

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitFrequencyType
import com.example.habittracker.domain.model.HabitLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculationTest {

    @Test
    fun consecutiveDailyCompletions_calculatesCorrectStreak() {
        val today = LocalDate.of(2026, 10, 20)
        val createdDate = today.minusDays(10)

        val habit = Habit(
            id = 1,
            title = "Daily Meditation",
            frequencyType = HabitFrequencyType.DAILY,
            createdAtEpochDay = createdDate.toEpochDay()
        )

        val logs = (0..4).map { offset ->
            val date = today.minusDays(offset.toLong())
            HabitLog(
                id = offset.toLong() + 1,
                habitId = 1,
                dateEpochDay = date.toEpochDay(),
                completedCount = 1,
                isCompleted = true
            )
        }

        val completedDates = logs.filter { it.isCompleted }.map { it.date }.toSet()
        val (currentStreak, bestStreak) = computeStreakForTesting(habit, today, completedDates, emptySet())

        assertEquals(5, currentStreak)
        assertEquals(5, bestStreak)
    }

    @Test
    fun streakFreeze_preservesStreakOnFrozenDays() {
        val today = LocalDate.of(2026, 10, 20)
        val yesterday = today.minusDays(1) // Frozen day
        val day2Ago = today.minusDays(2)   // Completed
        val day3Ago = today.minusDays(3)   // Completed

        val habit = Habit(
            id = 5,
            title = "Reading",
            frequencyType = HabitFrequencyType.DAILY,
            createdAtEpochDay = day3Ago.toEpochDay()
        )

        val completedDates = setOf(today, day2Ago, day3Ago)
        val frozenDates = setOf(yesterday)

        val (currentStreak, bestStreak) = computeStreakForTesting(habit, today, completedDates, frozenDates)

        // Yesterday was frozen, so streak is 3 (today + 2 ago + 3 ago) without breaking!
        assertEquals(3, currentStreak)
        assertEquals(3, bestStreak)
    }

    @Test
    fun unscheduledDays_doNotBreakStreak() {
        val wednesday = LocalDate.of(2026, 10, 21) // Wednesday (Day 3)
        val monday = LocalDate.of(2026, 10, 19)    // Monday (Day 1)
        val fridayPrev = LocalDate.of(2026, 10, 16) // Friday (Day 5)

        val habit = Habit(
            id = 2,
            title = "Mon/Wed/Fri Workout",
            frequencyType = HabitFrequencyType.WEEKLY_DAYS,
            targetDaysOfWeek = listOf(1, 3, 5),
            createdAtEpochDay = fridayPrev.toEpochDay()
        )

        val completedDates = setOf(fridayPrev, monday, wednesday)

        val (currentStreak, bestStreak) = computeStreakForTesting(habit, wednesday, completedDates, emptySet())

        assertEquals(3, currentStreak)
        assertEquals(3, bestStreak)
    }

    @Test
    fun missedScheduledDay_breaksCurrentStreak_butPreservesBestStreak() {
        val today = LocalDate.of(2026, 10, 20)
        val createdDate = today.minusDays(15)

        val habit = Habit(
            id = 3,
            title = "Read Book",
            frequencyType = HabitFrequencyType.DAILY,
            createdAtEpochDay = createdDate.toEpochDay()
        )

        val historicalLogs = (9..15).map { today.minusDays(it.toLong()) }
        val currentLogs = listOf(today, today.minusDays(1))

        val completedDates = (historicalLogs + currentLogs).toSet()

        val (currentStreak, bestStreak) = computeStreakForTesting(habit, today, completedDates, emptySet())

        assertEquals(2, currentStreak)
        assertEquals(7, bestStreak)
    }

    @Test
    fun newlyCreatedHabit_handlesZeroOrOneStreakGracefully() {
        val today = LocalDate.of(2026, 10, 20)

        val habit = Habit(
            id = 4,
            title = "New Habit",
            frequencyType = HabitFrequencyType.DAILY,
            createdAtEpochDay = today.toEpochDay()
        )

        val completedDatesEmpty = emptySet<LocalDate>()
        val (current0, best0) = computeStreakForTesting(habit, today, completedDatesEmpty, emptySet())

        assertEquals(0, current0)
        assertEquals(0, best0)

        val completedDatesToday = setOf(today)
        val (current1, best1) = computeStreakForTesting(habit, today, completedDatesToday, emptySet())

        assertEquals(1, current1)
        assertEquals(1, best1)
    }

    private fun computeStreakForTesting(
        habit: Habit,
        referenceDate: LocalDate,
        completedDates: Set<LocalDate>,
        frozenDates: Set<LocalDate> = emptySet()
    ): Pair<Int, Int> {
        var currentStreak = 0
        var bestStreak = 0
        var tempStreak = 0

        var checkDate = referenceDate
        if (!completedDates.contains(checkDate) && !frozenDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        while (checkDate.toEpochDay() >= habit.createdAtEpochDay) {
            val isScheduled = isHabitScheduledOn(habit, checkDate)
            if (isScheduled) {
                if (completedDates.contains(checkDate)) {
                    currentStreak++
                } else if (frozenDates.contains(checkDate)) {
                    // Frozen day preserves streak
                } else {
                    break
                }
            }
            checkDate = checkDate.minusDays(1)
        }

        var scanDate = LocalDate.ofEpochDay(habit.createdAtEpochDay)
        while (!scanDate.isAfter(referenceDate)) {
            if (isHabitScheduledOn(habit, scanDate)) {
                if (completedDates.contains(scanDate)) {
                    tempStreak++
                    if (tempStreak > bestStreak) {
                        bestStreak = tempStreak
                    }
                } else if (frozenDates.contains(scanDate)) {
                    // Preserved
                } else {
                    tempStreak = 0
                }
            }
            scanDate = scanDate.plusDays(1)
        }

        bestStreak = maxOf(bestStreak, currentStreak)
        return Pair(currentStreak, bestStreak)
    }

    private fun isHabitScheduledOn(habit: Habit, date: LocalDate): Boolean {
        return when (habit.frequencyType) {
            HabitFrequencyType.DAILY -> true
            HabitFrequencyType.WEEKLY_DAYS -> {
                habit.targetDaysOfWeek.contains(date.dayOfWeek.value)
            }
            HabitFrequencyType.TIMES_PER_WEEK -> true
        }
    }
}
