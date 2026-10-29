package com.aayush.emergency.data.model

/**
 * Demo user account for competition demonstration
 * 
 * IMPORTANT: This is a demo-only model for competition presentation.
 * - Passwords are NOT hashed (demo only)
 * - Local DataStore persistence only (no backend)
 * - NOT production-ready authentication
 * - Clearly labeled as fictional/demo data
 */
data class DemoUser(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val passwordHash: String,  // In demo: simple hash, NOT production-secure
    val phone: String = "",
    val preferredLanguage: String = "en",  // en, hi, mr
    val isDemo: Boolean = true  // Flag to identify seed/demo accounts
)

/**
 * Current session state
 */
data class UserSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val isDemo: Boolean
)
