package com.aayush.emergency.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aayush.emergency.data.model.*
import com.aayush.emergency.data.repository.EmergencyRepository
import com.aayush.emergency.data.repository.ProfileRepository
import com.aayush.emergency.data.repository.SharedCaseRepository
import com.aayush.emergency.domain.triage.TriageEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing emergency sessions and patient brief generation
 */
class EmergencyViewModel(application: Application) : AndroidViewModel(application) {
    
    private val emergencyRepository = EmergencyRepository.getInstance(application)
    private val profileRepository = ProfileRepository.getInstance(application)
    private val sharedRepository = SharedCaseRepository.getInstance(application)
    
    val activeSession: StateFlow<EmergencySession?> = emergencyRepository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    private val _patientBrief = MutableStateFlow<PatientBrief?>(null)
    val patientBrief: StateFlow<PatientBrief?> = _patientBrief.asStateFlow()
    
    /**
     * Start a new emergency session
     */
    fun startEmergency() {
        viewModelScope.launch {
            // Get patient name from health profile if available
            val healthProfile = profileRepository.healthProfile.first()
            val patientName = healthProfile?.fullName ?: ""
            
            val session = emergencyRepository.startEmergencySession()
            // Update with patient name
            if (patientName.isNotEmpty()) {
                val updated = session.copy(patientName = patientName)
                emergencyRepository.saveActiveSession(updated)
            }
        }
    }
    
    /**
     * Add symptom to active session
     */
    fun addSymptom(description: String, severity: SymptomSeverity, duration: String, notes: String) {
        viewModelScope.launch {
            val session = activeSession.value ?: return@launch
            
            val symptom = SymptomInfo(
                id = System.currentTimeMillis(),
                description = description.trim(),
                severity = severity,
                duration = duration.trim(),
                notes = notes.trim(),
                reportedAt = System.currentTimeMillis()
            )
            
            val updatedSession = emergencyRepository.addSymptoms(session, listOf(symptom))
            
            // Automatically run triage after adding symptoms
            runTriage(updatedSession)
        }
    }
    
    /**
     * Run triage assessment on current symptoms
     */
    private fun runTriage(session: EmergencySession) {
        viewModelScope.launch {
            if (session.symptoms.isEmpty()) return@launch
            
            val triageResult = TriageEngine.assessSymptoms(session.symptoms)
            val triaged = emergencyRepository.saveTriageResult(session, triageResult)
            
            // Submit to shared repository for cross-device access
            submitToSharedRepository(triaged)
        }
    }
    
    /**
     * Submit emergency to shared repository for multi-device demo
     */
    private fun submitToSharedRepository(session: EmergencySession) {
        viewModelScope.launch {
            // Generate patient brief first
            val brief = generateBrief(session)
            
            // Submit to shared repository
            sharedRepository.submitCase(session, brief)
        }
    }
    
    /**
     * Generate patient brief combining emergency info and health profile
     * Respects consent preferences
     */
    fun generatePatientBrief() {
        viewModelScope.launch {
            val session = activeSession.value
            if (session == null) {
                _patientBrief.value = null
                return@launch
            }
            
            val brief = generateBrief(session)
            _patientBrief.value = brief
        }
    }
    
    private suspend fun generateBrief(session: EmergencySession): PatientBrief {
        // Collect profile data
        val healthProfile = profileRepository.healthProfile.first()
        val conditions = profileRepository.medicalConditions.first()
        val medications = profileRepository.medications.first()
        val allergies = profileRepository.allergies.first()
        val contacts = profileRepository.emergencyContacts.first()
        val consent = profileRepository.consentPreferences.first()
        
        val consentGiven = consent?.shareWithHospital == true
        
        // Generate brief
        return PatientBrief(
            generatedAt = System.currentTimeMillis(),
            emergencySession = session,
            healthProfile = if (consentGiven) healthProfile else null,
            relevantConditions = if (consentGiven) conditions else emptyList(),
            relevantMedications = if (consentGiven) medications else emptyList(),
            relevantAllergies = if (consentGiven) allergies else emptyList(),
            emergencyContacts = contacts,  // Contacts always included
            consentGiven = consentGiven
        )
    }
    
    /**
     * Complete the active emergency session
     */
    fun completeEmergency() {
        viewModelScope.launch {
            val session = activeSession.value ?: return@launch
            emergencyRepository.completeSession(session)
            emergencyRepository.clearActiveSession()
            _patientBrief.value = null
        }
    }
    
    /**
     * Cancel the active emergency session
     */
    fun cancelEmergency() {
        viewModelScope.launch {
            val session = activeSession.value ?: return@launch
            emergencyRepository.cancelSession(session)
            emergencyRepository.clearActiveSession()
            _patientBrief.value = null
        }
    }
}
