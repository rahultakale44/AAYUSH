package com.aayush.emergency.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aayush.emergency.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing senior health profile data with DataStore persistence
 * Data persists across app restarts using JSON serialization
 */
class ProfileRepository(private val context: Context) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aayush_profile")
    private val gson = Gson()
    
    // Preference keys
    private val HEALTH_PROFILE_KEY = stringPreferencesKey("health_profile")
    private val MEDICAL_CONDITIONS_KEY = stringPreferencesKey("medical_conditions")
    private val MEDICATIONS_KEY = stringPreferencesKey("medications")
    private val ALLERGIES_KEY = stringPreferencesKey("allergies")
    private val SURGERIES_KEY = stringPreferencesKey("surgeries")
    private val EMERGENCY_CONTACTS_KEY = stringPreferencesKey("emergency_contacts")
    private val CONSENT_PREFERENCES_KEY = stringPreferencesKey("consent_preferences")
    
    private var nextId = 1L
    
    // Flows from DataStore
    val healthProfile: Flow<HealthProfile?> = context.dataStore.data.map { preferences ->
        preferences[HEALTH_PROFILE_KEY]?.let { json ->
            gson.fromJson(json, HealthProfile::class.java)
        }
    }
    
    val medicalConditions: Flow<List<MedicalCondition>> = context.dataStore.data.map { preferences ->
        preferences[MEDICAL_CONDITIONS_KEY]?.let { json ->
            val type = object : TypeToken<List<MedicalCondition>>() {}.type
            gson.fromJson<List<MedicalCondition>>(json, type) ?: emptyList()
        } ?: emptyList()
    }
    
    val medications: Flow<List<Medication>> = context.dataStore.data.map { preferences ->
        preferences[MEDICATIONS_KEY]?.let { json ->
            val type = object : TypeToken<List<Medication>>() {}.type
            gson.fromJson<List<Medication>>(json, type) ?: emptyList()
        } ?: emptyList()
    }
    
    val allergies: Flow<List<Allergy>> = context.dataStore.data.map { preferences ->
        preferences[ALLERGIES_KEY]?.let { json ->
            val type = object : TypeToken<List<Allergy>>() {}.type
            gson.fromJson<List<Allergy>>(json, type) ?: emptyList()
        } ?: emptyList()
    }
    
    val surgeries: Flow<List<Surgery>> = context.dataStore.data.map { preferences ->
        preferences[SURGERIES_KEY]?.let { json ->
            val type = object : TypeToken<List<Surgery>>() {}.type
            gson.fromJson<List<Surgery>>(json, type) ?: emptyList()
        } ?: emptyList()
    }
    
    val emergencyContacts: Flow<List<EmergencyContact>> = context.dataStore.data.map { preferences ->
        preferences[EMERGENCY_CONTACTS_KEY]?.let { json ->
            val type = object : TypeToken<List<EmergencyContact>>() {}.type
            gson.fromJson<List<EmergencyContact>>(json, type) ?: emptyList()
        } ?: emptyList()
    }
    
    val consentPreferences: Flow<ConsentPreferences?> = context.dataStore.data.map { preferences ->
        preferences[CONSENT_PREFERENCES_KEY]?.let { json ->
            gson.fromJson(json, ConsentPreferences::class.java)
        }
    }
    
    // Health Profile
    suspend fun saveHealthProfile(profile: HealthProfile) {
        context.dataStore.edit { preferences ->
            preferences[HEALTH_PROFILE_KEY] = gson.toJson(profile)
        }
    }
    
    // Medical Conditions
    suspend fun addMedicalCondition(condition: MedicalCondition): Long {
        val id = nextId++
        val newCondition = condition.copy(id = id)
        context.dataStore.edit { preferences ->
            val currentJson = preferences[MEDICAL_CONDITIONS_KEY]
            val currentList = if (currentJson != null) {
                val type = object : TypeToken<List<MedicalCondition>>() {}.type
                gson.fromJson<List<MedicalCondition>>(currentJson, type) ?: emptyList()
            } else emptyList()
            val updatedList = currentList + newCondition
            preferences[MEDICAL_CONDITIONS_KEY] = gson.toJson(updatedList)
        }
        return id
    }
    
    suspend fun updateMedicalCondition(condition: MedicalCondition) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[MEDICAL_CONDITIONS_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<MedicalCondition>>() {}.type
                val currentList = gson.fromJson<List<MedicalCondition>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.map { if (it.id == condition.id) condition else it }
                preferences[MEDICAL_CONDITIONS_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    suspend fun deleteMedicalCondition(condition: MedicalCondition) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[MEDICAL_CONDITIONS_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<MedicalCondition>>() {}.type
                val currentList = gson.fromJson<List<MedicalCondition>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.filter { it.id != condition.id }
                preferences[MEDICAL_CONDITIONS_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    // Medications
    suspend fun addMedication(medication: Medication): Long {
        val id = nextId++
        val newMedication = medication.copy(id = id)
        context.dataStore.edit { preferences ->
            val currentJson = preferences[MEDICATIONS_KEY]
            val currentList = if (currentJson != null) {
                val type = object : TypeToken<List<Medication>>() {}.type
                gson.fromJson<List<Medication>>(currentJson, type) ?: emptyList()
            } else emptyList()
            val updatedList = currentList + newMedication
            preferences[MEDICATIONS_KEY] = gson.toJson(updatedList)
        }
        return id
    }
    
    suspend fun updateMedication(medication: Medication) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[MEDICATIONS_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<Medication>>() {}.type
                val currentList = gson.fromJson<List<Medication>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.map { if (it.id == medication.id) medication else it }
                preferences[MEDICATIONS_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    suspend fun deleteMedication(medication: Medication) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[MEDICATIONS_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<Medication>>() {}.type
                val currentList = gson.fromJson<List<Medication>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.filter { it.id != medication.id }
                preferences[MEDICATIONS_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    // Allergies
    suspend fun addAllergy(allergy: Allergy): Long {
        val id = nextId++
        val newAllergy = allergy.copy(id = id)
        context.dataStore.edit { preferences ->
            val currentJson = preferences[ALLERGIES_KEY]
            val currentList = if (currentJson != null) {
                val type = object : TypeToken<List<Allergy>>() {}.type
                gson.fromJson<List<Allergy>>(currentJson, type) ?: emptyList()
            } else emptyList()
            val updatedList = currentList + newAllergy
            preferences[ALLERGIES_KEY] = gson.toJson(updatedList)
        }
        return id
    }
    
    suspend fun updateAllergy(allergy: Allergy) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[ALLERGIES_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<Allergy>>() {}.type
                val currentList = gson.fromJson<List<Allergy>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.map { if (it.id == allergy.id) allergy else it }
                preferences[ALLERGIES_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    suspend fun deleteAllergy(allergy: Allergy) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[ALLERGIES_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<Allergy>>() {}.type
                val currentList = gson.fromJson<List<Allergy>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.filter { it.id != allergy.id }
                preferences[ALLERGIES_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    // Surgeries
    suspend fun addSurgery(surgery: Surgery): Long {
        val id = nextId++
        val newSurgery = surgery.copy(id = id)
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SURGERIES_KEY]
            val currentList = if (currentJson != null) {
                val type = object : TypeToken<List<Surgery>>() {}.type
                gson.fromJson<List<Surgery>>(currentJson, type) ?: emptyList()
            } else emptyList()
            val updatedList = currentList + newSurgery
            preferences[SURGERIES_KEY] = gson.toJson(updatedList)
        }
        return id
    }
    
    suspend fun updateSurgery(surgery: Surgery) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SURGERIES_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<Surgery>>() {}.type
                val currentList = gson.fromJson<List<Surgery>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.map { if (it.id == surgery.id) surgery else it }
                preferences[SURGERIES_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    suspend fun deleteSurgery(surgery: Surgery) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SURGERIES_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<Surgery>>() {}.type
                val currentList = gson.fromJson<List<Surgery>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.filter { it.id != surgery.id }
                preferences[SURGERIES_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    // Emergency Contacts
    suspend fun addEmergencyContact(contact: EmergencyContact): Long {
        val id = nextId++
        val newContact = contact.copy(id = id)
        context.dataStore.edit { preferences ->
            val currentJson = preferences[EMERGENCY_CONTACTS_KEY]
            val currentList = if (currentJson != null) {
                val type = object : TypeToken<List<EmergencyContact>>() {}.type
                gson.fromJson<List<EmergencyContact>>(currentJson, type) ?: emptyList()
            } else emptyList()
            val updatedList = currentList + newContact
            preferences[EMERGENCY_CONTACTS_KEY] = gson.toJson(updatedList)
        }
        return id
    }
    
    suspend fun updateEmergencyContact(contact: EmergencyContact) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[EMERGENCY_CONTACTS_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<EmergencyContact>>() {}.type
                val currentList = gson.fromJson<List<EmergencyContact>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.map { if (it.id == contact.id) contact else it }
                preferences[EMERGENCY_CONTACTS_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    suspend fun deleteEmergencyContact(contact: EmergencyContact) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[EMERGENCY_CONTACTS_KEY]
            if (currentJson != null) {
                val type = object : TypeToken<List<EmergencyContact>>() {}.type
                val currentList = gson.fromJson<List<EmergencyContact>>(currentJson, type) ?: emptyList()
                val updatedList = currentList.filter { it.id != contact.id }
                preferences[EMERGENCY_CONTACTS_KEY] = gson.toJson(updatedList)
            }
        }
    }
    
    // Consent Preferences
    suspend fun saveConsentPreferences(preferences: ConsentPreferences) {
        context.dataStore.edit { prefs ->
            prefs[CONSENT_PREFERENCES_KEY] = gson.toJson(preferences)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ProfileRepository? = null
        
        fun getInstance(context: Context): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ProfileRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
