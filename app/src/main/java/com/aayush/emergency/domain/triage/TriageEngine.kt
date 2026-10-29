package com.aayush.emergency.domain.triage

import com.aayush.emergency.data.model.SymptomInfo
import com.aayush.emergency.data.model.TriageResult
import com.aayush.emergency.data.model.UrgencyLevel

/**
 * Rule-based triage engine for symptom assessment
 * Uses transparent, testable rules to assess urgency
 * Does NOT diagnose diseases - only assesses urgency and provides guidance
 * 
 * RED FLAG SYMPTOMS require immediate 112 call:
 * - Chest pain/pressure
 * - Difficulty breathing/shortness of breath
 * - Severe bleeding
 * - Loss of consciousness/fainting
 * - Stroke symptoms (face drooping, arm weakness, speech difficulty)
 * - Severe head injury
 * - Severe allergic reaction
 * - Seizures
 * - Severe abdominal pain
 * - Confusion/altered mental state
 */
object TriageEngine {
    
    // Red flag keywords that trigger immediate urgency
    private val RED_FLAG_PATTERNS = listOf(
        RedFlagRule(
            pattern = listOf("chest pain", "chest pressure", "heart pain", "crushing chest"),
            flagName = "Possible cardiac event",
            guidance = "Call 112 immediately. Do not drive yourself. Stay calm and sit down."
        ),
        RedFlagRule(
            pattern = listOf("can't breathe", "difficulty breathing", "shortness of breath", "gasping"),
            flagName = "Severe breathing difficulty",
            guidance = "Call 112 immediately. Try to stay calm and sit upright if possible."
        ),
        RedFlagRule(
            pattern = listOf("severe bleeding", "heavy bleeding", "blood won't stop"),
            flagName = "Severe hemorrhage",
            guidance = "Call 112 immediately. Apply direct pressure to wound if safe to do so."
        ),
        RedFlagRule(
            pattern = listOf("unconscious", "fainted", "passed out", "unresponsive"),
            flagName = "Loss of consciousness",
            guidance = "Call 112 immediately. Do not leave the person alone."
        ),
        RedFlagRule(
            pattern = listOf("face drooping", "arm weak", "speech slurred", "can't talk properly"),
            flagName = "Possible stroke",
            guidance = "Call 112 immediately. Note the time symptoms started. Do not give food or drink."
        ),
        RedFlagRule(
            pattern = listOf("head injury", "hit head", "head trauma", "skull injury"),
            flagName = "Head trauma",
            guidance = "Call 112 immediately. Keep the person still and monitor their consciousness."
        ),
        RedFlagRule(
            pattern = listOf("allergic reaction", "throat swelling", "anaphylaxis", "severe allergy"),
            flagName = "Severe allergic reaction",
            guidance = "Call 112 immediately. Use EpiPen if available. Do not give oral medications if throat is swelling."
        ),
        RedFlagRule(
            pattern = listOf("seizure", "convulsion", "fitting"),
            flagName = "Seizure activity",
            guidance = "Call 112 immediately. Clear area of hazards. Do not restrain the person or put anything in their mouth."
        ),
        RedFlagRule(
            pattern = listOf("severe abdominal pain", "intense stomach pain", "abdomen rigid"),
            flagName = "Acute abdominal emergency",
            guidance = "Call 112 immediately. Do not eat or drink. Lie down in comfortable position."
        ),
        RedFlagRule(
            pattern = listOf("confused", "disoriented", "mental state changed", "not making sense"),
            flagName = "Altered mental status",
            guidance = "Call 112 immediately. Monitor person closely. Note their symptoms."
        ),
        RedFlagRule(
            pattern = listOf("severe burn", "large burn", "burned badly"),
            flagName = "Severe burn injury",
            guidance = "Call 112 immediately. Cool burn with clean water. Do not apply ice or ointments."
        ),
        RedFlagRule(
            pattern = listOf("suicide", "want to die", "self-harm", "kill myself"),
            flagName = "Mental health crisis",
            guidance = "Call 112 or mental health crisis line immediately. Do not leave the person alone."
        )
    )
    
    /**
     * Assess symptoms and generate triage result
     * Returns urgency level and guidance based on rule-based assessment
     */
    fun assessSymptoms(symptoms: List<SymptomInfo>): TriageResult {
        val redFlags = mutableListOf<String>()
        val rulesFired = mutableListOf<String>()
        val allText = symptoms.joinToString(" ") { 
            "${it.description} ${it.notes}".lowercase()
        }
        
        // Check for red flags
        for (rule in RED_FLAG_PATTERNS) {
            if (rule.matches(allText)) {
                redFlags.add(rule.flagName)
                rulesFired.add(rule.flagName)
            }
        }
        
        // Determine urgency and guidance
        return when {
            redFlags.isNotEmpty() -> TriageResult(
                urgencyLevel = UrgencyLevel.IMMEDIATE,
                redFlagsDetected = redFlags,
                guidance = buildImmediateGuidance(redFlags),
                shouldCall112 = true,
                assessmentTime = System.currentTimeMillis(),
                rulesFired = rulesFired
            )
            
            containsUrgentIndicators(allText) -> TriageResult(
                urgencyLevel = UrgencyLevel.URGENT,
                guidance = "Seek immediate medical attention. Visit the nearest hospital emergency department or call 112 for guidance.",
                shouldCall112 = false,
                assessmentTime = System.currentTimeMillis(),
                rulesFired = listOf("Urgent indicators detected")
            )
            
            containsModerateIndicators(allText) -> TriageResult(
                urgencyLevel = UrgencyLevel.MODERATE,
                guidance = "Schedule a doctor's appointment soon. Monitor your symptoms and seek immediate care if they worsen.",
                shouldCall112 = false,
                assessmentTime = System.currentTimeMillis(),
                rulesFired = listOf("Moderate concern indicators")
            )
            
            symptoms.isNotEmpty() -> TriageResult(
                urgencyLevel = UrgencyLevel.LOW,
                guidance = "Your symptoms appear non-urgent. Consider scheduling a routine appointment with your doctor.",
                shouldCall112 = false,
                assessmentTime = System.currentTimeMillis(),
                rulesFired = listOf("Non-urgent assessment")
            )
            
            else -> TriageResult(
                urgencyLevel = UrgencyLevel.UNKNOWN,
                guidance = "Unable to assess. If you are experiencing a medical emergency, call 112 immediately.",
                shouldCall112 = false,
                assessmentTime = System.currentTimeMillis(),
                rulesFired = emptyList()
            )
        }
    }
    
    private fun buildImmediateGuidance(redFlags: List<String>): String {
        val primary = redFlags.first()
        val rule = RED_FLAG_PATTERNS.find { it.flagName == primary }
        return "⚠️ CALL 112 IMMEDIATELY ⚠️\n\n${rule?.guidance ?: "This is a medical emergency. Call 112 now."}\n\nRed flags detected: ${redFlags.joinToString(", ")}"
    }
    
    private fun containsUrgentIndicators(text: String): Boolean {
        val urgentPatterns = listOf(
            "severe pain", "high fever", "vomiting blood", "coughing blood",
            "sudden weakness", "very dizzy", "can't walk", "extreme pain"
        )
        return urgentPatterns.any { text.contains(it) }
    }
    
    private fun containsModerateIndicators(text: String): Boolean {
        val moderatePatterns = listOf(
            "fever", "pain", "cough", "nausea", "vomiting", "diarrhea",
            "headache", "dizzy", "tired", "weakness"
        )
        return moderatePatterns.any { text.contains(it) }
    }
}

/**
 * Represents a red flag detection rule
 */
private data class RedFlagRule(
    val pattern: List<String>,
    val flagName: String,
    val guidance: String
) {
    fun matches(text: String): Boolean {
        return pattern.any { keyword ->
            text.contains(keyword)
        }
    }
}
