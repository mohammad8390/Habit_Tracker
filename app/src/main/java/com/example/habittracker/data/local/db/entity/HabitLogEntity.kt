package com.example.habittracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.habittracker.domain.model.HabitLog

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "dateEpochDay"], unique = true)
    ]
)
data class HabitLogEntity(
    @field:PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val dateEpochDay: Long,
    val completedCount: Int,
    val isCompleted: Boolean,
    @ColumnInfo(defaultValue = "0")
    val isFrozen: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): HabitLog {
        return HabitLog(
            id = id,
            habitId = habitId,
            dateEpochDay = dateEpochDay,
            completedCount = completedCount,
            isCompleted = isCompleted,
            isFrozen = isFrozen,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    companion object {
        fun fromDomainModel(log: HabitLog): HabitLogEntity {
            return HabitLogEntity(
                id = log.id,
                habitId = log.habitId,
                dateEpochDay = log.dateEpochDay,
                completedCount = log.completedCount,
                isCompleted = log.isCompleted,
                isFrozen = log.isFrozen,
                updatedAtEpochMs = log.updatedAtEpochMs
            )
        }
    }
}
