package com.example.habittracker.domain.model

enum class HabitCategory(
    val displayName: String,
    val colorHex: String,
    val iconName: String
) {
    HEALTH("Health", "#059669", "Favorite"),
    FITNESS("Fitness", "#E11D48", "DirectionsRun"),
    PRODUCTIVITY("Productivity", "#2563EB", "CheckCircle"),
    MINDFULNESS("Mindfulness", "#D97706", "SelfImprovement"),
    LEARNING("Learning", "#4F46E5", "MenuBook"),
    FINANCE("Finance", "#0D9488", "AccountBalanceWallet"),
    GENERAL("General", "#7C3AED", "Star");

    companion object {
        fun fromName(name: String?): HabitCategory {
            return entries.find { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: GENERAL
        }
    }
}
