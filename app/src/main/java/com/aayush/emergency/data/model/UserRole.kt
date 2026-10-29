package com.aayush.emergency.data.model

/**
 * User role for three-device demo
 * This is a prototype demonstration mechanism, not real authentication
 */
enum class UserRole {
    SENIOR_CITIZEN,          // Device 1: Senior using AAYUSH app
    TRANSPORT_COORDINATOR,   // Device 2: Emergency response/transport
    HOSPITAL_STAFF           // Device 3: Hospital operations dashboard
}
