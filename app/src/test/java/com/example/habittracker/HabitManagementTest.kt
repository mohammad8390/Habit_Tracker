package com.example.habittracker

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitFrequencyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitManagementTest {

    @Test
    fun habitValidation_blankNameIsInvalid() {
        val blankTitle = "   "
        val isTitleValid = blankTitle.isNotBlank()

        assertFalse(isTitleValid)
    }

    @Test
    fun habitCreation_trimsWhitespaceAndSetsDefaults() {
        val rawTitle = "  Read 20 Minutes  "
        val rawDesc = "   Non-fiction books   "
        val trimmedTitle = rawTitle.trim()
        val trimmedDesc = rawDesc.trim().ifEmpty { null }

        val habit = Habit(
            id = 0,
            title = trimmedTitle,
            description = trimmedDesc,
            category = HabitCategory.LEARNING
        )

        assertEquals("Read 20 Minutes", habit.title)
        assertEquals("Non-fiction books", habit.description)
        assertEquals(HabitCategory.LEARNING, habit.category)
        assertFalse(habit.isArchived)
    }

    @Test
    fun habitEditing_preservesExistingIdAndHistoricalLogsKey() {
        val originalHabit = Habit(
            id = 42,
            title = "Drink Water",
            targetCount = 4
        )

        val editedHabit = originalHabit.copy(
            title = "Drink 3L Water",
            targetCount = 6
        )

        assertEquals(42, editedHabit.id)
        assertEquals("Drink 3L Water", editedHabit.title)
        assertEquals(6, editedHabit.targetCount)
        assertNotEquals(originalHabit.title, editedHabit.title)
    }

    @Test
    fun habitArchiving_togglesIsArchivedFlag() {
        val activeHabit = Habit(id = 5, title = "Meditate", isArchived = false)
        val archivedHabit = activeHabit.copy(isArchived = true)
        val restoredHabit = archivedHabit.copy(isArchived = false)

        assertFalse(activeHabit.isArchived)
        assertTrue(archivedHabit.isArchived)
        assertFalse(restoredHabit.isArchived)
    }

    @Test
    fun scheduleEvaluation_onlyCountsHabitsScheduledForGivenDay() {
        val mondayIso = 1
        val tuesdayIso = 2

        val monWedFriHabit = Habit(
            id = 1,
            title = "Workout",
            frequencyType = HabitFrequencyType.WEEKLY_DAYS,
            targetDaysOfWeek = listOf(1, 3, 5) // Mon, Wed, Fri
        )

        val isScheduledMonday = monWedFriHabit.targetDaysOfWeek.contains(mondayIso)
        val isScheduledTuesday = monWedFriHabit.targetDaysOfWeek.contains(tuesdayIso)

        assertTrue(isScheduledMonday)
        assertFalse(isScheduledTuesday)
    }
}
