package com.example.habittracker.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.habittracker.MainActivity
import com.example.habittracker.R

object NotificationHelper {

    const val CHANNEL_ID = "habit_reminders_channel"
    const val CHANNEL_NAME = "Habit Reminders"

    @Volatile
    private var activeRingtone: Ringtone? = null

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily notifications and reminders to complete your scheduled habits."
                enableVibration(true)
                setSound(alarmSound, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun playLoopingAlarm(context: Context) {
        try {
            stopLoopingAlarm()
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.audioAttributes = audioAttributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true
            }
            ringtone.play()
            activeRingtone = ringtone
        } catch (e: Exception) {
            // Ignore ringtone error
        }
    }

    fun stopLoopingAlarm() {
        try {
            activeRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            activeRingtone = null
        } catch (e: Exception) {
            // Ignore stop error
        }
    }

    fun showHabitNotification(
        context: Context,
        habitId: Long,
        title: String,
        description: String?
    ) {
        createNotificationChannel(context)

        // Start continuous looping alarm ringtone
        playLoopingAlarm(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_HABIT_ID", habitId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Incremental Progress
        val progressIntent = Intent(context, HabitNotificationActionReceiver::class.java).apply {
            action = HabitNotificationActionReceiver.ACTION_INCREMENT_HABIT
            putExtra("EXTRA_HABIT_ID", habitId)
        }
        val progressPendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 1).toInt(),
            progressIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 5 Minutes
        val snoozeIntent = Intent(context, HabitNotificationActionReceiver::class.java).apply {
            action = HabitNotificationActionReceiver.ACTION_SNOOZE_NOTIFICATION
            putExtra("EXTRA_HABIT_ID", habitId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Turn Off / Dismiss Alarm
        val dismissIntent = Intent(context, HabitNotificationActionReceiver::class.java).apply {
            action = HabitNotificationActionReceiver.ACTION_DISMISS_NOTIFICATION
            putExtra("EXTRA_HABIT_ID", habitId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 3).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("⏰ Reminder: $title")
            .setContentText(description ?: "Time to complete your habit for today!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(R.drawable.ic_widget_check, "+1 Progress", progressPendingIntent)
            .addAction(R.drawable.ic_widget_check, "Snooze 5M ⏰", snoozePendingIntent)
            .addAction(R.drawable.ic_widget_check, "Turn Off 🔔", dismissPendingIntent)

        val notification = builder.build().apply {
            flags = flags or Notification.FLAG_INSISTENT
        }

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(habitId.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission denied by user
        }
    }
}
