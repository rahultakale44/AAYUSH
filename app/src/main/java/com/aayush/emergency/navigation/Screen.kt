package com.aayush.emergency.navigation

/**
 * Sealed class representing all navigation destinations in the AAYUSH app
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Profile : Screen("profile")
    data object Contacts : Screen("contacts")
    data object Language : Screen("language")
    data object Journey : Screen("journey")
    data object EmergencyStart : Screen("emergency_start")
    data object SymptomCapture : Screen("symptom_capture")
}
