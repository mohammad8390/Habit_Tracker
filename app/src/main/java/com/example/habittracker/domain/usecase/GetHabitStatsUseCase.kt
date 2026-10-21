package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.model.CategoryStatData
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitFrequencyType
import com.example.habittracker.domain.model.HabitLog
import com.example.habittracker.domain.model.HabitPerformanceData
import com.example.habittracker.domain.model.HabitStats
import com.example.habittracker.domain.model.StatsPeriod
import com.example.habittracker.domain.model.WeeklyDayBarData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class GetHabitStatsUseCase {

    fun execute(
        allHabits: List<Habit>,
        allLogs: List<HabitLog>,
        period: StatsPeriod = StatsPeriod.WEEK,
        referenceDate: LocalDate = LocalDate.now()
    ): HabitStats {
        val activeHabits = allHabits.filter { !it.isArchived }
        if (activeHabits.isEmpty()) {
            return HabitStats()
        }

        val today = LocalDate.now()
        val (startDate, endDate) = getPeriodRange(period, referenceDate, activeHabits)

        val logsByHabitAndDate = allLogs
            .filter { it.isCompleted }
            .associateBy { Pair(it.habitId, it.dateEpochDay) }

        var totalScheduled = 0
        var totalCompleted = 0

        // Cap effectiveEndDate at referenceDate so future days in the period are not counted as missed
        val effectiveEndDate = if (endDate.isAfter(referenceDate)) referenceDate else endDate

        var currDate = startDate
        while (!currDate.isAfter(effectiveEndDate)) {
            val dateEpoch = currDate.toEpochDay()

            activeHabits.forEach { habit ->
                if (currDate.toEpochDay() >= habit.createdAtEpochDay && isHabitScheduledOn(habit, currDate)) {
                    totalScheduled++
                    if (logsByHabitAndDate.containsKey(Pair(habit.id, dateEpoch))) {
                        totalCompleted++
                    }
                }
            }
            currDate = currDate.plusDays(1)
        }

        val completionRate = if (totalScheduled > 0) {
            ((totalCompleted.toFloat() / totalScheduled.toFloat()) * 100f).coerceAtMost(100f)
        } else 0f

        // Weekly 7-day Bar Data (Monday .. Sunday of referenceDate's week)
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeklyBarData = (0..6).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            val dateEpoch = date.toEpochDay()
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            var dayScheduled = 0
            var dayCompleted = 0

            activeHabits.forEach { habit ->
                if (date.toEpochDay() >= habit.createdAtEpochDay && isHabitScheduledOn(habit, date)) {
                    dayScheduled++
                    if (logsByHabitAndDate.containsKey(Pair(habit.id, dateEpoch))) {
                        dayCompleted++
                    }
                }
            }

            WeeklyDayBarData(
                dayName = dayName,
                date = date,
                completedCount = dayCompleted,
                scheduledCount = dayScheduled,
                isToday = date.isEqual(today),
                isFuture = date.isAfter(today)
            )
        }

        // Category Breakdown
        val categoryBreakdown = HabitCategory.entries.mapNotNull { category ->
            val categoryHabits = activeHabits.filter { it.category == category }
            if (categoryHabits.isEmpty()) return@mapNotNull null

            var catScheduled = 0
            var catCompleted = 0

            var scanDate = startDate
            while (!scanDate.isAfter(effectiveEndDate)) {
                val epoch = scanDate.toEpochDay()
                categoryHabits.forEach { habit ->
                    if (scanDate.toEpochDay() >= habit.createdAtEpochDay && isHabitScheduledOn(habit, scanDate)) {
                        catScheduled++
                        if (logsByHabitAndDate.containsKey(Pair(habit.id, epoch))) {
                            catCompleted++
                        }
                    }
                }
                scanDate = scanDate.plusDays(1)
            }

            val rate = if (catScheduled > 0) ((catCompleted.toFloat() / catScheduled.toFloat()) * 100f).coerceAtMost(100f) else 0f

            CategoryStatData(
                category = category,
                completedCount = catCompleted,
                totalScheduledCount = catScheduled,
                completionRatePercent = rate
            )
        }

        // Per-Habit Performance Leaderboard (Filtered by selected period)
        val habitPerformanceList = activeHabits.map { habit ->
            val periodLogs = allLogs.filter { log ->
                log.habitId == habit.id &&
                        log.isCompleted &&
                        log.dateEpochDay >= startDate.toEpochDay() &&
                        log.dateEpochDay <= effectiveEndDate.toEpochDay()
            }
            val completedCount = periodLogs.size

            var scheduledCount = 0
            if (habit.frequencyType == HabitFrequencyType.TIMES_PER_WEEK) {
                scheduledCount = habit.targetCount
            } else {
                var scanDate = startDate
                while (!scanDate.isAfter(effectiveEndDate)) {
                    if (scanDate.toEpochDay() >= habit.createdAtEpochDay && isHabitScheduledOn(habit, scanDate)) {
                        scheduledCount++
                    }
                    scanDate = scanDate.plusDays(1)
                }
            }

            val rate = if (scheduledCount > 0) ((completedCount.toFloat() / scheduledCount.toFloat()) * 100f).coerceAtMost(100f) else 0f
            val (currentStreak, bestStreak) = computeStreakForHabit(habit, allLogs, referenceDate)

            HabitPerformanceData(
                habit = habit,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                totalCompletions = completedCount,
                completionRatePercent = rate
            )
        }.sortedWith(compareByDescending<HabitPerformanceData> { it.currentStreak }.thenByDescending { it.completionRatePercent })

        val maxCurrentStreak = habitPerformanceList.maxOfOrNull { it.currentStreak } ?: 0
        val maxBestStreak = habitPerformanceList.maxOfOrNull { it.bestStreak } ?: 0

        return HabitStats(
            completionRatePercent = completionRate,
            totalCompletedOccurrences = totalCompleted,
            totalScheduledOccurrences = totalScheduled,
            currentStreak = maxCurrentStreak,
            bestStreak = maxBestStreak,
            weeklyDayCounts = weeklyBarData,
            categoryBreakdown = categoryBreakdown,
            habitPerformanceList = habitPerformanceList
        )
    }

    private fun getPeriodRange(period: StatsPeriod, referenceDate: LocalDate, habits: List<Habit>): Pair<LocalDate, LocalDate> {
        return when (period) {
            StatsPeriod.WEEK -> {
                val start = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                Pair(start, end)
            }
            StatsPeriod.MONTH -> {
                val start = referenceDate.with(TemporalAdjusters.firstDayOfMonth())
                val end = referenceDate.with(TemporalAdjusters.lastDayOfMonth())
                Pair(start, end)
            }
            StatsPeriod.LIFETIME -> {
                val minCreationEpoch = habits.minOfOrNull { it.createdAtEpochDay } ?: referenceDate.toEpochDay()
                val start = LocalDate.ofEpochDay(minCreationEpoch)
                Pair(start, referenceDate)
            }
        }
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

    private fun computeStreakForHabit(habit: Habit, allLogs: List<HabitLog>, referenceDate: LocalDate): Pair<Int, Int> {
        val completedDates = allLogs
            .filter { it.habitId == habit.id && it.isCompleted }
            .map { it.date }
            .toSet()

        val frozenDates = allLogs
            .filter { it.habitId == habit.id && it.isFrozen }
            .map { it.date }
            .toSet()

        var currentStreak = 0
        var bestStreak = 0
        var tempStreak = 0

        var checkDate = referenceDate
        if (!completedDates.contains(checkDate) && !frozenDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        while (checkDate.toEpochDay() >= habit.createdAtEpochDay) {
            if (isHabitScheduledOn(habit, checkDate)) {
                if (completedDates.contains(checkDate)) {
                    currentStreak++
                } else if (frozenDates.contains(checkDate)) {
                    // Frozen/Preserved day: Keep streak intact without breaking
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
}
