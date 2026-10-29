package com.aayush.emergency.data.model

/**
 * Generated patient brief for emergency responders
 * Combines emergency session data with relevant health profile information
 * Respects consent preferences
 */
data class PatientBrief(
    val generatedAt: Long = 0L,
    val emergencySession: EmergencySession? = null,
    val healthProfile: HealthProfile? = null,
    val relevantConditions: List<MedicalCondition> = emptyList(),
    val relevantMedications: List<Medication> = emptyList(),
    val relevantAllergies: List<Allergy> = emptyList(),
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val consentGiven: Boolean = false,
    val disclaimer: String = "This information is locally generated and user-provided. It has not been clinically verified or transmitted."
)
