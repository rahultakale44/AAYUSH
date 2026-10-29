package com.aayush.emergency.data.model

/**
 * Information about a reported symptom
 * Captures what the user reports along with timing
 */
data class SymptomInfo(
    val id: Long = 0L,
    val description: String = "",
    val severity: SymptomSeverity = SymptomSeverity.UNKNOWN,
    val duration: String = "",  // e.g., "2 hours", "since morning"
    val notes: String = "",
    val reportedAt: Long = 0L
)

enum class SymptomSeverity {
    MILD,
    MODERATE,
    SEVERE,
    UNKNOWN
}
