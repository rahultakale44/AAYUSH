package com.aayush.emergency.data.model

data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
