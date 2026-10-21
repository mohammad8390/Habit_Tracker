package com.example.habittracker.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.db.HabitDatabase

class HabitReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getLong("EXTRA_HABIT_ID", -1L)
        if (habitId == -1L) return Result.failure()

        val db = HabitDatabase.getDatabase(context)
        val habitEntity = db.habitDao().getHabitById(habitId) ?: return Result.success()
        val habit = habitEntity.toDomainModel()

        // Check active & reminder status
        if (habit.isReminderEnabled && !habit.isArchived) {
            NotificationHelper.showHabitNotification(
                context = context,
                habitId = habit.id,
                title = habit.title,
                description = habit.description
            )
        }

        return Result.success()
    }
}
