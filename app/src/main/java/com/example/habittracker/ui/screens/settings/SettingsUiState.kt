package com.example.habittracker.ui.screens.settings

import com.example.habittracker.data.local.datastore.FirstDayOfWeekPref
import com.example.habittracker.data.local.datastore.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val firstDayOfWeek: FirstDayOfWeekPref = FirstDayOfWeekPref.MONDAY,
    val isHapticEnabled: Boolean = true,
    val isNotificationsEnabled: Boolean = true,
    val isNotificationPermissionGranted: Boolean = false,
    val exportJsonString: String? = null,
    val isExportDialogShowing: Boolean = false,
    val isImportDialogShowing: Boolean = false,
    val importInputText: String = "",
    val statusMessage: String? = null,
    val isResetConfirmShowing: Boolean = false
)
