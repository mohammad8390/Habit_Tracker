package com.example.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.local.db.HabitDatabase
import com.example.habittracker.data.repository.HabitRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_HABIT_FROM_WIDGET) {
            val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
            if (habitId != -1L) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = HabitDatabase.getDatabase(context.applicationContext)
                        val repository = HabitRepositoryImpl(db.habitDao(), null, context.applicationContext)
                        repository.toggleHabitCompletion(habitId, LocalDate.now())
                        notifyWidgetUpdate(context.applicationContext)
                    } catch (e: Exception) {
                        // Ignore widget toggle error
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_HABIT_FROM_WIDGET = "com.example.habittracker.ACTION_TOGGLE_HABIT_FROM_WIDGET"
        const val EXTRA_HABIT_ID = "EXTRA_HABIT_ID"

        fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HabitDatabase.getDatabase(context.applicationContext)
                    val today = LocalDate.now()
                    val todayEpoch = today.toEpochDay()
                    val dayOfWeekIso = today.dayOfWeek.value

                    val activeHabitEntities = db.habitDao().getAllActiveHabits().firstOrNull() ?: emptyList()
                    val todayLogs = db.habitDao().getLogsForDateOnce(todayEpoch)
                    val logMap = todayLogs.associateBy { it.habitId }

                    val scheduledHabits = activeHabitEntities
                        .map { it.toDomainModel() }
                        .filter { habit ->
                            when (habit.frequencyType) {
                                com.example.habittracker.domain.model.HabitFrequencyType.DAILY -> true
                                com.example.habittracker.domain.model.HabitFrequencyType.WEEKLY_DAYS -> habit.targetDaysOfWeek.contains(dayOfWeekIso)
                                com.example.habittracker.domain.model.HabitFrequencyType.TIMES_PER_WEEK -> true
                            }
                        }

                    val totalCount = scheduledHabits.size
                    val completedCount = scheduledHabits.count { habit ->
                        val log = logMap[habit.id]
                        log?.isCompleted == true || ((log?.completedCount ?: 0) >= habit.targetCount && habit.targetCount > 0)
                    }

                    val progressPercent = if (totalCount > 0) ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 0
                    val dateFormatted = today.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

                    appWidgetIds.forEach { widgetId ->
                        val views = RemoteViews(context.packageName, R.layout.widget_habit_tracker).apply {
                            setTextViewText(R.id.widget_date, dateFormatted)

                            if (totalCount == 0) {
                                setTextViewText(R.id.widget_status_text, "No habits scheduled for today")
                                setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                                setTextViewText(R.id.widget_subhead, "Tap below to add habits")
                            } else {
                                setTextViewText(R.id.widget_status_text, "$completedCount of $totalCount Completed ($progressPercent%)")
                                setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)
                                setTextViewText(
                                    R.id.widget_subhead,
                                    if (completedCount == totalCount) "All done for today! 🎉" else "Keep building your momentum! 🔥"
                                )
                            }

                            // Interactive Direct Check-Off List for up to 3 today habits
                            val maxVisibleHabits = scheduledHabits.take(3)
                            if (maxVisibleHabits.isNotEmpty()) {
                                val firstHabit = maxVisibleHabits[0]
                                val isFirstCompleted = logMap[firstHabit.id]?.isCompleted == true

                                setTextViewText(R.id.widget_subhead, "${firstHabit.title} (${if (isFirstCompleted) "Done ✓" else "Pending"})")

                                // Direct Check-off Click Intent
                                val toggleIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                                    action = ACTION_TOGGLE_HABIT_FROM_WIDGET
                                    putExtra(EXTRA_HABIT_ID, firstHabit.id)
                                }
                                val togglePendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    firstHabit.id.toInt(),
                                    toggleIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                setOnClickPendingIntent(R.id.widget_action_btn, togglePendingIntent)
                                setTextViewText(R.id.widget_action_btn, if (isFirstCompleted) "✓ ${firstHabit.title} Completed" else "Check-Off ${firstHabit.title}")
                            } else {
                                // Default Tap Widget Intent -> Open MainActivity
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                val pendingIntent = PendingIntent.getActivity(
                                    context,
                                    0,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                setOnClickPendingIntent(R.id.widget_action_btn, pendingIntent)
                                setTextViewText(R.id.widget_action_btn, "Open Today's Habits")
                            }

                            // Tap Widget Background -> Open MainActivity
                            val appIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            val appPendingIntent = PendingIntent.getActivity(
                                context,
                                0,
                                appIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            setOnClickPendingIntent(R.id.widget_root, appPendingIntent)
                        }

                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                } catch (e: Exception) {
                    // Ignore background widget update error
                }
            }
        }

        fun notifyWidgetUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HabitWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }
    }
}
