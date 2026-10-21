package com.example.habittracker.data.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.local.db.HabitDatabase
import com.example.habittracker.data.repository.HabitRepositoryImpl
import com.example.habittracker.widget.HabitWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

class FocusTimerService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var timerJob: Job? = null

    private var habitId: Long = -1L
    private var habitTitle: String = "Focus Session"
    private var totalSeconds: Int = 0
    private var secondsLeft: Int = 0
    private var isRunning: Boolean = false

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    data class TimerState(
        val habitId: Long = -1L,
        val habitTitle: String = "",
        val totalSeconds: Int = 0,
        val secondsLeft: Int = 0,
        val isRunning: Boolean = false,
        val isFinished: Boolean = false
    )

    inner class LocalBinder : Binder() {
        fun getService(): FocusTimerService = this@FocusTimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_TIMER -> {
                    habitId = it.getLongExtra(EXTRA_HABIT_ID, -1L)
                    habitTitle = it.getStringExtra(EXTRA_HABIT_TITLE) ?: "Focus Session"
                    totalSeconds = it.getIntExtra(EXTRA_TOTAL_SECONDS, 900)
                    secondsLeft = totalSeconds
                    startTimer()
                }
                ACTION_PAUSE_TIMER -> pauseTimer()
                ACTION_RESUME_TIMER -> resumeTimer()
                ACTION_STOP_TIMER -> stopTimer()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep running in background status bar even if user swipes away app from recent tasks
        if (isRunning && secondsLeft > 0) {
            updateNotification()
        }
    }

    private fun startTimer() {
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (secondsLeft > 0 && isRunning) {
                delay(1000L)
                if (isRunning) {
                    secondsLeft -= 1
                    updateNotification()
                    _timerState.value = TimerState(
                        habitId = habitId,
                        habitTitle = habitTitle,
                        totalSeconds = totalSeconds,
                        secondsLeft = secondsLeft,
                        isRunning = isRunning,
                        isFinished = false
                    )
                }
            }

            if (secondsLeft == 0) {
                onTimerComplete()
            }
        }
    }

    private fun pauseTimer() {
        isRunning = false
        updateNotification()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    private fun resumeTimer() {
        if (secondsLeft > 0) {
            isRunning = true
            startTimer()
        }
    }

    private fun stopTimer() {
        isRunning = false
        timerJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun onTimerComplete() {
        isRunning = false
        _timerState.value = _timerState.value.copy(isRunning = false, isFinished = true)

        if (habitId != -1L) {
            serviceScope.launch {
                try {
                    val db = HabitDatabase.getDatabase(applicationContext)
                    val repository = HabitRepositoryImpl(db.habitDao(), null, applicationContext)
                    val habitEntity = db.habitDao().getHabitById(habitId)
                    if (habitEntity != null) {
                        val habit = habitEntity.toDomainModel()
                        repository.setHabitCompletionCount(habitId, LocalDate.now(), habit.targetCount)
                        HabitWidgetProvider.notifyWidgetUpdate(applicationContext)
                    }
                } catch (e: Exception) {
                    // Ignore error
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, FocusTimerService::class.java).apply {
            action = if (isRunning) ACTION_PAUSE_TIMER else ACTION_RESUME_TIMER
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("⏱️ $habitTitle ($timeFormatted)")
            .setContentText(if (isRunning) "Focus session in progress..." else "Focus timer paused")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_widget_check, if (isRunning) "Pause" else "Resume", pausePendingIntent)
            .addAction(R.drawable.ic_widget_check, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer Live Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live ongoing focus timer progress in status bar / dynamic bar."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "focus_timer_live_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_TIMER = "ACTION_START_TIMER"
        const val ACTION_PAUSE_TIMER = "ACTION_PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "ACTION_RESUME_TIMER"
        const val ACTION_STOP_TIMER = "ACTION_STOP_TIMER"

        const val EXTRA_HABIT_ID = "EXTRA_HABIT_ID"
        const val EXTRA_HABIT_TITLE = "EXTRA_HABIT_TITLE"
        const val EXTRA_TOTAL_SECONDS = "EXTRA_TOTAL_SECONDS"
    }
}
