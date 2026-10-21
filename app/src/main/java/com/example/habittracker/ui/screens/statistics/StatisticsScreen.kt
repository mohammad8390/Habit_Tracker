package com.example.habittracker.ui.screens.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.domain.model.HabitCategory
import com.example.habittracker.domain.model.HabitPerformanceData
import com.example.habittracker.domain.model.StatsPeriod
import com.example.habittracker.domain.model.WeeklyDayBarData
import com.example.habittracker.ui.components.MonthCalendarView
import com.example.habittracker.ui.components.StreakBadge

@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onPeriodSelect: (StatsPeriod) -> Unit,
    onDateSelect: (java.time.LocalDate) -> Unit,
    onYearMonthChange: (java.time.YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = uiState.stats

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Text(
                text = "Analytics & History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Period Selector Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    StatsPeriod.WEEK to "This Week",
                    StatsPeriod.MONTH to "This Month"
                ).forEach { (periodOption, label) ->
                    val isSelected = uiState.period == periodOption
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPeriodSelect(periodOption) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Activity Bar Chart (When This Week Selected)
            if (uiState.period == StatsPeriod.WEEK && stats.weeklyDayCounts.isNotEmpty()) {
                Text(
                    text = "Weekly Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                WeeklyBarChartCard(dayCounts = stats.weeklyDayCounts)

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Monthly History Calendar
            Text(
                text = "History Calendar 📅",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            MonthCalendarView(
                currentYearMonth = uiState.currentYearMonth,
                selectedDate = uiState.selectedDate,
                completedDates = uiState.completedDatesForMonth,
                frozenDates = uiState.frozenDatesForMonth,
                onMonthChange = onYearMonthChange,
                onDateSelected = onDateSelect
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Habit Days Completed Leaderboard (#1, #2, #3)
            if (stats.habitPerformanceList.isNotEmpty()) {
                Text(
                    text = "Habit Days Completed 🏆",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stats.habitPerformanceList.forEachIndexed { index, perf ->
                        HabitLeaderboardRow(
                            rank = index + 1,
                            perfData = perf,
                            period = uiState.period
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun WeeklyBarChartCard(
    dayCounts: List<WeeklyDayBarData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dayCounts.forEach { bar ->
                    val maxVal = dayCounts.maxOfOrNull { it.completedCount }?.coerceAtLeast(1) ?: 1
                    val fillRatio = if (bar.completedCount > 0 && maxVal > 0) {
                        (bar.completedCount.toFloat() / maxVal.toFloat()).coerceIn(0.1f, 1f)
                    } else {
                        0f
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = bar.completedCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bar.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(60.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((60 * fillRatio).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (bar.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = bar.dayName.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bar.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitLeaderboardRow(
    rank: Int,
    perfData: HabitPerformanceData,
    period: StatsPeriod
) {
    val habit = perfData.habit
    val categoryColor = parseCategoryColor(habit.colorHex)
    val periodLabel = if (period == StatsPeriod.WEEK) "this week" else "this month"

    val rankBadgeBg = when (rank) {
        1 -> Color(0xFFF59E0B).copy(alpha = 0.2f) // Gold
        2 -> Color(0xFF94A3B8).copy(alpha = 0.2f) // Silver
        3 -> Color(0xFFD97706).copy(alpha = 0.2f) // Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val rankTextColor = when (rank) {
        1 -> Color(0xFFF59E0B)
        2 -> Color(0xFF64748B)
        3 -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rankBadgeBg,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rank",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = rankTextColor,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = getCategoryIcon(habit.category),
                        contentDescription = habit.category.displayName,
                        tint = categoryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${perfData.totalCompletions} days completed $periodLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (perfData.currentStreak > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                StreakBadge(streakCount = perfData.currentStreak)
            }
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

private fun getCategoryIcon(category: HabitCategory): ImageVector {
    return when (category) {
        HabitCategory.HEALTH -> Icons.Default.LocalDrink
        HabitCategory.FITNESS -> Icons.Default.DirectionsRun
        HabitCategory.PRODUCTIVITY -> Icons.Default.Star
        HabitCategory.MINDFULNESS -> Icons.Default.SelfImprovement
        HabitCategory.LEARNING -> Icons.Default.MenuBook
        HabitCategory.FINANCE -> Icons.Default.Favorite
        HabitCategory.GENERAL -> Icons.Default.Star
    }
}
