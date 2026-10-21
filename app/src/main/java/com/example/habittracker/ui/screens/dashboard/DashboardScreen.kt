package com.example.habittracker.ui.screens.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.ui.components.CelebrationOverlay
import com.example.habittracker.ui.components.DashboardHeroCard
import com.example.habittracker.ui.components.DateStripBar
import com.example.habittracker.ui.components.EmptyStateView
import com.example.habittracker.ui.components.FocusTimerDialog
import com.example.habittracker.ui.components.HabitCard
import com.example.habittracker.ui.components.HabitFormSheet
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onToggleHabitCompletion: (Long) -> Unit,
    onToggleFreeze: (Long) -> Unit,
    onToggleFreezeAll: () -> Unit,
    onHabitClick: (Long) -> Unit,
    onStartTimer: (Habit) -> Unit,
    onTimerFinished: () -> Unit,
    onCloseTimer: () -> Unit,
    onAddHabitClick: () -> Unit,
    onSaveHabit: (Habit) -> Unit,
    onDeleteHabit: ((Long) -> Unit)? = null,
    onArchiveHabit: ((Long) -> Unit)? = null,
    onCloseFormSheet: () -> Unit,
    onSettingsClick: () -> Unit,
    onLoadSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
    val formattedDate = uiState.selectedDate.format(dateFormatter)
    var showCelebration by remember { mutableStateOf(false) }

    // Automatic Notification Permission Request on First Launch (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Header Row (Greeting on Left, Freeze ❄️ Button in Top Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = uiState.userGreeting,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Top Streak Freeze Action Button (Where Settings Gear Used To Be)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (uiState.isAnyHabitFrozen) Color(0xFF3B82F6) else Color(0xFF3B82F6).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        onToggleFreezeAll()
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = "Freeze Streak",
                            tint = if (uiState.isAnyHabitFrozen) Color.White else Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isAnyHabitFrozen) "Frozen ❄️" else "Freeze ❄️",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isAnyHabitFrozen) Color.White else Color(0xFF3B82F6)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Hero Progress Card
            DashboardHeroCard(
                completedCount = uiState.completedCount,
                totalCount = uiState.totalCount,
                overallStreak = uiState.overallStreak
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Interactive Month Date Strip
            DateStripBar(
                selectedDate = uiState.selectedDate,
                onDateSelected = onSelectDate
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (uiState.totalCount > 0) {
                    Text(
                        text = "${uiState.completedCount}/${uiState.totalCount} Done",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Habit List or Empty State
            if (uiState.habits.isEmpty()) {
                EmptyStateView(
                    title = "No habits scheduled for this day",
                    subtitle = "Tap below to create a new habit or load sample habits for testing.",
                    actionText = "Add Habit",
                    onActionClick = onAddHabitClick,
                    onLoadSampleDataClick = onLoadSampleData
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.habits,
                        key = { it.habit.id }
                    ) { habitWithProgress ->
                        HabitCard(
                            habitWithProgress = habitWithProgress,
                            isFutureDate = uiState.selectedDate.isAfter(java.time.LocalDate.now()),
                            onToggleComplete = { habitId ->
                                val progressItem = uiState.habits.find { it.habit.id == habitId }
                                if (progressItem != null && !progressItem.isCompleted) {
                                    showCelebration = true
                                }
                                onToggleHabitCompletion(habitId)
                            },
                            onClick = onHabitClick,
                            onStartTimer = onStartTimer,
                            onToggleFreeze = onToggleFreeze
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddHabitClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Habit",
                modifier = Modifier.size(24.dp)
            )
        }

        // Celebration Particle Burst & Audio Chime
        CelebrationOverlay(
            isVisible = showCelebration,
            onDismiss = { showCelebration = false }
        )
    }

    // Modal Bottom Sheet Form for Adding / Editing Habits
    if (uiState.isFormSheetOpen) {
        HabitFormSheet(
            habitToEdit = uiState.habitToEdit,
            onDismiss = onCloseFormSheet,
            onSaveHabit = onSaveHabit,
            onDeleteHabit = onDeleteHabit,
            onArchiveHabit = onArchiveHabit
        )
    }

    // Focus Timer Dialog
    if (uiState.isTimerOpen && uiState.habitForTimer != null) {
        FocusTimerDialog(
            habit = uiState.habitForTimer,
            onDismiss = onCloseTimer,
            onTimerFinished = {
                showCelebration = true
                onTimerFinished()
            }
        )
    }
}
