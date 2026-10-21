package com.example.habittracker.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.domain.model.StatsPeriod
import com.example.habittracker.domain.repository.HabitRepository
import com.example.habittracker.domain.usecase.GetHabitStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class StatisticsViewModel(
    private val repository: HabitRepository,
    private val statsUseCase: GetHabitStatsUseCase = GetHabitStatsUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _period = MutableStateFlow(StatsPeriod.WEEK)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _yearMonth = MutableStateFlow(YearMonth.now())

    init {
        viewModelScope.launch {
            combine(
                repository.getAllActiveHabits(),
                repository.getAllLogs(),
                _period,
                _selectedDate,
                _yearMonth
            ) { habits, logs, period, selectedDate, yearMonth ->
                val today = LocalDate.now()
                val computedStats = statsUseCase.execute(habits, logs, period, today)

                val monthCompletedLogs = logs.filter { log ->
                    log.isCompleted &&
                            log.date.year == yearMonth.year &&
                            log.date.month == yearMonth.month
                }.map { it.date }.toSet()

                val monthFrozenLogs = logs.filter { log ->
                    log.isFrozen &&
                            log.date.year == yearMonth.year &&
                            log.date.month == yearMonth.month
                }.map { it.date }.toSet()

                _uiState.update { currentState ->
                    currentState.copy(
                        period = period,
                        selectedDate = selectedDate,
                        currentYearMonth = yearMonth,
                        stats = computedStats,
                        completedDatesForMonth = monthCompletedLogs,
                        frozenDatesForMonth = monthFrozenLogs,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun selectPeriod(period: StatsPeriod) {
        _period.value = period
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun changeYearMonth(yearMonth: YearMonth) {
        _yearMonth.value = yearMonth
    }
}
