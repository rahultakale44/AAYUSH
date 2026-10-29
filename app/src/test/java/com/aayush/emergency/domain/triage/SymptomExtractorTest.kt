package com.aayush.emergency.domain.triage

import com.aayush.emergency.data.model.SymptomSeverity
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for SymptomExtractor
 * 
 * Verifies:
 * - English symptom extraction
 * - Hindi symptom extraction (transliterated and Devanagari)
 * - Marathi symptom extraction
 * - Negation handling
 * - Multiple symptoms in one transcript
 * - Unknown/uncertain symptoms
 */
class SymptomExtractorTest {
    
    @Test
    fun `extract chest pain from English`() {
        val transcript = "I have chest pain"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        assertEquals(SymptomType.CHEST_PAIN, symptoms[0].symptomType)
        assertEquals(SymptomSeverity.SEVERE, symptoms[0].severity)
        assertTrue(symptoms[0].confidence > 0.8)
    }
    
    @Test
    fun `extract breathing difficulty from English`() {
        val transcript = "I can't breathe properly"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val breathingSymptom = symptoms.find { it.symptomType == SymptomType.BREATHING_DIFFICULTY }
        assertNotNull(breathingSymptom)
        assertEquals(SymptomSeverity.SEVERE, breathingSymptom!!.severity)
    }
    
    @Test
    fun `extract chest pain from Hindi transliterated`() {
        val transcript = "Mujhe seene mein dard ho raha hai"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val chestPain = symptoms.find { it.symptomType == SymptomType.CHEST_PAIN }
        assertNotNull(chestPain)
        assertEquals("Hindi", chestPain!!.language)
    }
    
    @Test
    fun `extract breathing difficulty from Hindi transliterated`() {
        val transcript = "Mujhe saans lene mein takleef ho rahi hai"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val breathingIssue = symptoms.find { it.symptomType == SymptomType.BREATHING_DIFFICULTY }
        assertNotNull(breathingIssue)
        assertEquals(SymptomSeverity.SEVERE, breathingIssue!!.severity)
    }
    
    @Test
    fun `extract breathing difficulty from Hindi Devanagari`() {
        val transcript = "मुझे साँस लेने में तकलीफ हो रही है"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val breathingIssue = symptoms.find { it.symptomType == SymptomType.BREATHING_DIFFICULTY }
        assertNotNull(breathingIssue)
        assertEquals("Hindi", breathingIssue!!.language)
    }
    
    @Test
    fun `extract chest pain from Marathi`() {
        val transcript = "मला छातीत दुखत आहे"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val chestPain = symptoms.find { it.symptomType == SymptomType.CHEST_PAIN }
        assertNotNull(chestPain)
        assertEquals("Marathi", chestPain!!.language)
    }
    
    @Test
    fun `extract breathing difficulty from Marathi`() {
        val transcript = "मला श्वास घ्यायला त्रास होत आहे"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val breathingIssue = symptoms.find { it.symptomType == SymptomType.BREATHING_DIFFICULTY }
        assertNotNull(breathingIssue)
        assertEquals("Marathi", breathingIssue!!.language)
    }
    
    @Test
    fun `extract loss of consciousness`() {
        val transcript = "He fainted and became unconscious"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val consciousness = symptoms.find { it.symptomType == SymptomType.LOSS_OF_CONSCIOUSNESS }
        assertNotNull(consciousness)
        assertEquals(SymptomSeverity.SEVERE, consciousness!!.severity)
    }
    
    @Test
    fun `extract severe bleeding`() {
        val transcript = "There is severe bleeding and blood won't stop"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val bleeding = symptoms.find { it.symptomType == SymptomType.SEVERE_BLEEDING }
        assertNotNull(bleeding)
        assertEquals(SymptomSeverity.SEVERE, bleeding!!.severity)
    }
    
    @Test
    fun `extract stroke symptoms`() {
        val transcript = "face drooping and arm weakness"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue("Should extract at least one symptom", symptoms.isNotEmpty())
        val stroke = symptoms.find { it.symptomType == SymptomType.STROKE_SYMPTOMS }
        assertNotNull("Should find stroke symptoms", stroke)
        if (stroke != null) {
            assertEquals(SymptomSeverity.SEVERE, stroke.severity)
        }
    }
    
    @Test
    fun `extract head trauma`() {
        val transcript = "I hit my head badly"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val headTrauma = symptoms.find { it.symptomType == SymptomType.HEAD_TRAUMA }
        assertNotNull(headTrauma)
        assertEquals(SymptomSeverity.SEVERE, headTrauma!!.severity)
    }
    
    @Test
    fun `extract severe allergy`() {
        val transcript = "Having a severe allergic reaction and throat is swelling"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val allergy = symptoms.find { it.symptomType == SymptomType.SEVERE_ALLERGIC_REACTION }
        assertNotNull(allergy)
        assertEquals(SymptomSeverity.SEVERE, allergy!!.severity)
    }
    
    @Test
    fun `extract seizure`() {
        val transcript = "Patient is having a seizure"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val seizure = symptoms.find { it.symptomType == SymptomType.SEIZURE }
        assertNotNull(seizure)
        assertEquals(SymptomSeverity.SEVERE, seizure!!.severity)
    }
    
    @Test
    fun `handle negation - do not have chest pain`() {
        val transcript = "I do not have chest pain"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        // Should detect strong negation and mark as uncertain
        assertTrue(symptoms.isNotEmpty())
        assertEquals(SymptomType.UNCERTAIN, symptoms[0].symptomType)
        assertTrue(symptoms[0].confidence < 0.5)
    }
    
    @Test
    fun `handle negation - no breathing difficulty`() {
        val transcript = "I have no breathing difficulty"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        // Should detect strong negation
        assertTrue(symptoms.isNotEmpty())
        assertEquals(SymptomType.UNCERTAIN, symptoms[0].symptomType)
    }
    
    @Test
    fun `extract multiple symptoms from one transcript`() {
        val transcript = "I have chest pain and difficulty breathing"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.size >= 2)
        val types = symptoms.map { it.symptomType }
        assertTrue(types.contains(SymptomType.CHEST_PAIN))
        assertTrue(types.contains(SymptomType.BREATHING_DIFFICULTY))
    }
    
    @Test
    fun `handle general symptoms when no specific pattern matches`() {
        val transcript = "I feel very dizzy and tired"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        assertEquals(SymptomType.GENERAL, symptoms[0].symptomType)
        assertEquals(transcript, symptoms[0].description)
    }
    
    @Test
    fun `handle empty transcript`() {
        val transcript = ""
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isEmpty())
    }
    
    @Test
    fun `handle whitespace-only transcript`() {
        val transcript = "   "
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isEmpty())
    }
    
    @Test
    fun `convert extracted symptom to SymptomInfo`() {
        val extracted = ExtractedSymptom(
            symptomType = SymptomType.CHEST_PAIN,
            description = "Chest pain or discomfort",
            severity = SymptomSeverity.SEVERE,
            confidence = 0.9,
            originalPhrase = "chest pain",
            language = "English"
        )
        
        val symptomInfo = SymptomExtractor.toSymptomInfo(extracted)
        
        assertEquals(extracted.description, symptomInfo.description)
        assertEquals(extracted.severity, symptomInfo.severity)
        assertTrue(symptomInfo.notes.contains("English"))
        assertTrue(symptomInfo.notes.contains("90%"))
    }
    
    @Test
    fun `case insensitive matching`() {
        val transcript = "CHEST PAIN"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val chestPain = symptoms.find { it.symptomType == SymptomType.CHEST_PAIN }
        assertNotNull(chestPain)
    }
    
    @Test
    fun `partial phrase matching`() {
        val transcript = "I am experiencing shortness of breath and feeling weak"
        val symptoms = SymptomExtractor.extractSymptoms(transcript)
        
        assertTrue(symptoms.isNotEmpty())
        val breathingIssue = symptoms.find { it.symptomType == SymptomType.BREATHING_DIFFICULTY }
        assertNotNull(breathingIssue)
    }
}
