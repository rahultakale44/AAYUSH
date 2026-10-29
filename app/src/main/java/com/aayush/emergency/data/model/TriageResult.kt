package com.aayush.emergency.data.model

/**
 * Result of rule-based triage assessment
 * Determines urgency level and provides guidance
 */
data class TriageResult(
    val urgencyLevel: UrgencyLevel = UrgencyLevel.UNKNOWN,
    val redFlagsDetected: List<String> = emptyList(),
    val guidance: String = "",
    val shouldCall112: Boolean = false,
    val assessmentTime: Long = 0L,
    val rulesFired: List<String> = emptyList()  // Which triage rules were triggered
)

enum class UrgencyLevel {
    IMMEDIATE,      // Red flags detected - call 112 immediately
    URGENT,         // Significant concern - seek immediate medical attention
    MODERATE,       // Should see a doctor soon
    LOW,            // Non-urgent, can schedule appointment
    UNKNOWN         // Unable to assess
}
