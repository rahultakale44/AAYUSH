package com.aayush.emergency.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aayush.emergency.data.api.DemoApiClient
import com.aayush.emergency.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Shared case repository for three-device demo
 * 
 * MODES:
 * - NETWORK: Uses shared API server for cross-device sync
 * - FALLBACK: Uses local demo data when server unavailable
 * 
 * The repository automatically detects server availability and falls back gracefully
 */
class SharedCaseRepository(
    private val context: Context,
    serverUrl: String = "http://10.0.2.2:8080"
) {
    
    private val apiClient = DemoApiClient(serverUrl)
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aayush_shared_cases")
    private val gson = Gson()
    
    private val DEMO_CASES_KEY = stringPreferencesKey("demo_cases")
    private val DEMO_STAFF_KEY = stringPreferencesKey("demo_staff")
    
    private val _serverAvailable = MutableStateFlow(false)
    val serverAvailable = _serverAvailable.asStateFlow()
    
    private val TAG = "SharedCaseRepository"
    
    /**
     * Check server connectivity
     */
    suspend fun checkServerConnection(): Boolean {
        val available = apiClient.checkConnection()
        _serverAvailable.value = available
        return available
    }
    
    /**
     * Submit new emergency case
     */
    suspend fun submitCase(
        session: EmergencySession,
        patientBrief: PatientBrief?
    ): Result<EmergencyCase> {
        val case = EmergencyCase(
            id = session.id,
            patientName = session.patientName.ifEmpty { "Patient #${session.id}" },
            session = session,
            patientBrief = patientBrief,
            transportStatus = TransportStatus.NONE,
            hospitalStatus = HospitalCaseStatus.NEW
        )
        
        // Try server first
        if (_serverAvailable.value) {
            val result = apiClient.submitCase(case)
            if (result.isSuccess) {
                Log.d(TAG, "Case submitted to server")
                return result
            }
            Log.w(TAG, "Server submission failed, saving locally")
        }
        
        // Fallback: save locally
        saveCaseLocally(case)
        return Result.success(case)
    }
    
    /**
     * Get all cases
     */
    suspend fun getCases(): Result<List<EmergencyCase>> {
        // Try server first
        if (_serverAvailable.value) {
            val result = apiClient.getCases()
            if (result.isSuccess) {
                return result
            }
        }
        
        // Fallback: load demo cases
        return Result.success(getDemoCases())
    }
    
    /**
     * Get specific case
     */
    suspend fun getCase(caseId: Long): Result<EmergencyCase> {
        // Try server first
        if (_serverAvailable.value) {
            val result = apiClient.getCase(caseId)
            if (result.isSuccess) {
                return result
            }
        }
        
        // Fallback: find in local demo cases
        val localCase = getDemoCases().find { it.id == caseId }
        return if (localCase != null) {
            Result.success(localCase)
        } else {
            Result.failure(Exception("Case not found"))
        }
    }
    
    /**
     * Update transport status
     */
    suspend fun updateTransportStatus(
        caseId: Long,
        status: TransportStatus
    ): Result<EmergencyCase> {
        // Try server first
        if (_serverAvailable.value) {
            val result = apiClient.updateTransportStatus(caseId, status)
            if (result.isSuccess) {
                return result
            }
        }
        
        // Fallback: update locally
        val cases = getDemoCases().toMutableList()
        val index = cases.indexOfFirst { it.id == caseId }
        if (index >= 0) {
            val updated = cases[index].copy(
                transportStatus = status,
                lastUpdated = System.currentTimeMillis()
            )
            cases[index] = updated
            saveDemoCases(cases)
            return Result.success(updated)
        }
        
        return Result.failure(Exception("Case not found"))
    }
    
    /**
     * Update hospital case status
     */
    suspend fun updateHospitalStatus(
        caseId: Long,
        status: HospitalCaseStatus
    ): Result<EmergencyCase> {
        if (_serverAvailable.value) {
            val caseResult = apiClient.getCase(caseId)
            if (caseResult.isSuccess) {
                val updatedCase = caseResult.getOrNull()!!.copy(
                    hospitalStatus = status,
                    lastUpdated = System.currentTimeMillis()
                )
                return apiClient.updateCase(updatedCase)
            }
        }
        
        // Fallback
        val cases = getDemoCases().toMutableList()
        val index = cases.indexOfFirst { it.id == caseId }
        if (index >= 0) {
            val updated = cases[index].copy(
                hospitalStatus = status,
                lastUpdated = System.currentTimeMillis()
            )
            cases[index] = updated
            saveDemoCases(cases)
            return Result.success(updated)
        }
        
        return Result.failure(Exception("Case not found"))
    }
    
    /**
     * Assign staff to case
     */
    suspend fun assignStaff(
        caseId: Long,
        staffAssignment: StaffAssignment
    ): Result<EmergencyCase> {
        if (_serverAvailable.value) {
            val result = apiClient.assignStaff(caseId, staffAssignment)
            if (result.isSuccess) {
                return result
            }
        }
        
        // Fallback
        val cases = getDemoCases().toMutableList()
        val index = cases.indexOfFirst { it.id == caseId }
        if (index >= 0) {
            val currentCase = cases[index]
            
            // Check for duplicate assignment
            if (currentCase.assignedStaff.any { it.staffId == staffAssignment.staffId }) {
                return Result.failure(Exception("Staff already assigned to this case"))
            }
            
            val updated = currentCase.copy(
                assignedStaff = currentCase.assignedStaff + staffAssignment,
                hospitalStatus = if (currentCase.hospitalStatus == HospitalCaseStatus.NEW) {
                    HospitalCaseStatus.STAFF_ASSIGNED
                } else {
                    currentCase.hospitalStatus
                },
                lastUpdated = System.currentTimeMillis()
            )
            cases[index] = updated
            saveDemoCases(cases)
            return Result.success(updated)
        }
        
        return Result.failure(Exception("Case not found"))
    }
    
    /**
     * Remove staff assignment
     */
    suspend fun removeStaffAssignment(
        caseId: Long,
        staffId: String
    ): Result<EmergencyCase> {
        val cases = getDemoCases().toMutableList()
        val index = cases.indexOfFirst { it.id == caseId }
        if (index >= 0) {
            val updated = cases[index].copy(
                assignedStaff = cases[index].assignedStaff.filter { it.staffId != staffId },
                lastUpdated = System.currentTimeMillis()
            )
            cases[index] = updated
            saveDemoCases(cases)
            return Result.success(updated)
        }
        
        return Result.failure(Exception("Case not found"))
    }
    
    /**
     * Get available hospital staff (demo data)
     */
    suspend fun getHospitalStaff(): List<HospitalStaff> {
        var result: List<HospitalStaff>? = null
        context.dataStore.data.map { prefs ->
            prefs[DEMO_STAFF_KEY]
        }.collect { json ->
            result = if (json != null) {
                val type = object : TypeToken<List<HospitalStaff>>() {}.type
                gson.fromJson<List<HospitalStaff>>(json, type) ?: getDefaultStaff()
            } else {
                getDefaultStaff()
            }
        }
        
        return result ?: getDefaultStaff()
    }
    
    /**
     * Get default demo staff
     */
    private fun getDefaultStaff(): List<HospitalStaff> {
        return listOf(
            HospitalStaff(
                id = "staff001",
                displayName = "Dr. Sarah Patel",
                service = StaffService.CARDIAC_RESPONSE,
                availability = StaffAvailability.AVAILABLE
            ),
            HospitalStaff(
                id = "staff002",
                displayName = "Nurse Rajesh Kumar",
                service = StaffService.EMERGENCY_NURSE,
                availability = StaffAvailability.AVAILABLE
            ),
            HospitalStaff(
                id = "staff003",
                displayName = "Dr. Priya Sharma",
                service = StaffService.RESPIRATORY_SPECIALIST,
                availability = StaffAvailability.AVAILABLE
            ),
            HospitalStaff(
                id = "staff004",
                displayName = "Nurse Amit Singh",
                service = StaffService.TRIAGE_NURSE,
                availability = StaffAvailability.AVAILABLE
            ),
            HospitalStaff(
                id = "staff005",
                displayName = "Dr. Meera Reddy",
                service = StaffService.GENERAL_MEDICAL,
                availability = StaffAvailability.AVAILABLE
            ),
            HospitalStaff(
                id = "staff006",
                displayName = "Coordinator Anil Desai",
                service = StaffService.PATIENT_COORDINATION,
                availability = StaffAvailability.AVAILABLE
            ),
            HospitalStaff(
                id = "staff007",
                displayName = "Dr. Vikram Joshi",
                service = StaffService.TRAUMA_TEAM,
                availability = StaffAvailability.AVAILABLE
            )
        )
    }
    
    private suspend fun saveCaseLocally(case: EmergencyCase) {
        val cases = getDemoCases().toMutableList()
        cases.add(case)
        saveDemoCases(cases)
    }
    
    private suspend fun getDemoCases(): List<EmergencyCase> {
        var result: List<EmergencyCase>? = null
        context.dataStore.data.map { prefs ->
            prefs[DEMO_CASES_KEY]
        }.collect { json ->
            result = if (json != null) {
                val type = object : TypeToken<List<EmergencyCase>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        }
        return result ?: emptyList()
    }
    
    private suspend fun saveDemoCases(cases: List<EmergencyCase>) {
        context.dataStore.edit { prefs ->
            prefs[DEMO_CASES_KEY] = gson.toJson(cases)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SharedCaseRepository? = null
        
        fun getInstance(context: Context, serverUrl: String = "http://10.0.2.2:8080"): SharedCaseRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SharedCaseRepository(context.applicationContext, serverUrl)
                INSTANCE = instance
                instance
            }
        }
    }
}
