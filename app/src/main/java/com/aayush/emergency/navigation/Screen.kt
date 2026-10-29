package com.aayush.emergency.navigation

/**
 * Sealed class representing all navigation destinations in the AAYUSH app
 */
sealed class Screen(val route: String) {
    // Phase 1: Onboarding
    data object Splash : Screen("splash")
    data object RoleSelection : Screen("role_selection")
    data object Login : Screen("login/{role}") {
        fun createRoute(role: String) = "login/$role"
    }
    data object SignUp : Screen("signup/{role}") {
        fun createRoute(role: String) = "signup/$role"
    }
    data object ForgotPassword : Screen("forgot_password/{role}") {
        fun createRoute(role: String) = "forgot_password/$role"
    }
    
    // Senior Citizen screens
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
    data object UrgencyAssessment : Screen("urgency_assessment")
    data object PatientBrief : Screen("patient_brief")
    data object VoiceAssistance : Screen("voice_assistance")
    data object DiagnosticVoice : Screen("diagnostic_voice")
    
    // Multi-role dashboards
    data object TransportDashboard : Screen("transport_dashboard")
    data object HospitalDashboard : Screen("hospital_dashboard")
    data object HospitalCaseDetail : Screen("hospital_case_detail/{caseId}") {
        fun createRoute(caseId: Long) = "hospital_case_detail/$caseId"
    }
}
