package com.example.habittracker

import com.example.habittracker.data.local.BackupRestoreManager
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitLog
import com.example.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupRestoreManagerTest {

    private val fakeRepository = FakeHabitRepository()
    private val manager = BackupRestoreManager(fakeRepository)

    @Test
    fun exportDataToJson_producesValidJsonStructure() = runBlocking {
        fakeRepository.saveHabit(
            Habit(id = 1, title = "Export Test Habit", category = HabitCategory.FITNESS)
        )

        val jsonString = manager.exportDataToJson()

        assertTrue(jsonString.contains("\"version\": 1"))
        assertTrue(jsonString.contains("Export Test Habit"))
        assertTrue(jsonString.contains("FITNESS"))
    }

    @Test
    fun importInvalidJson_returnsErrorWithoutCrash() = runBlocking {
        val invalidJson = "{ invalid json }"
        val (count, message) = manager.importDataFromJson(invalidJson)

        assertEquals(0, count)
        assertTrue(message.contains("Failed to import"))
    }

    @Test
    fun importValidJson_createsHabitsAndReturnsSuccessCount() = runBlocking {
        val validJson = """
            {
              "version": 1,
              "habits": [
                {
                  "id": 101,
                  "title": "Imported Water Habit",
                  "category": "HEALTH",
                  "targetCount": 5
                }
              ]
            }
        """.trimIndent()

        val (count, message) = manager.importDataFromJson(validJson)

        assertEquals(1, count)
        assertTrue(message.contains("Successfully imported 1 habit(s)"))
    }
}

private class FakeHabitRepository : HabitRepository {
    private val habitsList = mutableListOf<Habit>()
    private val logsList = mutableListOf<HabitLog>()
    private var nextId = 1L

    override fun getAllActiveHabits(): Flow<List<Habit>> = flowOf(habitsList.filter { !it.isArchived })
    override fun getArchivedHabits(): Flow<List<Habit>> = flowOf(habitsList.filter { it.isArchived })
    override fun getAllHabits(): Flow<List<Habit>> = flowOf(habitsList)
    override fun getAllLogs(): Flow<List<HabitLog>> = flowOf(logsList)

    override suspend fun getHabitById(id: Long): Habit? = habitsList.find { it.id == id }

    override suspend fun saveHabit(habit: Habit): Long {
        return if (habit.id == 0L) {
            val assignedId = nextId++
            val saved = habit.copy(id = assignedId)
            habitsList.add(saved)
            assignedId
        } else {
            val index = habitsList.indexOfFirst { it.id == habit.id }
            if (index != -1) habitsList[index] = habit else habitsList.add(habit)
            habit.id
        }
    }

    override suspend fun archiveHabit(id: Long) {
        val index = habitsList.indexOfFirst { it.id == id }
        if (index != -1) habitsList[index] = habitsList[index].copy(isArchived = true)
    }

    override suspend fun unarchiveHabit(id: Long) {
        val index = habitsList.indexOfFirst { it.id == id }
        if (index != -1) habitsList[index] = habitsList[index].copy(isArchived = false)
    }

    override suspend fun deleteHabit(id: Long) {
        habitsList.removeAll { it.id == id }
        logsList.removeAll { it.habitId == id }
    }

    override fun getHabitsWithProgressForDate(date: LocalDate): Flow<List<com.example.habittracker.domain.model.HabitWithProgress>> = flowOf(emptyList())

    override suspend fun toggleHabitCompletion(habitId: Long, date: LocalDate): Boolean = true

    override suspend fun toggleStreakFreeze(habitId: Long, date: LocalDate): Boolean = true

    override suspend fun setHabitCompletionCount(habitId: Long, date: LocalDate, count: Int) {
        logsList.add(HabitLog(id = logsList.size + 1L, habitId = habitId, dateEpochDay = date.toEpochDay(), completedCount = count, isCompleted = count >= 1))
    }

    override fun getLogsForHabit(habitId: Long): Flow<List<HabitLog>> = flowOf(logsList.filter { it.habitId == habitId })

    override suspend fun calculateStreak(habitId: Long): Pair<Int, Int> = Pair(0, 0)
}
