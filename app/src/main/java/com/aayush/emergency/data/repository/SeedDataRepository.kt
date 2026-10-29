package com.aayush.emergency.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aayush.emergency.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for seeding fictional Indian family data
 * 
 * IMPORTANT: All data is fictional for demo purposes
 * - Names: Fictional Indian personas
 * - Phone numbers: Clearly marked as demo/fictional
 * - Medical data: Sample data for demonstration
 */
class SeedDataRepository(private val context: Context) {
    
    private val Context.seedDataStore: DataStore<Preferences> by preferencesDataStore(name = "aayush_seed")
    private val SEED_INITIALIZED_KEY = stringPreferencesKey("profiles_seed_initialized")
    
    /**
     * Initialize seed data for demo senior citizen account
     * Called once when demo account first logs in
     */
    suspend fun initializeSeniorSeedData(userId: String): Boolean {
        val profileRepo = ProfileRepository.getInstance(context)
        
        // Check if already initialized for this user
        val seedInitialized = context.seedDataStore.data.map { prefs ->
            prefs[SEED_INITIALIZED_KEY] == userId
        }.first()
        
        if (!seedInitialized) {
            // Health Profile - Savita Deshmukh
            profileRepo.saveHealthProfile(
                HealthProfile(
                    id = 1L,
                    fullName = "Savita Deshmukh",
                    dateOfBirth = "15/08/1958",
                    notes = "Age 68, Retired teacher from Pune"
                )
            )
            
            // Medical Conditions
            profileRepo.addMedicalCondition(
                MedicalCondition(
                    id = 0L,
                    conditionName = "Type 2 Diabetes",
                    notes = "Well controlled with medication, diagnosed 2015"
                )
            )
            
            profileRepo.addMedicalCondition(
                MedicalCondition(
                    id = 0L,
                    conditionName = "Hypertension",
                    notes = "Monitoring required, diagnosed 2018"
                )
            )
            
            // Medications
            profileRepo.addMedication(
                Medication(
                    id = 0L,
                    medicationName = "Metformin",
                    dosage = "500mg",
                    frequency = "Twice daily with meals",
                    notes = "For diabetes management"
                )
            )
            
            profileRepo.addMedication(
                Medication(
                    id = 0L,
                    medicationName = "Amlodipine",
                    dosage = "5mg",
                    frequency = "Once daily, morning",
                    notes = "For blood pressure"
                )
            )
            
            // Allergies
            profileRepo.addAllergy(
                Allergy(
                    id = 0L,
                    allergyName = "Penicillin",
                    reaction = "Skin rash and itching",
                    severity = "Moderate",
                    notes = "Discovered during antibiotic treatment in 2010"
                )
            )
            
            // Surgeries
            profileRepo.addSurgery(
                Surgery(
                    id = 0L,
                    surgeryName = "Cataract Surgery",
                    date = "March 2020",
                    notes = "Both eyes at Pune Eye Hospital, successful recovery"
                )
            )
            
            // Emergency Contacts - Fictional Indian Family
            profileRepo.addEmergencyContact(
                EmergencyContact(
                    id = 0L,
                    name = "Amit Deshmukh",
                    relationship = "Son",
                    phoneNumber = "+91-98765-DEMO1",  // Clearly fictional
                    notes = "Lives nearby, primary contact"
                )
            )
            
            profileRepo.addEmergencyContact(
                EmergencyContact(
                    id = 0L,
                    name = "Neha Deshmukh",
                    relationship = "Daughter-in-law",
                    phoneNumber = "+91-97654-DEMO2",  // Clearly fictional
                    notes = "Secondary contact, works from home"
                )
            )
            
            profileRepo.addEmergencyContact(
                EmergencyContact(
                    id = 0L,
                    name = "Dr. Priya Sharma",
                    relationship = "Family Doctor",
                    phoneNumber = "+91-96543-DEMO3",  // Clearly fictional
                    notes = "General physician, Pune"
                )
            )
            
            // Consent Preferences
            profileRepo.saveConsentPreferences(
                ConsentPreferences(
                    shareWithHospital = true,
                    shareWithCaregivers = true,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            
            // Mark as initialized
            context.seedDataStore.edit { prefs ->
                prefs[SEED_INITIALIZED_KEY] = userId
            }
            
            return true
        }
        
        return false  // Already initialized
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SeedDataRepository? = null
        
        fun getInstance(context: Context): SeedDataRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SeedDataRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
