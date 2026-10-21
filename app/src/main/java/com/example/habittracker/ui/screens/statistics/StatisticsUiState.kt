package com.example.habittracker.ui.screens.statistics

import com.example.habittracker.domain.model.HabitStats
import com.example.habittracker.domain.model.StatsPeriod
import java.time.LocalDate
import java.time.YearMonth

data class StatisticsUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val currentYearMonth: YearMonth = YearMonth.now(),
    val stats: HabitStats = HabitStats(),
    val completedDatesForMonth: Set<LocalDate> = emptySet(),
    val frozenDatesForMonth: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = false
)
