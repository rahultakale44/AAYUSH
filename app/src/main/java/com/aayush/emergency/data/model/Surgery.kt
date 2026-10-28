package com.aayush.emergency.data.model

data class Surgery(
    val id: Long = 0,
    val surgeryName: String,
    val date: String = "",
    val notes: String = "",
    val isVerified: Boolean = false,
    val verificationSource: String = "",
    val createdAt: Long = 0L
)
