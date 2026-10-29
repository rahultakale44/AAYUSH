package com.aayush.emergency.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aayush.emergency.data.model.DemoUser
import com.aayush.emergency.data.model.UserRole
import com.aayush.emergency.data.model.UserSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/**
 * Authentication Repository for Demo Mode
 * 
 * DEMO LIMITATIONS - NOT PRODUCTION READY:
 * - Passwords stored with simple MD5 hash (NOT secure for production)
 * - Local DataStore only (no backend API)
 * - No token refresh, no secure session management
 * - Demo accounts clearly labeled
 * - Suitable for competition demonstration only
 */
class AuthRepository(private val context: Context) {
    
    private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "aayush_auth")
    private val gson = Gson()
    
    private val USERS_KEY = stringPreferencesKey("demo_users")
    private val SESSION_KEY = stringPreferencesKey("current_session")
    private val SEED_INITIALIZED_KEY = stringPreferencesKey("seed_initialized")
    
    // Current session flow
    val currentSession: Flow<UserSession?> = context.authDataStore.data.map { preferences ->
        preferences[SESSION_KEY]?.let { json ->
            gson.fromJson(json, UserSession::class.java)
        }
    }
    
    /**
     * Initialize seed data with fictional Indian family
     * Called once on first app launch
     */
    suspend fun initializeSeedData() {
        val seedInitialized = context.authDataStore.data.map { prefs ->
            prefs[SEED_INITIALIZED_KEY] == "true"
        }.first()
        
        if (!seedInitialized) {
            val demoAccounts = listOf(
                // Senior Citizen - Savita Deshmukh
                DemoUser(
                    id = "demo_senior_001",
                    email = "savita.deshmukh@demo.aayush.in",
                    fullName = "Savita Deshmukh",
                    role = UserRole.SENIOR_CITIZEN,
                    passwordHash = simpleHash("Demo123!"),
                    phone = "+91-98765-XXXXX",  // Fictional demo number
                    preferredLanguage = "mr",
                    isDemo = true
                ),
                // Transport Coordinator - Rohan Patil
                DemoUser(
                    id = "demo_transport_001",
                    email = "rohan.patil@demo.aayush.in",
                    fullName = "Rohan Patil",
                    role = UserRole.TRANSPORT_COORDINATOR,
                    passwordHash = simpleHash("Demo123!"),
                    phone = "+91-91234-XXXXX",
                    preferredLanguage = "mr",
                    isDemo = true
                ),
                // Hospital Staff - Dr. Meera Kulkarni
                DemoUser(
                    id = "demo_hospital_001",
                    email = "dr.meera.kulkarni@demo.aayush.in",
                    fullName = "Dr. Meera Kulkarni",
                    role = UserRole.HOSPITAL_STAFF,
                    passwordHash = simpleHash("Demo123!"),
                    phone = "+91-93456-XXXXX",
                    preferredLanguage = "hi",
                    isDemo = true
                )
            )
            
            saveUsers(demoAccounts)
            
            context.authDataStore.edit { prefs ->
                prefs[SEED_INITIALIZED_KEY] = "true"
            }
        }
    }
    
    /**
     * Login with email and password
     * Returns UserSession if successful, null if failed
     */
    suspend fun login(email: String, password: String): UserSession? {
        val users = getUsers()
        val user = users.find { it.email.equals(email, ignoreCase = true) }
        
        return if (user != null && user.passwordHash == simpleHash(password)) {
            val session = UserSession(
                userId = user.id,
                email = user.email,
                fullName = user.fullName,
                role = user.role,
                isDemo = user.isDemo
            )
            saveSession(session)
            session
        } else {
            null
        }
    }
    
    /**
     * Register new demo user
     */
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        role: UserRole,
        phone: String = "",
        preferredLanguage: String = "en"
    ): Result<UserSession> {
        // Check if email already exists
        val users = getUsers()
        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(Exception("Email already registered"))
        }
        
        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(Exception("Invalid email format"))
        }
        
        // Validate password (demo: just check not empty)
        if (password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters"))
        }
        
        // Create new user
        val newUser = DemoUser(
            id = "user_${System.currentTimeMillis()}",
            email = email,
            fullName = fullName,
            role = role,
            passwordHash = simpleHash(password),
            phone = phone,
            preferredLanguage = preferredLanguage,
            isDemo = false  // User-created, not seed data
        )
        
        // Save user
        val updatedUsers = users + newUser
        saveUsers(updatedUsers)
        
        // Create session
        val session = UserSession(
            userId = newUser.id,
            email = newUser.email,
            fullName = newUser.fullName,
            role = newUser.role,
            isDemo = newUser.isDemo
        )
        saveSession(session)
        
        return Result.success(session)
    }
    
    /**
     * Demo access - quick login without password
     */
    suspend fun demoLogin(role: UserRole): UserSession? {
        val users = getUsers()
        val demoUser = users.find { it.role == role && it.isDemo }
        
        return demoUser?.let {
            val session = UserSession(
                userId = it.id,
                email = it.email,
                fullName = it.fullName,
                role = it.role,
                isDemo = it.isDemo
            )
            saveSession(session)
            session
        }
    }
    
    /**
     * Logout - clear current session
     */
    suspend fun logout() {
        context.authDataStore.edit { prefs ->
            prefs.remove(SESSION_KEY)
        }
    }
    
    /**
     * Request password reset (demo only - no real email)
     */
    suspend fun requestPasswordReset(email: String): Boolean {
        val users = getUsers()
        return users.any { it.email.equals(email, ignoreCase = true) }
    }
    
    // Private helpers
    
    private suspend fun getUsers(): List<DemoUser> {
        return context.authDataStore.data.map { prefs ->
            prefs[USERS_KEY]?.let { json ->
                val type = object : TypeToken<List<DemoUser>>() {}.type
                gson.fromJson<List<DemoUser>>(json, type) ?: emptyList()
            } ?: emptyList()
        }.first()
    }
    
    private suspend fun saveUsers(users: List<DemoUser>) {
        context.authDataStore.edit { prefs ->
            prefs[USERS_KEY] = gson.toJson(users)
        }
    }
    
    private suspend fun saveSession(session: UserSession) {
        context.authDataStore.edit { prefs ->
            prefs[SESSION_KEY] = gson.toJson(session)
        }
    }
    
    /**
     * Simple MD5 hash for demo purposes only
     * NOT suitable for production use
     */
    private fun simpleHash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null
        
        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
