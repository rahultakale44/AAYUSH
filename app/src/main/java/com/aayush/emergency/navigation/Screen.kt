package com.aayush.emergency.navigation

/**
 * Sealed class representing all navigation destinations in the AAYUSH app
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Profile : Screen("profile")
    data object ProfilePersonalInfo : Screen("profile_personal_info")
    data object ProfileConditions : Screen("profile_conditions")
    data object ProfileMedications : Screen("profile_medications")
    data object ProfileAllergies : Screen("profile_allergies")
    data object ProfileSurgeries : Screen("profile_surgeries")
    data object ProfileContacts : Screen("profile_emergency_contacts")
    data object ProfileConsent : Screen("profile_consent")
    data object Contacts : Screen("contacts")
    data object Language : Screen("language")
    data object Journey : Screen("journey")
    data object EmergencyStart : Screen("emergency_start")
    data object SymptomCapture : Screen("symptom_capture")
    data object PatientBrief : Screen("patient_brief")
}
