package com.aayush.emergency.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aayush.emergency.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing emergency sessions with DataStore persistence
 * Tracks active and historical emergency sessions
 */
class EmergencyRepository(private val context: Context) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aayush_emergency")
    private val gson = Gson()
    
    private val ACTIVE_SESSION_KEY = stringPreferencesKey("active_emergency_session")
    private val SESSION_HISTORY_KEY = stringPreferencesKey("emergency_session_history")
    
    private var nextSessionId = 1L
    
    /**
     * Flow of the currently active emergency session
     */
    val activeSession: Flow<EmergencySession?> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_SESSION_KEY]?.let { json ->
            gson.fromJson(json, EmergencySession::class.java)
        }
    }
    
    /**
     * Start a new emergency session
     */
    suspend fun startEmergencySession(): EmergencySession {
        val session = EmergencySession(
            id = nextSessionId++,
            startTime = System.currentTimeMillis(),
            status = EmergencyStatus.STARTED
        )
        saveActiveSession(session)
        return session
    }
    
    /**
     * Add symptoms to active session
     */
    suspend fun addSymptoms(session: EmergencySession, symptoms: List<SymptomInfo>): EmergencySession {
        val updated = session.copy(
            symptoms = session.symptoms + symptoms,
            status = EmergencyStatus.SYMPTOMS_CAPTURED
        )
        saveActiveSession(updated)
        return updated
    }
    
    /**
     * Save triage result to active session
     */
    suspend fun saveTriageResult(session: EmergencySession, triageResult: TriageResult): EmergencySession {
        val updated = session.copy(
            triageResult = triageResult,
            status = EmergencyStatus.TRIAGED
        )
        saveActiveSession(updated)
        return updated
    }
    
    /**
     * Complete the active emergency session
     */
    suspend fun completeSession(session: EmergencySession): EmergencySession {
        val completed = session.copy(
            status = EmergencyStatus.COMPLETED,
            endTime = System.currentTimeMillis()
        )
        saveActiveSession(completed)
        addToHistory(completed)
        return completed
    }
    
    /**
     * Cancel the active emergency session
     */
    suspend fun cancelSession(session: EmergencySession): EmergencySession {
        val cancelled = session.copy(
            status = EmergencyStatus.CANCELLED,
            endTime = System.currentTimeMillis()
        )
        saveActiveSession(cancelled)
        addToHistory(cancelled)
        return cancelled
    }
    
    /**
     * Clear active session (after completion or cancellation)
     */
    suspend fun clearActiveSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACTIVE_SESSION_KEY)
        }
    }
    
    private suspend fun saveActiveSession(session: EmergencySession) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_SESSION_KEY] = gson.toJson(session)
        }
    }
    
    private suspend fun addToHistory(session: EmergencySession) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SESSION_HISTORY_KEY]
            val history = if (currentJson != null) {
                gson.fromJson(currentJson, Array<EmergencySession>::class.java).toMutableList()
            } else {
                mutableListOf()
            }
            history.add(0, session)  // Add to front
            // Keep last 10 sessions
            val trimmed = history.take(10)
            preferences[SESSION_HISTORY_KEY] = gson.toJson(trimmed)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: EmergencyRepository? = null
        
        fun getInstance(context: Context): EmergencyRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = EmergencyRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
