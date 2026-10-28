package com.aayush.emergency.data.model

/**
 * Medical condition with verification status
 */
data class MedicalCondition(
    val id: Long = 0,
    val conditionName: String,
    val notes: String = "",
    val isVerified: Boolean = false,
    val verificationSource: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
