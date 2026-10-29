package com.aayush.emergency.data.api

import android.util.Log
import com.aayush.emergency.data.model.*
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*

/**
 * API client for three-device demo coordination
 * 
 * DEMO CONFIGURATION:
 * - Set serverUrl to laptop's local IP (e.g., "http://192.168.1.100:8080")
 * - Run demo-server.jar on laptop before demo
 * - All three devices must be on same WiFi network
 * 
 * FALLBACK MODE:
 * - If server unavailable, uses demo data stored locally
 * - Cases won't sync between devices in fallback mode
 */
class DemoApiClient(
    private val serverUrl: String = "http://10.0.2.2:8080" // Default for emulator
) {
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            gson()
        }
        
        engine {
            connectTimeout = 5_000
            socketTimeout = 5_000
        }
    }
    
    private val gson = Gson()
    private val TAG = "DemoApiClient"
    
    /**
     * Submit emergency case to shared server
     */
    suspend fun submitCase(case: EmergencyCase): Result<EmergencyCase> {
        return try {
            val response: HttpResponse = client.post("$serverUrl/api/cases") {
                contentType(ContentType.Application.Json)
                setBody(case)
            }
            
            if (response.status.isSuccess()) {
                val submittedCase: EmergencyCase = response.body()
                Log.d(TAG, "Case submitted successfully: ${submittedCase.id}")
                Result.success(submittedCase)
            } else {
                Log.e(TAG, "Failed to submit case: ${response.status}")
                Result.failure(Exception("Server error: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting case", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all cases from shared server
     */
    suspend fun getCases(): Result<List<EmergencyCase>> {
        return try {
            val response: HttpResponse = client.get("$serverUrl/api/cases")
            
            if (response.status.isSuccess()) {
                val cases: List<EmergencyCase> = response.body()
                Log.d(TAG, "Retrieved ${cases.size} cases")
                Result.success(cases)
            } else {
                Log.e(TAG, "Failed to get cases: ${response.status}")
                Result.failure(Exception("Server error: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cases", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get specific case by ID
     */
    suspend fun getCase(caseId: Long): Result<EmergencyCase> {
        return try {
            val response: HttpResponse = client.get("$serverUrl/api/cases/$caseId")
            
            if (response.status.isSuccess()) {
                val case: EmergencyCase = response.body()
                Result.success(case)
            } else {
                Result.failure(Exception("Server error: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting case $caseId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update case (transport status, hospital status, staff assignments)
     */
    suspend fun updateCase(case: EmergencyCase): Result<EmergencyCase> {
        return try {
            val response: HttpResponse = client.put("$serverUrl/api/cases/${case.id}") {
                contentType(ContentType.Application.Json)
                setBody(case)
            }
            
            if (response.status.isSuccess()) {
                val updated: EmergencyCase = response.body()
                Log.d(TAG, "Case updated successfully: ${updated.id}")
                Result.success(updated)
            } else {
                Result.failure(Exception("Server error: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating case", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update transport status
     */
    suspend fun updateTransportStatus(
        caseId: Long,
        status: TransportStatus
    ): Result<EmergencyCase> {
        return try {
            val response: HttpResponse = client.patch("$serverUrl/api/cases/$caseId/transport") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("status" to status.name))
            }
            
            if (response.status.isSuccess()) {
                val updated: EmergencyCase = response.body()
                Result.success(updated)
            } else {
                Result.failure(Exception("Server error: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating transport status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Assign staff to case
     */
    suspend fun assignStaff(
        caseId: Long,
        staffAssignment: StaffAssignment
    ): Result<EmergencyCase> {
        return try {
            val response: HttpResponse = client.post("$serverUrl/api/cases/$caseId/staff") {
                contentType(ContentType.Application.Json)
                setBody(staffAssignment)
            }
            
            if (response.status.isSuccess()) {
                val updated: EmergencyCase = response.body()
                Result.success(updated)
            } else {
                Result.failure(Exception("Server error: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning staff", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if server is available
     */
    suspend fun checkConnection(): Boolean {
        return try {
            val response: HttpResponse = client.get("$serverUrl/api/health")
            response.status.isSuccess()
        } catch (e: Exception) {
            Log.w(TAG, "Server unavailable: ${e.message}")
            false
        }
    }
    
    fun close() {
        client.close()
    }
}
