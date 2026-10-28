package com.aayush.emergency.data.model

/**
 * Senior's basic health profile information
 */
data class HealthProfile(
    val id: Long = 1L,
    val fullName: String = "",
    val dateOfBirth: String = "",
    val notes: String = ""
)
