package com.example.habittracker.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.habittracker.data.local.db.HabitDatabase
import com.example.habittracker.domain.model.Habit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class HabitBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HabitDatabase.getDatabase(context.applicationContext)
                    val activeHabits = db.habitDao().getAllActiveHabits().firstOrNull() ?: emptyList()

                    val scheduler = HabitReminderScheduler(context.applicationContext)
                    activeHabits.forEach { habitEntity ->
                        val habit = habitEntity.toDomainModel()
                        if (habit.isReminderEnabled && !habit.isArchived) {
                            scheduler.scheduleOrUpdateReminder(habit)
                        }
                    }

                    // Reschedule pending To-Do task reminders
                    val todayEpoch = LocalDate.now().toEpochDay()
                    val pendingTasks = db.todoDao().getTasksForDate(todayEpoch).firstOrNull() ?: emptyList()
                    pendingTasks.filter { it.isReminderEnabled && !it.isCompleted && !it.reminderTimeString.isNullOrBlank() }.forEach { taskEntity ->
                        val tempHabit = Habit(
                            id = taskEntity.id + 90000,
                            title = "To-Do: ${taskEntity.title}",
                            isReminderEnabled = true,
                            reminderTimeString = taskEntity.reminderTimeString
                        )
                        scheduler.scheduleOrUpdateReminder(tempHabit)
                    }
                } catch (e: Exception) {
                    // Ignore background errors
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
