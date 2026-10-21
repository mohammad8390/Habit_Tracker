package com.example.habittracker

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitFrequencyType
import com.example.habittracker.domain.model.HabitLog
import com.example.habittracker.domain.model.StatsPeriod
import com.example.habittracker.domain.usecase.GetHabitStatsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetHabitStatsUseCaseTest {

    private val useCase = GetHabitStatsUseCase()

    @Test
    fun emptyHabitsList_returnsZeroedStatsWithoutCrash() {
        val stats = useCase.execute(
            allHabits = emptyList(),
            allLogs = emptyList(),
            period = StatsPeriod.WEEK,
            referenceDate = LocalDate.now()
        )

        assertEquals(0f, stats.completionRatePercent, 0.001f)
        assertEquals(0, stats.totalCompletedOccurrences)
        assertEquals(0, stats.totalScheduledOccurrences)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.bestStreak)
        assertTrue(stats.weeklyDayCounts.isEmpty())
        assertTrue(stats.categoryBreakdown.isEmpty())
        assertTrue(stats.habitPerformanceList.isEmpty())
    }

    @Test
    fun unscheduledDays_areExcludedFromCompletionRateDenominator() {
        val today = LocalDate.of(2026, 10, 20) // Tuesday
        val monday = LocalDate.of(2026, 10, 19)

        // Mon/Wed/Fri schedule (Days 1, 3, 5)
        val habit = Habit(
            id = 1,
            title = "Gym",
            category = HabitCategory.FITNESS,
            frequencyType = HabitFrequencyType.WEEKLY_DAYS,
            targetDaysOfWeek = listOf(1, 3, 5),
            createdAtEpochDay = monday.toEpochDay()
        )

        // Mon completed (Tuesday not scheduled, so not counted)
        val logs = listOf(
            HabitLog(id = 1, habitId = 1, dateEpochDay = monday.toEpochDay(), completedCount = 1, isCompleted = true)
        )

        val stats = useCase.execute(
            allHabits = listOf(habit),
            allLogs = logs,
            period = StatsPeriod.WEEK,
            referenceDate = today
        )

        // On Monday and Tuesday up to today, only 1 day (Monday) was scheduled!
        assertEquals(1, stats.totalScheduledOccurrences)
        assertEquals(1, stats.totalCompletedOccurrences)
        assertEquals(100f, stats.completionRatePercent, 0.001f)
    }

    @Test
    fun futureDates_areNotTreatedAsMissedDays() {
        val tuesday = LocalDate.of(2026, 10, 20) // Tuesday of current week

        val habit = Habit(
            id = 2,
            title = "Daily Drink Water",
            category = HabitCategory.HEALTH,
            frequencyType = HabitFrequencyType.DAILY,
            createdAtEpochDay = tuesday.minusDays(1).toEpochDay() // Mon & Tue
        )

        // Mon & Tue completed
        val monday = tuesday.minusDays(1)
        val logs = listOf(
            HabitLog(id = 1, habitId = 2, dateEpochDay = monday.toEpochDay(), completedCount = 1, isCompleted = true),
            HabitLog(id = 2, habitId = 2, dateEpochDay = tuesday.toEpochDay(), completedCount = 1, isCompleted = true)
        )

        val stats = useCase.execute(
            allHabits = listOf(habit),
            allLogs = logs,
            period = StatsPeriod.WEEK,
            referenceDate = tuesday
        )

        // Even though Wednesday..Sunday are in the week, they are future dates so effectiveEndDate = Tuesday
        assertEquals(2, stats.totalScheduledOccurrences)
        assertEquals(2, stats.totalCompletedOccurrences)
        assertEquals(100f, stats.completionRatePercent, 0.001f)
    }

    @Test
    fun monthAndYearBoundaries_calculateCorrectRanges() {
        val newYearsEve = LocalDate.of(2026, 12, 31)

        val habit = Habit(
            id = 3,
            title = "Year End Habit",
            createdAtEpochDay = LocalDate.of(2026, 12, 1).toEpochDay()
        )

        val stats = useCase.execute(
            allHabits = listOf(habit),
            allLogs = emptyList(),
            period = StatsPeriod.MONTH,
            referenceDate = newYearsEve
        )

        // 31 days in December scheduled
        assertEquals(31, stats.totalScheduledOccurrences)
        assertEquals(0, stats.totalCompletedOccurrences)
        assertEquals(0f, stats.completionRatePercent, 0.001f)
    }
}
