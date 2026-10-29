package com.aayush.emergency.data.model

/**
 * Emergency case combining session data with hospital/transport status
 * Used for cross-device demo coordination
 */
data class EmergencyCase(
    val id: Long = 0L,
    val patientName: String = "",  // Demo identifier or actual name from consent
    val session: EmergencySession,
    val patientBrief: PatientBrief? = null,
    val transportStatus: TransportStatus = TransportStatus.NONE,
    val hospitalStatus: HospitalCaseStatus = HospitalCaseStatus.NEW,
    val assignedStaff: List<StaffAssignment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Staff assignment to a case
 */
data class StaffAssignment(
    val staffId: String = "",
    val staffName: String = "",
    val service: StaffService = StaffService.GENERAL_MEDICAL,
    val assignedAt: Long = System.currentTimeMillis()
)

/**
 * Transport/ambulance status for emergency response coordination
 * Explicitly simulated - no real ambulance dispatch
 */
enum class TransportStatus {
    NONE,               // No transport requested
    REQUESTED,          // Transport requested (simulated)
    ACKNOWLEDGED,       // Request acknowledged (simulated)
    EN_ROUTE,          // Transport en route (simulated)
    ARRIVED,           // Arrived at scene (simulated)
    TRANSPORTING,      // Transporting to hospital (simulated)
    COMPLETED          // Transport completed (simulated)
}

/**
 * Hospital case management status
 */
enum class HospitalCaseStatus {
    NEW,                    // New case, awaiting acknowledgement
    ACKNOWLEDGED,           // Case acknowledged by hospital
    STAFF_ASSIGNED,         // Staff/caregivers assigned
    CARE_IN_PROGRESS,       // Active care coordination
    COMPLETED,              // Case closed
    CANCELLED               // Case cancelled
}
