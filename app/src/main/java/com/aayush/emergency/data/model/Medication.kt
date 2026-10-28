package com.aayush.emergency.data.model

data class Medication(
    val id: Long = 0,
    val medicationName: String,
    val dosage: String = "",
    val frequency: String = "",
    val notes: String = "",
    val isVerified: Boolean = false,
    val verificationSource: String = "",
    val createdAt: Long = 0L
)
