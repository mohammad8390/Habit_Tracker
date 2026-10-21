package com.example.habittracker.data.repository

import android.content.Context
import com.example.habittracker.data.local.db.dao.HabitDao
import com.example.habittracker.data.local.db.entity.HabitEntity
import com.example.habittracker.data.local.db.entity.HabitLogEntity
import com.example.habittracker.data.notification.HabitReminderScheduler
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitLog
import com.example.habittracker.domain.model.HabitWithProgress
import com.example.habittracker.domain.repository.HabitRepository
import com.example.habittracker.widget.HabitWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val reminderScheduler: HabitReminderScheduler? = null,
    private val context: Context? = null
) : HabitRepository {

    override fun getAllActiveHabits(): Flow<List<Habit>> {
        return habitDao.getAllActiveHabits().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getArchivedHabits(): Flow<List<Habit>> {
        return habitDao.getArchivedHabits().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllLogs(): Flow<List<HabitLog>> {
        return habitDao.getAllLogs().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getHabitById(id: Long): Habit? {
        return habitDao.getHabitById(id)?.toDomainModel()
    }

    override suspend fun saveHabit(habit: Habit): Long {
        val habitId = if (habit.id == 0L) {
            habitDao.insertHabit(HabitEntity.fromDomainModel(habit))
        } else {
            habitDao.updateHabit(HabitEntity.fromDomainModel(habit))
            habit.id
        }

        val savedHabit = habit.copy(id = habitId)
        reminderScheduler?.scheduleOrUpdateReminder(savedHabit)
        context?.let { HabitWidgetProvider.notifyWidgetUpdate(it) }
        return habitId
    }

    override suspend fun archiveHabit(id: Long) {
        habitDao.archiveHabit(id)
        reminderScheduler?.cancelReminder(id)
        context?.let { HabitWidgetProvider.notifyWidgetUpdate(it) }
    }

    override suspend fun unarchiveHabit(id: Long) {
        habitDao.unarchiveHabit(id)
        val habit = habitDao.getHabitById(id)?.toDomainModel()
        if (habit != null) {
            reminderScheduler?.scheduleOrUpdateReminder(habit)
        }
        context?.let { HabitWidgetProvider.notifyWidgetUpdate(it) }
    }

    override suspend fun deleteHabit(id: Long) {
        habitDao.deleteHabitById(id)
        reminderScheduler?.cancelReminder(id)
        context?.let { HabitWidgetProvider.notifyWidgetUpdate(it) }
    }

    override fun getHabitsWithProgressForDate(date: LocalDate): Flow<List<HabitWithProgress>> {
        val dateEpochDay = date.toEpochDay()
        val dayOfWeekIso = date.dayOfWeek.value // 1 = Monday, 7 = Sunday

        return combine(
            habitDao.getAllActiveHabits(),
            habitDao.getLogsForDate(dateEpochDay)
        ) { habits, logs ->
            val logMap = logs.associateBy { it.habitId }

            habits
                .map { it.toDomainModel() }
                .filter { habit ->
                    val isAfterCreation = dateEpochDay >= habit.createdAtEpochDay
                    val isScheduledDay = when (habit.frequencyType) {
                        com.example.habittracker.domain.model.HabitFrequencyType.DAILY -> true
                        com.example.habittracker.domain.model.HabitFrequencyType.WEEKLY_DAYS -> {
                            habit.targetDaysOfWeek.contains(dayOfWeekIso)
                        }
                        com.example.habittracker.domain.model.HabitFrequencyType.TIMES_PER_WEEK -> true
                    }
                    isAfterCreation && isScheduledDay
                }
                .map { habit ->
                    val log = logMap[habit.id]
                    val completedCount = log?.completedCount ?: 0
                    val isCompleted = log?.isCompleted ?: (completedCount >= habit.targetCount && habit.targetCount > 0)
                    val isFrozen = log?.isFrozen ?: false
                    val (currentStreak, bestStreak) = computeStreakForHabit(habit, date)

                    HabitWithProgress(
                        habit = habit,
                        completedCount = completedCount,
                        isCompleted = isCompleted,
                        isFrozen = isFrozen,
                        currentStreak = currentStreak,
                        bestStreak = bestStreak
                    )
                }
        }
    }

    override suspend fun toggleHabitCompletion(habitId: Long, date: LocalDate): Boolean {
        val dateEpochDay = date.toEpochDay()
        val habit = habitDao.getHabitById(habitId)?.toDomainModel() ?: return false
        val existingLog = habitDao.getLogForHabitAndDate(habitId, dateEpochDay)

        val newCompletedCount = if (existingLog == null || !existingLog.isCompleted) {
            habit.targetCount
        } else {
            0
        }
        val newIsCompleted = newCompletedCount >= habit.targetCount && habit.targetCount > 0

        val logToSave = HabitLogEntity(
            id = existingLog?.id ?: 0,
            habitId = habitId,
            dateEpochDay = dateEpochDay,
            completedCount = newCompletedCount,
            isCompleted = newIsCompleted,
            isFrozen = false, // Reset frozen if completed
            updatedAtEpochMs = System.currentTimeMillis()
        )

        habitDao.insertOrUpdateLog(logToSave)
        context?.let { HabitWidgetProvider.notifyWidgetUpdate(it) }
        return newIsCompleted
    }

    override suspend fun toggleStreakFreeze(habitId: Long, date: LocalDate): Boolean {
        val dateEpochDay = date.toEpochDay()
        val habit = habitDao.getHabitById(habitId)?.toDomainModel() ?: return false
        val existingLog = habitDao.getLogForHabitAndDate(habitId, dateEpochDay)

        val newIsFrozen = !(existingLog?.isFrozen ?: false)

        val logToSave = HabitLogEntity(
            id = existingLog?.id ?: 0,
            habitId = habitId,
            dateEpochDay = dateEpochDay,
            completedCount = existingLog?.completedCount ?: 0,
            isCompleted = if (newIsFrozen) false else (existingLog?.isCompleted ?: false),
            isFrozen = newIsFrozen,
            updatedAtEpochMs = System.currentTimeMillis()
        )

        habitDao.insertOrUpdateLog(logToSave)
        context?.let { HabitWidgetProvider.notifyWidgetUpdate(it) }
        return newIsFrozen
    }

    override suspend fun setHabitCompletionCount(habitId: Long, date: LocalDate, count: Int) {
        val dateEpochDay = date.toEpochDay()
        val habit = habitDao.getHabitById(habitId)?.toDomainModel() ?: return
        val existingLog = habitDao.getLogForHabitAndDate(habitId, dateEpochDay)

        val targetCount = habit.targetCount
        val safeCount = count.coerceAtLeast(0)
        val isCompleted = safeCount >= targetCount && targetCount > 0

        val logToSave = HabitLogEntity(
            id = existingLog?.id ?: 0,
            habitId = habitId,
            dateEpochDay = dateEpochDay,
            completedCount = safeCount,
            isCompleted = isCompleted,
            isFrozen = false,
            updatedAtEpochMs = System.currentTimeMillis()
        )

        habitDao.insertOrUpdateLog(logToSave)
        context?.let { HabitWidgetProvider.notifyWidgetUpdate(it) }
    }

    override fun getLogsForHabit(habitId: Long): Flow<List<HabitLog>> {
        return habitDao.getLogsForHabit(habitId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun calculateStreak(habitId: Long): Pair<Int, Int> {
        val habit = habitDao.getHabitById(habitId)?.toDomainModel() ?: return Pair(0, 0)
        return computeStreakForHabit(habit, LocalDate.now())
    }

    private suspend fun computeStreakForHabit(habit: Habit, referenceDate: LocalDate): Pair<Int, Int> {
        val logs = habitDao.getLogsForHabitOnce(habit.id)
        val completedDates = logs
            .filter { it.isCompleted }
            .map { LocalDate.ofEpochDay(it.dateEpochDay) }
            .toSet()

        val frozenDates = logs
            .filter { it.isFrozen }
            .map { LocalDate.ofEpochDay(it.dateEpochDay) }
            .toSet()

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
                    // Frozen/Preserved day: Keep streak intact without breaking
                } else {
                    break
                }
            }
            checkDate = checkDate.minusDays(1)
        }

        var scanDate = LocalDate.ofEpochDay(habit.createdAtEpochDay)
        val today = LocalDate.now()
        while (!scanDate.isAfter(today)) {
            if (isHabitScheduledOn(habit, scanDate)) {
                if (completedDates.contains(scanDate)) {
                    tempStreak++
                    if (tempStreak > bestStreak) {
                        bestStreak = tempStreak
                    }
                } else if (frozenDates.contains(scanDate)) {
                    // Keep tempStreak as is
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
            com.example.habittracker.domain.model.HabitFrequencyType.DAILY -> true
            com.example.habittracker.domain.model.HabitFrequencyType.WEEKLY_DAYS -> {
                habit.targetDaysOfWeek.contains(date.dayOfWeek.value)
            }
            com.example.habittracker.domain.model.HabitFrequencyType.TIMES_PER_WEEK -> true
        }
    }
}
