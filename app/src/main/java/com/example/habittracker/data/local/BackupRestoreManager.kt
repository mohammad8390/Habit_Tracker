package com.example.habittracker.data.local

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitFrequencyType
import com.example.habittracker.domain.model.HabitLog
import com.example.habittracker.domain.model.TodoTask
import com.example.habittracker.domain.repository.HabitRepository
import com.example.habittracker.domain.repository.TodoRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

class BackupRestoreManager(
    private val repository: HabitRepository,
    private val todoRepository: TodoRepository? = null
) {

    suspend fun exportDataToJson(): String {
        val habits = repository.getAllHabits().firstOrNull() ?: emptyList()
        val allLogs = mutableListOf<HabitLog>()

        habits.forEach { habit ->
            val logs = repository.getLogsForHabit(habit.id).firstOrNull() ?: emptyList()
            allLogs.addAll(logs)
        }

        val todoTasks = todoRepository?.getTasksForDate(LocalDate.now())?.firstOrNull() ?: emptyList()

        val habitsJson = habits.joinToString(",\n    ") { habit ->
            """{
      "id": ${habit.id},
      "title": "${escapeJson(habit.title)}",
      "description": "${escapeJson(habit.description ?: "")}",
      "category": "${habit.category.name}",
      "colorHex": "${habit.colorHex}",
      "iconName": "${habit.iconName}",
      "frequencyType": "${habit.frequencyType.name}",
      "targetDaysOfWeek": "${habit.targetDaysOfWeek.joinToString(",")}",
      "targetCount": ${habit.targetCount},
      "unit": "${escapeJson(habit.unit ?: "times")}",
      "isReminderEnabled": ${habit.isReminderEnabled},
      "reminderTimeString": "${habit.reminderTimeString ?: ""}",
      "isArchived": ${habit.isArchived},
      "createdAtEpochDay": ${habit.createdAtEpochDay}
    }"""
        }

        val logsJson = allLogs.joinToString(",\n    ") { log ->
            """{
      "id": ${log.id},
      "habitId": ${log.habitId},
      "dateEpochDay": ${log.dateEpochDay},
      "completedCount": ${log.completedCount},
      "isCompleted": ${log.isCompleted},
      "isFrozen": ${log.isFrozen},
      "updatedAtEpochMs": ${log.updatedAtEpochMs}
    }"""
        }

        val todoJson = todoTasks.joinToString(",\n    ") { task ->
            """{
      "id": ${task.id},
      "title": "${escapeJson(task.title)}",
      "dueDateEpochDay": ${task.dueDateEpochDay},
      "isCompleted": ${task.isCompleted},
      "isReminderEnabled": ${task.isReminderEnabled},
      "reminderTimeString": "${task.reminderTimeString ?: ""}"
    }"""
        }

        return """{
  "version": 1,
  "exportedAtMs": ${System.currentTimeMillis()},
  "habits": [
    $habitsJson
  ],
  "logs": [
    $logsJson
  ],
  "todoTasks": [
    $todoJson
  ]
}"""
    }

    suspend fun importDataFromJson(jsonString: String): Pair<Int, String> {
        return try {
            if (!jsonString.contains("\"version\"") || !jsonString.contains("\"habits\"")) {
                return Pair(0, "Failed to import data: Invalid backup format.")
            }

            val habitBlocks = extractJsonObjects(jsonString, "habits")
            var importedCount = 0
            val oldIdToNewIdMap = mutableMapOf<Long, Long>()

            habitBlocks.forEach { block ->
                val oldId = extractJsonLong(block, "id", 0L)
                val title = extractJsonString(block, "title").trim()

                if (title.isNotBlank()) {
                    val categoryName = extractJsonString(block, "category").ifEmpty { "GENERAL" }
                    val category = HabitCategory.fromName(categoryName)
                    val freqName = extractJsonString(block, "frequencyType").ifEmpty { "DAILY" }
                    val frequency = try {
                        HabitFrequencyType.valueOf(freqName)
                    } catch (e: Exception) {
                        HabitFrequencyType.DAILY
                    }

                    val daysStr = extractJsonString(block, "targetDaysOfWeek").ifEmpty { "1,2,3,4,5,6,7" }
                    val daysList = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }

                    val habitToInsert = Habit(
                        id = 0,
                        title = title,
                        description = extractJsonString(block, "description").ifEmpty { null },
                        category = category,
                        colorHex = extractJsonString(block, "colorHex").ifEmpty { category.colorHex },
                        iconName = extractJsonString(block, "iconName").ifEmpty { category.iconName },
                        frequencyType = frequency,
                        targetDaysOfWeek = daysList,
                        targetCount = extractJsonInt(block, "targetCount", 1).coerceAtLeast(1),
                        unit = extractJsonString(block, "unit").ifEmpty { "times" },
                        isReminderEnabled = extractJsonBoolean(block, "isReminderEnabled"),
                        reminderTimeString = extractJsonString(block, "reminderTimeString").ifEmpty { null },
                        isArchived = extractJsonBoolean(block, "isArchived"),
                        createdAtEpochDay = extractJsonLong(block, "createdAtEpochDay", LocalDate.now().toEpochDay())
                    )

                    val newId = repository.saveHabit(habitToInsert)
                    if (oldId != 0L) {
                        oldIdToNewIdMap[oldId] = newId
                    }
                    importedCount++
                }
            }

            // Restore Completion & Streak Freeze Logs
            val logBlocks = extractJsonObjects(jsonString, "logs")
            logBlocks.forEach { logBlock ->
                val oldHabitId = extractJsonLong(logBlock, "habitId", 0L)
                val dateEpochDay = extractJsonLong(logBlock, "dateEpochDay", 0L)
                val completedCount = extractJsonInt(logBlock, "completedCount", 0)
                val isFrozen = extractJsonBoolean(logBlock, "isFrozen")

                val targetNewHabitId = oldIdToNewIdMap[oldHabitId]
                if (targetNewHabitId != null && dateEpochDay > 0L) {
                    if (isFrozen) {
                        repository.toggleStreakFreeze(
                            habitId = targetNewHabitId,
                            date = LocalDate.ofEpochDay(dateEpochDay)
                        )
                    } else {
                        repository.setHabitCompletionCount(
                            habitId = targetNewHabitId,
                            date = LocalDate.ofEpochDay(dateEpochDay),
                            count = completedCount
                        )
                    }
                }
            }

            // Restore To-Do Tasks
            if (todoRepository != null) {
                val todoBlocks = extractJsonObjects(jsonString, "todoTasks")
                todoBlocks.forEach { taskBlock ->
                    val title = extractJsonString(taskBlock, "title").trim()
                    if (title.isNotBlank()) {
                        val taskToInsert = TodoTask(
                            id = 0,
                            title = title,
                            dueDateEpochDay = extractJsonLong(taskBlock, "dueDateEpochDay", LocalDate.now().toEpochDay()),
                            isCompleted = extractJsonBoolean(taskBlock, "isCompleted"),
                            isReminderEnabled = extractJsonBoolean(taskBlock, "isReminderEnabled"),
                            reminderTimeString = extractJsonString(taskBlock, "reminderTimeString").ifEmpty { null }
                        )
                        todoRepository.saveTask(taskToInsert)
                    }
                }
            }

            Pair(importedCount, "Successfully imported $importedCount habit(s) and To-Do tasks!")
        } catch (e: Exception) {
            Pair(0, "Failed to import data: ${e.localizedMessage ?: "Invalid JSON format"}")
        }
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    private fun extractJsonString(objStr: String, key: String): String {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        val match = pattern.find(objStr)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun extractJsonInt(objStr: String, key: String, defaultVal: Int): Int {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        val match = pattern.find(objStr)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: defaultVal
    }

    private fun extractJsonLong(objStr: String, key: String, defaultVal: Long): Long {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        val match = pattern.find(objStr)
        return match?.groupValues?.get(1)?.toLongOrNull() ?: defaultVal
    }

    private fun extractJsonBoolean(objStr: String, key: String): Boolean {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        val match = pattern.find(objStr)
        return match?.groupValues?.get(1)?.toBoolean() ?: false
    }

    private fun extractJsonObjects(fullJson: String, arrayKey: String): List<String> {
        val keyIndex = fullJson.indexOf("\"$arrayKey\"")
        if (keyIndex == -1) return emptyList()

        val arrayStartIndex = fullJson.indexOf("[", keyIndex)
        val arrayEndIndex = fullJson.indexOf("]", arrayStartIndex)
        if (arrayStartIndex == -1 || arrayEndIndex == -1) return emptyList()

        val arrayContent = fullJson.substring(arrayStartIndex + 1, arrayEndIndex)
        val objPattern = "\\{[^{}]*\\}".toRegex()
        return objPattern.findAll(arrayContent).map { it.value }.toList()
    }
}
