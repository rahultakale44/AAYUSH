package com.aayush.emergency.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aayush.emergency.data.model.*
import com.aayush.emergency.data.repository.ProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing senior health profile with DataStore persistence
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ProfileRepository.getInstance(application)
    
    // State flows for UI
    val healthProfile: StateFlow<HealthProfile?> = repository.healthProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val medicalConditions: StateFlow<List<MedicalCondition>> = repository.medicalConditions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val medications: StateFlow<List<Medication>> = repository.medications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val allergies: StateFlow<List<Allergy>> = repository.allergies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val surgeries: StateFlow<List<Surgery>> = repository.surgeries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val emergencyContacts: StateFlow<List<EmergencyContact>> = repository.emergencyContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val consentPreferences: StateFlow<ConsentPreferences?> = repository.consentPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Health Profile operations
    fun saveHealthProfile(profile: HealthProfile) {
        viewModelScope.launch {
            repository.saveHealthProfile(profile)
        }
    }
    
    // Medical Condition operations
    fun addMedicalCondition(conditionName: String, notes: String = "") {
        if (conditionName.isBlank()) return
        viewModelScope.launch {
            repository.addMedicalCondition(
                MedicalCondition(
                    conditionName = conditionName.trim(),
                    notes = notes.trim(),
                    isVerified = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun deleteMedicalCondition(condition: MedicalCondition) {
        viewModelScope.launch {
            repository.deleteMedicalCondition(condition)
        }
    }
    
    // Medication operations
    fun addMedication(name: String, dosage: String = "", frequency: String = "", notes: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addMedication(
                Medication(
                    medicationName = name.trim(),
                    dosage = dosage.trim(),
                    frequency = frequency.trim(),
                    notes = notes.trim(),
                    isVerified = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
        }
    }
    
    // Allergy operations
    fun addAllergy(name: String, reaction: String = "", severity: String = "", notes: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addAllergy(
                Allergy(
                    allergyName = name.trim(),
                    reaction = reaction.trim(),
                    severity = severity,
                    notes = notes.trim(),
                    isVerified = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun deleteAllergy(allergy: Allergy) {
        viewModelScope.launch {
            repository.deleteAllergy(allergy)
        }
    }
    
    // Surgery operations
    fun addSurgery(name: String, date: String = "", notes: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addSurgery(
                Surgery(
                    surgeryName = name.trim(),
                    date = date.trim(),
                    notes = notes.trim(),
                    isVerified = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun deleteSurgery(surgery: Surgery) {
        viewModelScope.launch {
            repository.deleteSurgery(surgery)
        }
    }
    
    // Emergency Contact operations
    fun addEmergencyContact(name: String, relationship: String, phoneNumber: String, notes: String = "") {
        if (name.isBlank() || relationship.isBlank() || phoneNumber.isBlank()) return
        viewModelScope.launch {
            repository.addEmergencyContact(
                EmergencyContact(
                    name = name.trim(),
                    relationship = relationship.trim(),
                    phoneNumber = phoneNumber.trim(),
                    notes = notes.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun deleteEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(contact)
        }
    }
    
    // Consent operations
    fun updateConsent(shareWithHospital: Boolean, shareWithCaregivers: Boolean) {
        viewModelScope.launch {
            repository.saveConsentPreferences(
                ConsentPreferences(
                    shareWithHospital = shareWithHospital,
                    shareWithCaregivers = shareWithCaregivers,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }
}
