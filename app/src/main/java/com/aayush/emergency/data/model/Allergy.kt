package com.aayush.emergency.data.model

data class Allergy(
    val id: Long = 0,
    val allergyName: String,
    val reaction: String = "",
    val severity: String = "",
    val notes: String = "",
    val isVerified: Boolean = false,
    val verificationSource: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
