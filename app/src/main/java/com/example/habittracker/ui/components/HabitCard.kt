package com.example.habittracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitWithProgress

@Composable
fun HabitCard(
    habitWithProgress: HabitWithProgress,
    onToggleComplete: (Long) -> Unit,
    onClick: (Long) -> Unit,
    isFutureDate: Boolean = false,
    onStartTimer: ((Habit) -> Unit)? = null,
    onToggleFreeze: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val habit = habitWithProgress.habit
    val isCompleted = habitWithProgress.isCompleted
    val isFrozen = habitWithProgress.isFrozen
    val completedCount = habitWithProgress.completedCount
    val targetCount = habit.targetCount

    val categoryColor = parseCategoryColor(habit.colorHex, habit.category)

    val isTimeBased = remember(habit.unit) {
        val unit = habit.unit?.lowercase() ?: ""
        unit.contains("min") || unit.contains("hour")
    }

    val checkBgColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primary
            isFrozen -> Color(0xFF3B82F6) // Freeze Blue
            isFutureDate -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "CheckBgAnimation"
    )

    val checkIconColor by animateColorAsState(
        targetValue = if (isCompleted || isFrozen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "CheckIconAnimation"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isCompleted || isFrozen) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CheckScaleAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(habit.id) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                isFrozen -> Color(0xFF3B82F6).copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(categoryColor.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = getCategoryIcon(habit.category),
                    contentDescription = habit.category.displayName,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Habit Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isFrozen) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = "Streak Frozen",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Frozen ❄️",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF3B82F6),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else if (habitWithProgress.currentStreak > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StreakBadge(streakCount = habitWithProgress.currentStreak)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = habit.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = if (targetCount > 1) {
                            "$completedCount/$targetCount ${habit.unit ?: "times"}"
                        } else {
                            habit.unit ?: "Daily"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Focus Timer Icon Button if Time-based Habit
            if (isTimeBased && onStartTimer != null && !isCompleted && !isFrozen && !isFutureDate) {
                IconButton(
                    onClick = { onStartTimer(habit) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Start Focus Timer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Individual Streak Freeze Button
            if (onToggleFreeze != null && !isCompleted && !isFutureDate) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFreeze(habit.id)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = if (isFrozen) "Unfreeze Habit" else "Freeze Habit",
                        tint = if (isFrozen) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Quick Toggle Complete Button (44dp touch target)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(checkBgColor)
                    .clickable(enabled = !isFutureDate) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleComplete(habit.id)
                    }
            ) {
                Icon(
                    imageVector = if (isFrozen) Icons.Default.AcUnit else Icons.Default.Check,
                    contentDescription = if (isCompleted) "Completed - Tap to undo" else "Mark Complete",
                    tint = checkIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun parseCategoryColor(hex: String, category: HabitCategory): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        when (category) {
            HabitCategory.HEALTH -> Color(0xFF059669)
            HabitCategory.FITNESS -> Color(0xFFE11D48)
            HabitCategory.PRODUCTIVITY -> Color(0xFF2563EB)
            HabitCategory.MINDFULNESS -> Color(0xFFD97706)
            HabitCategory.LEARNING -> Color(0xFF4F46E5)
            HabitCategory.FINANCE -> Color(0xFF0D9488)
            HabitCategory.GENERAL -> Color(0xFF7C3AED)
        }
    }
}

private fun getCategoryIcon(category: HabitCategory): ImageVector {
    return when (category) {
        HabitCategory.HEALTH -> Icons.Default.LocalDrink
        HabitCategory.FITNESS -> Icons.Default.DirectionsRun
        HabitCategory.PRODUCTIVITY -> Icons.Default.CheckCircle
        HabitCategory.MINDFULNESS -> Icons.Default.SelfImprovement
        HabitCategory.LEARNING -> Icons.Default.MenuBook
        HabitCategory.FINANCE -> Icons.Default.Favorite
        HabitCategory.GENERAL -> Icons.Default.Star
    }
}
