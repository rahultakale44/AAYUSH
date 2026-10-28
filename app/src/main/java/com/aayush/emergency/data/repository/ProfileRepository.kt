package com.aayush.emergency.data.repository

import com.aayush.emergency.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory repository for managing senior health profile data
 * Data persists during app session but resets on restart
 * Future: Can be enhanced with SharedPreferences or DataStore for persistence
 */
class ProfileRepository {
    private val _healthProfile = MutableStateFlow<HealthProfile?>(null)
    val healthProfile: Flow<HealthProfile?> = _healthProfile.asStateFlow()
    
    private val _medicalConditions = MutableStateFlow<List<MedicalCondition>>(emptyList())
    val medicalConditions: Flow<List<MedicalCondition>> = _medicalConditions.asStateFlow()
    
    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: Flow<List<Medication>> = _medications.asStateFlow()
    
    private val _allergies = MutableStateFlow<List<Allergy>>(emptyList())
    val allergies: Flow<List<Allergy>> = _allergies.asStateFlow()
    
    private val _surgeries = MutableStateFlow<List<Surgery>>(emptyList())
    val surgeries: Flow<List<Surgery>> = _surgeries.asStateFlow()
    
    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: Flow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()
    
    private val _consentPreferences = MutableStateFlow<ConsentPreferences?>(null)
    val consentPreferences: Flow<ConsentPreferences?> = _consentPreferences.asStateFlow()
    
    private var nextId = 1L
    
    // Health Profile
    suspend fun saveHealthProfile(profile: HealthProfile) {
        _healthProfile.value = profile
    }
    
    // Medical Conditions
    suspend fun addMedicalCondition(condition: MedicalCondition): Long {
        val id = nextId++
        val newCondition = condition.copy(id = id)
        _medicalConditions.value = _medicalConditions.value + newCondition
        return id
    }
    
    suspend fun updateMedicalCondition(condition: MedicalCondition) {
        _medicalConditions.value = _medicalConditions.value.map {
            if (it.id == condition.id) condition else it
        }
    }
    
    suspend fun deleteMedicalCondition(condition: MedicalCondition) {
        _medicalConditions.value = _medicalConditions.value.filter { it.id != condition.id }
    }
    
    // Medications
    suspend fun addMedication(medication: Medication): Long {
        val id = nextId++
        val newMedication = medication.copy(id = id)
        _medications.value = _medications.value + newMedication
        return id
    }
    
    suspend fun updateMedication(medication: Medication) {
        _medications.value = _medications.value.map {
            if (it.id == medication.id) medication else it
        }
    }
    
    suspend fun deleteMedication(medication: Medication) {
        _medications.value = _medications.value.filter { it.id != medication.id }
    }
    
    // Allergies
    suspend fun addAllergy(allergy: Allergy): Long {
        val id = nextId++
        val newAllergy = allergy.copy(id = id)
        _allergies.value = _allergies.value + newAllergy
        return id
    }
    
    suspend fun updateAllergy(allergy: Allergy) {
        _allergies.value = _allergies.value.map {
            if (it.id == allergy.id) allergy else it
        }
    }
    
    suspend fun deleteAllergy(allergy: Allergy) {
        _allergies.value = _allergies.value.filter { it.id != allergy.id }
    }
    
    // Surgeries
    suspend fun addSurgery(surgery: Surgery): Long {
        val id = nextId++
        val newSurgery = surgery.copy(id = id)
        _surgeries.value = _surgeries.value + newSurgery
        return id
    }
    
    suspend fun updateSurgery(surgery: Surgery) {
        _surgeries.value = _surgeries.value.map {
            if (it.id == surgery.id) surgery else it
        }
    }
    
    suspend fun deleteSurgery(surgery: Surgery) {
        _surgeries.value = _surgeries.value.filter { it.id != surgery.id }
    }
    
    // Emergency Contacts
    suspend fun addEmergencyContact(contact: EmergencyContact): Long {
        val id = nextId++
        val newContact = contact.copy(id = id)
        _emergencyContacts.value = _emergencyContacts.value + newContact
        return id
    }
    
    suspend fun updateEmergencyContact(contact: EmergencyContact) {
        _emergencyContacts.value = _emergencyContacts.value.map {
            if (it.id == contact.id) contact else it
        }
    }
    
    suspend fun deleteEmergencyContact(contact: EmergencyContact) {
        _emergencyContacts.value = _emergencyContacts.value.filter { it.id != contact.id }
    }
    
    // Consent Preferences
    suspend fun saveConsentPreferences(preferences: ConsentPreferences) {
        _consentPreferences.value = preferences
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ProfileRepository? = null
        
        fun getInstance(): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ProfileRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}
