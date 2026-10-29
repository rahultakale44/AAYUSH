package com.aayush.emergency.data.model

/**
 * Hospital staff member for caregiver/staff management
 * Demo data - not real medical credentials
 */
data class HospitalStaff(
    val id: String = "",
    val displayName: String = "",
    val service: StaffService = StaffService.GENERAL_MEDICAL,
    val availability: StaffAvailability = StaffAvailability.AVAILABLE,
    val assignedCases: List<Long> = emptyList()
)

enum class StaffService {
    EMERGENCY_NURSE,
    TRIAGE_NURSE,
    GENERAL_MEDICAL,
    CARDIAC_RESPONSE,
    PATIENT_COORDINATION,
    RESPIRATORY_SPECIALIST,
    TRAUMA_TEAM
}

enum class StaffAvailability {
    AVAILABLE,
    BUSY,
    OFF_DUTY
}
