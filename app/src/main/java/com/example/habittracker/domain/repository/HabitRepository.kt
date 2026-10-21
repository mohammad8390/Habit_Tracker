package com.example.habittracker.domain.repository

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitLog
import com.example.habittracker.domain.model.HabitWithProgress
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {

    fun getAllActiveHabits(): Flow<List<Habit>>

    fun getArchivedHabits(): Flow<List<Habit>>

    fun getAllHabits(): Flow<List<Habit>>

    fun getAllLogs(): Flow<List<HabitLog>>

    suspend fun getHabitById(id: Long): Habit?

    suspend fun saveHabit(habit: Habit): Long

    suspend fun archiveHabit(id: Long)

    suspend fun unarchiveHabit(id: Long)

    suspend fun deleteHabit(id: Long)

    fun getHabitsWithProgressForDate(date: LocalDate): Flow<List<HabitWithProgress>>

    suspend fun toggleHabitCompletion(habitId: Long, date: LocalDate): Boolean

    suspend fun toggleStreakFreeze(habitId: Long, date: LocalDate): Boolean

    suspend fun setHabitCompletionCount(habitId: Long, date: LocalDate, count: Int)

    fun getLogsForHabit(habitId: Long): Flow<List<HabitLog>>

    suspend fun calculateStreak(habitId: Long): Pair<Int, Int> // (currentStreak, bestStreak)
}
