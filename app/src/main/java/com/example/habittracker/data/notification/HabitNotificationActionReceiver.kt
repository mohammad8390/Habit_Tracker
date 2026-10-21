package com.example.habittracker.data.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.habittracker.data.local.db.HabitDatabase
import com.example.habittracker.data.repository.HabitRepositoryImpl
import com.example.habittracker.widget.HabitWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class HabitNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra("EXTRA_HABIT_ID", -1L)
        if (habitId == -1L) return

        // Always stop the continuous alarm ringtone when an action is performed
        NotificationHelper.stopLoopingAlarm()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val action = intent.action ?: ACTION_INCREMENT_HABIT

        when (action) {
            ACTION_DISMISS_NOTIFICATION -> {
                notificationManager.cancel(habitId.toInt())
            }
            ACTION_SNOOZE_NOTIFICATION -> {
                notificationManager.cancel(habitId.toInt())
                scheduleSnoozeAlarm(context, habitId)
            }
            else -> { // Incremental complete or To-Do toggle
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = HabitDatabase.getDatabase(context.applicationContext)
                        if (habitId >= 90000) {
                            val rawTodoId = habitId - 90000
                            db.todoDao().toggleTaskCompletion(rawTodoId)
                            notificationManager.cancel(habitId.toInt())
                        } else {
                            val repository = HabitRepositoryImpl(db.habitDao(), null, context.applicationContext)
                            val habitEntity = db.habitDao().getHabitById(habitId)
                            if (habitEntity != null) {
                                val habit = habitEntity.toDomainModel()
                                val today = LocalDate.now()
                                val todayLog = db.habitDao().getLogForHabitAndDate(habitId, today.toEpochDay())
                                val currentCount = todayLog?.completedCount ?: 0

                                if (habit.targetCount > 1) {
                                    val newCount = (currentCount + 1).coerceAtMost(habit.targetCount)
                                    repository.setHabitCompletionCount(habitId, today, newCount)
                                } else {
                                    repository.toggleHabitCompletion(habitId, today)
                                }

                                HabitWidgetProvider.notifyWidgetUpdate(context.applicationContext)
                                notificationManager.cancel(habitId.toInt())
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore error
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun scheduleSnoozeAlarm(context: Context, habitId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("EXTRA_HABIT_ID", habitId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, (habitId * 1000 + 99).toInt(), intent, flags)

        val snoozeTimeMs = System.currentTimeMillis() + (5 * 60 * 1000L) // 5 Minutes Snooze

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMs, pendingIntent)
            }
        } catch (e: Exception) {
            // Fallback
            alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTimeMs, pendingIntent)
        }
    }

    companion object {
        const val ACTION_INCREMENT_HABIT = "com.example.habittracker.ACTION_INCREMENT_HABIT"
        const val ACTION_DISMISS_NOTIFICATION = "com.example.habittracker.ACTION_DISMISS_NOTIFICATION"
        const val ACTION_SNOOZE_NOTIFICATION = "com.example.habittracker.ACTION_SNOOZE_NOTIFICATION"
    }
}
