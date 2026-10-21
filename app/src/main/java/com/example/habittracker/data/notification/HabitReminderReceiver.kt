package com.example.habittracker.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.habittracker.data.local.datastore.UserPreferencesRepository
import com.example.habittracker.data.local.db.HabitDatabase
import com.example.habittracker.domain.model.HabitFrequencyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra("EXTRA_HABIT_ID", -1L)
        if (habitId == -1L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if notifications are globally enabled in settings
                val userPrefsRepo = UserPreferencesRepository(context.applicationContext)
                val isNotifEnabled = userPrefsRepo.isNotificationsEnabledFlow.firstOrNull() ?: true
                if (!isNotifEnabled) {
                    return@launch
                }

                val db = HabitDatabase.getDatabase(context.applicationContext)
                val habitEntity = db.habitDao().getHabitById(habitId)
                val today = LocalDate.now()
                val todayEpoch = today.toEpochDay()

                if (habitEntity != null) {
                    val habit = habitEntity.toDomainModel()
                    if (habit.isReminderEnabled && !habit.isArchived) {
                        val isScheduledToday = when (habit.frequencyType) {
                            HabitFrequencyType.DAILY -> true
                            HabitFrequencyType.WEEKLY_DAYS -> habit.targetDaysOfWeek.contains(today.dayOfWeek.value)
                            HabitFrequencyType.TIMES_PER_WEEK -> true
                        }

                        val todayLog = db.habitDao().getLogForHabitAndDate(habit.id, todayEpoch)
                        val isAlreadyCompleted = todayLog?.isCompleted == true || ((todayLog?.completedCount ?: 0) >= habit.targetCount && habit.targetCount > 0)

                        if (isScheduledToday && !isAlreadyCompleted) {
                            NotificationHelper.showHabitNotification(
                                context = context.applicationContext,
                                habitId = habit.id,
                                title = habit.title,
                                description = habit.description
                            )
                        }

                        val scheduler = HabitReminderScheduler(context.applicationContext)
                        scheduler.scheduleOrUpdateReminder(habit)
                    }
                } else {
                    // Check if it's a To-Do Task reminder for today's due date
                    val rawTodoId = if (habitId >= 90000) habitId - 90000 else habitId
                    val todoEntity = db.todoDao().getTaskById(rawTodoId)
                    if (todoEntity != null && todoEntity.isReminderEnabled && !todoEntity.isCompleted && todoEntity.dueDateEpochDay == todayEpoch) {
                        NotificationHelper.showHabitNotification(
                            context = context.applicationContext,
                            habitId = habitId,
                            title = "To-Do Task: ${todoEntity.title}",
                            description = "Pending work reminder"
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore background errors
            } finally {
                pendingResult.finish()
            }
        }
    }
}
