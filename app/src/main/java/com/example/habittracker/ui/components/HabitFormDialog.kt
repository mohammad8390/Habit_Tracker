package com.example.habittracker.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitFrequencyType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitFormSheet(
    habitToEdit: Habit? = null,
    onDismiss: () -> Unit,
    onSaveHabit: (Habit) -> Unit,
    onDeleteHabit: ((Long) -> Unit)? = null,
    onArchiveHabit: ((Long) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var description by remember { mutableStateOf(habitToEdit?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(habitToEdit?.category ?: HabitCategory.HEALTH) }
    var frequencyType by remember { mutableStateOf(habitToEdit?.frequencyType ?: HabitFrequencyType.DAILY) }
    var selectedDays by remember {
        mutableStateOf(habitToEdit?.targetDaysOfWeek?.toSet() ?: setOf(1, 2, 3, 4, 5, 6, 7))
    }
    var targetCountString by remember { mutableStateOf((habitToEdit?.targetCount ?: 1).toString()) }
    var unitString by remember { mutableStateOf(habitToEdit?.unit ?: "times") }
    var isReminderEnabled by remember { mutableStateOf(habitToEdit?.isReminderEnabled ?: false) }
    var reminderTimes by remember {
        mutableStateOf(habitToEdit?.reminderTimesList?.ifEmpty { listOf("08:00 AM") } ?: listOf("08:00 AM"))
    }

    var isTitleError by remember { mutableStateOf(false) }

    val daysOfWeekMap = remember {
        listOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
    }

    fun openTimePickerForNewTime() {
        val picker = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val localTime = LocalTime.of(hourOfDay, minute)
                val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
                val newFormattedTime = localTime.format(formatter)
                if (!reminderTimes.contains(newFormattedTime)) {
                    reminderTimes = (reminderTimes + newFormattedTime).sorted()
                }
            },
            8,
            0,
            false
        )
        picker.show()
    }

    fun openTimePickerForEditing(index: Int, currentTimeStr: String) {
        val hour = parseHour(currentTimeStr)
        val minute = parseMinute(currentTimeStr)
        val picker = TimePickerDialog(
            context,
            { _, hourOfDay, min ->
                val localTime = LocalTime.of(hourOfDay, min)
                val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
                val updatedTime = localTime.format(formatter)
                val updatedList = reminderTimes.toMutableList()
                if (index in updatedList.indices) {
                    updatedList[index] = updatedTime
                    reminderTimes = updatedList.distinct().sorted()
                }
            },
            hour,
            minute,
            false
        )
        picker.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (habitToEdit == null) "Create New Habit" else "Edit Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Habit Name
            Text(
                text = "Habit Name *",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) isTitleError = false
                },
                placeholder = { Text("e.g. Drink 2L Water, Morning Run") },
                isError = isTitleError,
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (isTitleError) {
                Text(
                    text = "Habit name cannot be empty",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "Description (Optional)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("e.g. Keep a water bottle at desk") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HabitCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    val categoryColor = parseCategoryColor(category.colorHex)

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) categoryColor else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedCategory = category }
                    ) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Schedule Selection
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (frequencyType == HabitFrequencyType.DAILY) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            frequencyType = HabitFrequencyType.DAILY
                            selectedDays = setOf(1, 2, 3, 4, 5, 6, 7)
                        }
                ) {
                    Text(
                        text = "Every Day",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (frequencyType == HabitFrequencyType.DAILY) FontWeight.Bold else FontWeight.Medium,
                        color = if (frequencyType == HabitFrequencyType.DAILY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (frequencyType == HabitFrequencyType.WEEKLY_DAYS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { frequencyType = HabitFrequencyType.WEEKLY_DAYS }
                ) {
                    Text(
                        text = "Specific Days",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (frequencyType == HabitFrequencyType.WEEKLY_DAYS) FontWeight.Bold else FontWeight.Medium,
                        color = if (frequencyType == HabitFrequencyType.WEEKLY_DAYS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                    )
                }
            }

            // Weekday Chips if Specific Days selected
            if (frequencyType == HabitFrequencyType.WEEKLY_DAYS) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    daysOfWeekMap.forEach { (dayIso, label) ->
                        val isDaySelected = selectedDays.contains(dayIso)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                selectedDays = if (isDaySelected) {
                                    if (selectedDays.size > 1) selectedDays - dayIso else selectedDays
                                } else {
                                    selectedDays + dayIso
                                }
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDaySelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Goal Target & Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Goal Target",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = targetCountString,
                        onValueChange = { targetCountString = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "Unit Label",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = unitString,
                        onValueChange = { unitString = it },
                        placeholder = { Text("times, mins, glasses") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Reminder Toggle & Multiple Times List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Reminders",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isReminderEnabled) "${reminderTimes.size} time(s) set (Tap time to edit)" else "Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isReminderEnabled,
                    onCheckedChange = {
                        isReminderEnabled = it
                        if (it && reminderTimes.isEmpty()) {
                            reminderTimes = listOf("08:00 AM")
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Multiple Time Pills & Add Button if Reminder is Enabled
            if (isReminderEnabled) {
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    reminderTimes.forEachIndexed { index, timeStr ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .clickable { openTimePickerForEditing(index, timeStr) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Edit Time",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        reminderTimes = reminderTimes.filterIndexed { i, _ -> i != index }
                                        if (reminderTimes.isEmpty()) {
                                            isReminderEnabled = false
                                        }
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Time",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Add Time Button
                    OutlinedButton(
                        onClick = { openTimePickerForNewTime() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Time",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Time",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save CTA
            Button(
                onClick = {
                    if (title.isBlank()) {
                        isTitleError = true
                    } else {
                        val targetVal = targetCountString.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val daysList = if (frequencyType == HabitFrequencyType.DAILY) {
                            listOf(1, 2, 3, 4, 5, 6, 7)
                        } else {
                            selectedDays.ifEmpty { setOf(LocalDate.now().dayOfWeek.value) }.toList().sorted()
                        }

                        val reminderStr = if (isReminderEnabled && reminderTimes.isNotEmpty()) {
                            reminderTimes.joinToString(", ")
                        } else null

                        val newHabit = (habitToEdit ?: Habit(title = title.trim())).copy(
                            title = title.trim(),
                            description = description.trim().ifEmpty { null },
                            category = selectedCategory,
                            colorHex = selectedCategory.colorHex,
                            iconName = selectedCategory.iconName,
                            frequencyType = frequencyType,
                            targetDaysOfWeek = daysList,
                            targetCount = targetVal,
                            unit = unitString.trim().ifEmpty { "times" },
                            isReminderEnabled = isReminderEnabled,
                            reminderTimeString = reminderStr
                        )
                        onSaveHabit(newHabit)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (habitToEdit == null) "Create Habit" else "Save Changes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Archive / Delete Buttons if Editing Habit
            if (habitToEdit != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onArchiveHabit != null) {
                        OutlinedButton(
                            onClick = {
                                onArchiveHabit(habitToEdit.id)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Archive 📦", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (onDeleteHabit != null) {
                        OutlinedButton(
                            onClick = {
                                onDeleteHabit(habitToEdit.id)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete 🗑️", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun parseHour(timeString: String): Int {
    return try {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
        LocalTime.parse(timeString, formatter).hour
    } catch (e1: Exception) {
        try {
            val formatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
            LocalTime.parse(timeString, formatter24).hour
        } catch (e2: Exception) {
            8
        }
    }
}

private fun parseMinute(timeString: String): Int {
    return try {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
        LocalTime.parse(timeString, formatter).minute
    } catch (e1: Exception) {
        try {
            val formatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
            LocalTime.parse(timeString, formatter24).minute
        } catch (e2: Exception) {
            0
        }
    }
}

private fun parseCategoryColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF059669)
    }
}
