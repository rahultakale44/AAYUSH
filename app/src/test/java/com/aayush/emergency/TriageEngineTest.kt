package com.aayush.emergency

import com.aayush.emergency.data.model.SymptomInfo
import com.aayush.emergency.data.model.SymptomSeverity
import com.aayush.emergency.data.model.UrgencyLevel
import com.aayush.emergency.domain.triage.TriageEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TriageEngine
 * Tests red-flag detection, urgency levels, and guidance
 */
class TriageEngineTest {
    
    @Test
    fun `chest pain triggers immediate urgency and call 112`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "chest pain and pressure",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
        assertTrue(result.redFlagsDetected.isNotEmpty())
        assertTrue(result.guidance.contains("112"))
    }
    
    @Test
    fun `difficulty breathing triggers immediate urgency`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "can't breathe properly",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
        assertTrue(result.redFlagsDetected.any { it.contains("breathing", ignoreCase = true) })
    }
    
    @Test
    fun `stroke symptoms trigger immediate urgency`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "face drooping and speech slurred",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
        assertTrue(result.redFlagsDetected.any { it.contains("stroke", ignoreCase = true) })
    }
    
    @Test
    fun `severe bleeding triggers immediate urgency`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "severe bleeding won't stop",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
    }
    
    @Test
    fun `unconscious triggers immediate urgency`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "person passed out and unresponsive",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
        assertTrue(result.redFlagsDetected.any { it.contains("consciousness", ignoreCase = true) })
    }
    
    @Test
    fun `seizure triggers immediate urgency`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "having a seizure",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
    }
    
    @Test
    fun `allergic reaction triggers immediate urgency`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "severe allergic reaction with throat swelling",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
    }
    
    @Test
    fun `mental health crisis triggers immediate urgency`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "want to die",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
    }
    
    @Test
    fun `severe pain without red flags is urgent`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "severe pain in my leg",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
        assertEquals(false, result.shouldCall112)
    }
    
    @Test
    fun `high fever is urgent`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "high fever and feeling very unwell",
                severity = SymptomSeverity.MODERATE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
        assertEquals(false, result.shouldCall112)
    }
    
    @Test
    fun `moderate symptoms without urgent indicators are moderate`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "headache and feeling tired",
                severity = SymptomSeverity.MODERATE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.MODERATE, result.urgencyLevel)
        assertEquals(false, result.shouldCall112)
    }
    
    @Test
    fun `mild symptoms are low urgency`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "minor cut on finger",
                severity = SymptomSeverity.MILD,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.LOW, result.urgencyLevel)
        assertEquals(false, result.shouldCall112)
    }
    
    @Test
    fun `empty symptoms list returns unknown`() {
        val symptoms = emptyList<SymptomInfo>()
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.UNKNOWN, result.urgencyLevel)
        assertEquals(false, result.shouldCall112)
    }
    
    @Test
    fun `multiple symptoms with one red flag triggers immediate`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "headache",
                severity = SymptomSeverity.MILD,
                reportedAt = System.currentTimeMillis()
            ),
            SymptomInfo(
                id = 2L,
                description = "chest pain",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
    }
    
    @Test
    fun `red flag detection is case insensitive`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "CHEST PAIN AND PRESSURE",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertEquals(UrgencyLevel.IMMEDIATE, result.urgencyLevel)
        assertTrue(result.shouldCall112)
    }
    
    @Test
    fun `triage result includes assessment time`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "mild headache",
                severity = SymptomSeverity.MILD,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertTrue(result.assessmentTime > 0)
    }
    
    @Test
    fun `triage result includes rules fired`() {
        val symptoms = listOf(
            SymptomInfo(
                id = 1L,
                description = "chest pain",
                severity = SymptomSeverity.SEVERE,
                reportedAt = System.currentTimeMillis()
            )
        )
        
        val result = TriageEngine.assessSymptoms(symptoms)
        
        assertTrue(result.rulesFired.isNotEmpty())
    }
}
