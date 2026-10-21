package com.example.habittracker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitFrequencyType

@Entity(tableName = "habits")
data class HabitEntity(
    @field:PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    val categoryName: String,
    val colorHex: String,
    val iconName: String,
    val frequencyType: String,
    val targetDaysOfWeek: String, // Comma separated, e.g. "1,2,3,4,5,6,7"
    val targetCount: Int,
    val unit: String?,
    val isReminderEnabled: Boolean,
    val reminderTimeString: String?,
    val isArchived: Boolean,
    val createdAtEpochDay: Long
) {
    fun toDomainModel(): Habit {
        val daysList = targetDaysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }

        return Habit(
            id = id,
            title = title,
            description = description,
            category = HabitCategory.fromName(categoryName),
            colorHex = colorHex,
            iconName = iconName,
            frequencyType = try {
                HabitFrequencyType.valueOf(frequencyType)
            } catch (e: Exception) {
                HabitFrequencyType.DAILY
            },
            targetDaysOfWeek = daysList,
            targetCount = targetCount,
            unit = unit,
            isReminderEnabled = isReminderEnabled,
            reminderTimeString = reminderTimeString,
            isArchived = isArchived,
            createdAtEpochDay = createdAtEpochDay
        )
    }

    companion object {
        fun fromDomainModel(habit: Habit): HabitEntity {
            return HabitEntity(
                id = habit.id,
                title = habit.title,
                description = habit.description,
                categoryName = habit.category.name,
                colorHex = habit.colorHex,
                iconName = habit.iconName,
                frequencyType = habit.frequencyType.name,
                targetDaysOfWeek = habit.targetDaysOfWeek.joinToString(","),
                targetCount = habit.targetCount,
                unit = habit.unit,
                isReminderEnabled = habit.isReminderEnabled,
                reminderTimeString = habit.reminderTimeString,
                isArchived = habit.isArchived,
                createdAtEpochDay = habit.createdAtEpochDay
            )
        }
    }
}
