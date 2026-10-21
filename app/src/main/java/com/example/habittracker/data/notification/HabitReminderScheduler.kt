package com.example.habittracker.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.habittracker.domain.model.Habit
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HabitReminderScheduler(
    private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleOrUpdateReminder(habit: Habit) {
        // First cancel any existing alarms for this habit
        cancelReminder(habit.id)

        if (!habit.isReminderEnabled || habit.isArchived) return

        val timesList = habit.reminderTimesList
        if (timesList.isEmpty()) return

        timesList.forEachIndexed { index, timeStr ->
            val targetTime = parseReminderTime(timeStr) ?: LocalTime.of(8, 0)
            val triggerTimeMs = calculateNextTriggerTimeMs(targetTime)
            val requestCode = (habit.id * 100 + index).toInt()

            val pendingIntent = createPendingIntent(habit.id, requestCode)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                try {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } catch (e2: Exception) {
                    // Ignore schedule error
                }
            }
        }
    }

    fun cancelReminder(habitId: Long) {
        // Cancel up to 10 potential reminder times per habit
        for (index in 0..10) {
            val requestCode = (habitId * 100 + index).toInt()
            val pendingIntent = createPendingIntent(habitId, requestCode)
            try {
                alarmManager.cancel(pendingIntent)
            } catch (e: Exception) {
                // Ignore cancel failure
            }
        }
    }

    private fun createPendingIntent(habitId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("EXTRA_HABIT_ID", habitId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun calculateNextTriggerTimeMs(targetTime: LocalTime): Long {
        val now = LocalDateTime.now()
        var targetDateTime = now.withHour(targetTime.hour).withMinute(targetTime.minute).withSecond(0).withNano(0)

        if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        return targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun parseReminderTime(timeString: String?): LocalTime? {
        if (timeString.isNullOrBlank()) return null
        return try {
            val formatter12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
            LocalTime.parse(timeString, formatter12)
        } catch (e1: Exception) {
            try {
                val formatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
                LocalTime.parse(timeString, formatter24)
            } catch (e2: Exception) {
                null
            }
        }
    }
}
