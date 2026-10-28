package com.aayush.emergency.data.model

data class ConsentPreferences(
    val id: Long = 1L,
    val shareWithHospital: Boolean = false,
    val shareWithCaregivers: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
