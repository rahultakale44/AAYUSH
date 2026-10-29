package com.aayush.emergency.data.model

/**
 * Represents an active or past emergency session
 * Contains timing, symptoms, triage results, and status tracking
 */
data class EmergencySession(
    val id: Long = 0L,
    val startTime: Long = 0L,
    val symptoms: List<SymptomInfo> = emptyList(),
    val triageResult: TriageResult? = null,
    val status: EmergencyStatus = EmergencyStatus.STARTED,
    val notes: String = "",
    val endTime: Long? = null
)

enum class EmergencyStatus {
    STARTED,        // Emergency session initiated
    SYMPTOMS_CAPTURED,  // User has provided symptoms
    TRIAGED,        // Triage assessment completed
    COMPLETED,      // Emergency resolved or closed
    CANCELLED       // Session cancelled by user
}
