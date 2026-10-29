package com.aayush.emergency.domain.triage

import com.aayush.emergency.data.model.SymptomInfo
import com.aayush.emergency.data.model.SymptomSeverity

/**
 * Symptom Fact Extractor
 * 
 * Extracts structured symptom facts from natural language transcripts.
 * Supports English, Hindi, and Marathi phrases.
 * 
 * IMPORTANT: This is a rule-based baseline extractor for the prototype.
 * It uses explicit pattern matching, NOT machine learning.
 * Design allows future ML/NLP service integration without replacing entire app.
 * 
 * Handles negation carefully - "I do not have chest pain" is different from "I have chest pain".
 * Unknown or uncertain facts remain marked as uncertain.
 */
object SymptomExtractor {
    
    /**
     * Extract symptom facts from natural language transcript
     * Returns list of detected symptoms with severity and original phrases
     */
    fun extractSymptoms(transcript: String): List<ExtractedSymptom> {
        val normalized = transcript.trim().lowercase()
        val symptoms = mutableListOf<ExtractedSymptom>()
        
        // Check for negation patterns
        val hasNegation = containsNegation(normalized)
        
        // If strong negation detected, mark as uncertain rather than detecting false positives
        if (hasNegation && isStrongNegation(normalized)) {
            symptoms.add(ExtractedSymptom(
                symptomType = SymptomType.UNCERTAIN,
                description = "Patient statement contains negation: $transcript",
                severity = SymptomSeverity.UNKNOWN,
                confidence = 0.3,
                originalPhrase = transcript,
                language = detectLanguage(normalized)
            ))
            return symptoms
        }
        
        // Extract chest pain
        val chestPain = detectChestPain(normalized)
        if (chestPain != null) symptoms.add(chestPain)
        
        // Extract breathing difficulty
        val breathingIssue = detectBreathingDifficulty(normalized)
        if (breathingIssue != null) symptoms.add(breathingIssue)
        
        // Extract loss of consciousness
        val consciousness = detectLossOfConsciousness(normalized)
        if (consciousness != null) symptoms.add(consciousness)
        
        // Extract severe bleeding
        val bleeding = detectSevereBleeding(normalized)
        if (bleeding != null) symptoms.add(bleeding)
        
        // Extract stroke symptoms
        val stroke = detectStrokeSymptoms(normalized)
        if (stroke != null) symptoms.add(stroke)
        
        // Extract head trauma
        val headTrauma = detectHeadTrauma(normalized)
        if (headTrauma != null) symptoms.add(headTrauma)
        
        // Extract severe allergic reaction
        val allergy = detectSevereAllergy(normalized)
        if (allergy != null) symptoms.add(allergy)
        
        // Extract seizure
        val seizure = detectSeizure(normalized)
        if (seizure != null) symptoms.add(seizure)
        
        // If no specific symptoms detected but text exists, mark as general symptom
        if (symptoms.isEmpty() && normalized.isNotBlank()) {
            symptoms.add(ExtractedSymptom(
                symptomType = SymptomType.GENERAL,
                description = transcript,
                severity = SymptomSeverity.MODERATE,
                confidence = 0.5,
                originalPhrase = transcript,
                language = detectLanguage(normalized)
            ))
        }
        
        return symptoms
    }
    
    /**
     * Convert extracted symptoms to SymptomInfo for emergency session
     */
    fun toSymptomInfo(extracted: ExtractedSymptom): SymptomInfo {
        return SymptomInfo(
            id = System.currentTimeMillis(),
            description = extracted.description,
            severity = extracted.severity,
            duration = "",
            notes = "Voice-reported (${extracted.language}). Confidence: ${(extracted.confidence * 100).toInt()}%",
            reportedAt = System.currentTimeMillis()
        )
    }
    
    // Detection functions for specific symptoms
    
    private fun detectChestPain(text: String): ExtractedSymptom? {
        val patterns = listOf(
            // English
            "chest pain", "chest pressure", "heart pain", "crushing chest", "tight chest",
            "chest hurts", "pain in chest", "chest ache",
            // Hindi (transliterated and Devanagari)
            "seene mein dard", "chhaati mein dard", "dil mein dard",
            "सीने में दर्द", "छाती में दर्द", "दिल में दर्द",
            // Marathi
            "chhatit dukhat", "छातीत दुखत", "हृदयात दुखत"
        )
        
        for (pattern in patterns) {
            if (text.contains(pattern)) {
                return ExtractedSymptom(
                    symptomType = SymptomType.CHEST_PAIN,
                    description = "Chest pain or discomfort",
                    severity = SymptomSeverity.SEVERE,
                    confidence = 0.9,
                    originalPhrase = pattern,
                    language = detectLanguage(text)
                )
            }
        }
        return null
    }
    
    private fun detectBreathingDifficulty(text: String): ExtractedSymptom? {
        val patterns = listOf(
            // English
            "can't breathe", "cannot breathe", "difficulty breathing", "shortness of breath",
            "gasping", "hard to breathe", "trouble breathing", "breathless",
            // Hindi (transliterated and Devanagari)
            "saans nahi aa rahi", "saans lene mein takleef", "saans lene mein dikkat",
            "साँस नहीं आ रही", "साँस लेने में तकलीफ", "साँस लेने में दिक्कत",
            // Marathi
            "shvas ghenyat tras", "श्वास घ्यायला त्रास", "श्वास घेण्यास त्रास"
        )
        
        for (pattern in patterns) {
            if (text.contains(pattern)) {
                return ExtractedSymptom(
                    symptomType = SymptomType.BREATHING_DIFFICULTY,
                    description = "Difficulty breathing or shortness of breath",
                    severity = SymptomSeverity.SEVERE,
                    confidence = 0.9,
                    originalPhrase = pattern,
                    language = detectLanguage(text)
                )
            }
        }
        return null
    }
    
    private fun detectLossOfConsciousness(text: String): ExtractedSymptom? {
        val patterns = listOf(
            // English
            "unconscious", "fainted", "passed out", "faint", "blacked out",
            "lost consciousness", "unresponsive",
            // Hindi
            "behosh", "behoshi", "girgaya", "gir gaya",
            "बेहोश", "बेहोशी", "गिर गया",
            // Marathi
            "bedhund", "behosh", "बेधुंद", "बेहोश"
        )
        
        for (pattern in patterns) {
            if (text.contains(pattern)) {
                return ExtractedSymptom(
                    symptomType = SymptomType.LOSS_OF_CONSCIOUSNESS,
                    description = "Loss of consciousness or fainting",
                    severity = SymptomSeverity.SEVERE,
                    confidence = 0.95,
                    originalPhrase = pattern,
                    language = detectLanguage(text)
                )
            }
        }
        return null
    }
    
    private fun detectSevereBleeding(text: String): ExtractedSymptom? {
        val patterns = listOf(
            // English
            "severe bleeding", "heavy bleeding", "blood won't stop", "bleeding heavily",
            "lot of blood", "bleeding badly",
            // Hindi
            "khoon beh raha", "bahut khoon", "khoon nahi ruk raha",
            "खून बह रहा", "बहुत खून", "खून नहीं रुक रहा",
            // Marathi
            "raktsrav", "khup rakta", "रक्तस्त्राव", "खूप रक्त"
        )
        
        for (pattern in patterns) {
            if (text.contains(pattern)) {
                return ExtractedSymptom(
                    symptomType = SymptomType.SEVERE_BLEEDING,
                    description = "Severe bleeding",
                    severity = SymptomSeverity.SEVERE,
                    confidence = 0.9,
                    originalPhrase = pattern,
                    language = detectLanguage(text)
                )
            }
        }
        return null
    }
    
    private fun detectStrokeSymptoms(text: String): ExtractedSymptom? {
        val patterns = listOf(
            // English
            "face drooping", "arm weakness", "arm weak", "speech slurred",
            "can't talk properly", "speech difficulty", "face numb", "face droops",
            "weak arm", "slurred speech",
            // Hindi
            "chehra teda", "haath kamzor", "bolne mein dikkat",
            "चेहरा टेढ़ा", "हाथ कमज़ोर", "बोलने में दिक्कत",
            // Marathi
            "chehara vakra", "haat kamjor", "bolnyat tras",
            "चेहरा वक्र", "हात कमजोर", "बोलण्यात त्रास"
        )
        
        for (pattern in patterns) {
            if (text.contains(pattern)) {
                return ExtractedSymptom(
                    symptomType = SymptomType.STROKE_SYMPTOMS,
                    description = "Possible stroke symptoms (face drooping, arm weakness, speech difficulty)",
                    severity = SymptomSeverity.SEVERE,
                    confidence = 0.85,
                    originalPhrase = pattern,
                    language = detectLanguage(text)
                )
            }
        }
        return null
    }
    
    private fun detectHeadTrauma(text: String): ExtractedSymptom? {
        val patterns = listOf(
            // English
            "head injury", "hit head", "head trauma", "hit my head",
            "head hurt", "banged head",
            // Hindi
            "sar pe chot", "sir pe laga", "सर पे चोट", "सिर पे लगा",
            // Marathi
            "dokkyala laga", "dokyavar chot", "डोक्याला लागला", "डोक्यावर चोट"
        )
        
        for (pattern in patterns) {
            if (text.contains(pattern)) {
                return ExtractedSymptom(
                    symptomType = SymptomType.HEAD_TRAUMA,
                    description = "Head injury or trauma",
                    severity = SymptomSeverity.SEVERE,
                    confidence = 0.9,
                    originalPhrase = pattern,
                    language = detectLanguage(text)
                )
            }
        }
        return null
    }
    
    private fun detectSevereAllergy(text: String): ExtractedSymptom? {
        val patterns = listOf(
            // English
            "allergic reaction", "throat swelling", "anaphylaxis", "severe allergy",
            "can't swallow", "throat closing",
            // Hindi
            "gala suj raha", "allergy", "गला सूज रहा", "एलर्जी",
            // Marathi
            "gala sujna", "allergy", "गळा सुजणे", "अ‍ॅलर्जी"
        )
        
        for (pattern in patterns) {
            if (text.contains(pattern)) {
                return ExtractedSymptom(
                    symptomType = SymptomType.SEVERE_ALLERGIC_REACTION,
                    description = "Severe allergic reaction",
                    severity = SymptomSeverity.SEVERE,
                    confidence = 0.9,
                    originalPhrase = pattern,
                    language = detectLanguage(text)
                )
            }
        }
        return null
    }
    
    private fun detectSeizure(text: String): ExtractedSymptom? {
        val patterns = listOf(
            // English
            "seizure", "convulsion", "fitting", "shaking uncontrollably",
            // Hindi
            "daura", "mirgi", "दौरा", "मिर्गी",
            // Marathi
            "apasmar", "mirgi", "अपस्मार", "मिर्गी"
        )
        
        for (pattern in patterns) {
            if (text.contains(pattern)) {
                return ExtractedSymptom(
                    symptomType = SymptomType.SEIZURE,
                    description = "Seizure or convulsion",
                    severity = SymptomSeverity.SEVERE,
                    confidence = 0.9,
                    originalPhrase = pattern,
                    language = detectLanguage(text)
                )
            }
        }
        return null
    }
    
    // Helper functions
    
    private fun containsNegation(text: String): Boolean {
        val negationWords = listOf(
            "not", "no", "never", "neither", "don't", "doesn't", "didn't", "won't", "can't",
            "nahi", "nahin", "नहीं", "mat", "मत"
        )
        return negationWords.any { text.contains(it) }
    }
    
    private fun isStrongNegation(text: String): Boolean {
        // Strong negation patterns that indicate explicit denial
        val strongNegation = listOf(
            "do not have", "don't have", "does not have", "doesn't have",
            "no chest pain", "no breathing", "not having",
            "nahi hai", "नहीं है"
        )
        return strongNegation.any { text.contains(it) }
    }
    
    private fun detectLanguage(text: String): String {
        return when {
            containsDevanagariScript(text) -> {
                when {
                    containsMarathiWords(text) -> "Marathi"
                    else -> "Hindi"
                }
            }
            containsMarathiTransliteratedWords(text) -> "Marathi"
            containsHindiTransliteratedWords(text) -> "Hindi"
            else -> "English"
        }
    }
    
    private fun containsDevanagariScript(text: String): Boolean {
        return text.any { it in '\u0900'..'\u097F' }
    }
    
    private fun containsMarathiWords(text: String): Boolean {
        val marathiIndicators = listOf("आहे", "त्रास", "दुखत", "लागला")
        return marathiIndicators.any { text.contains(it) }
    }
    
    private fun containsMarathiTransliteratedWords(text: String): Boolean {
        val marathiWords = listOf("ahe", "tras", "dukhat", "dokya")
        return marathiWords.any { text.contains(it) }
    }
    
    private fun containsHindiTransliteratedWords(text: String): Boolean {
        val hindiWords = listOf("hai", "takleef", "dard", "behosh", "sar")
        return hindiWords.any { text.contains(it) }
    }
}

/**
 * Represents an extracted symptom with metadata
 */
data class ExtractedSymptom(
    val symptomType: SymptomType,
    val description: String,
    val severity: SymptomSeverity,
    val confidence: Double,  // 0.0 to 1.0
    val originalPhrase: String,
    val language: String
)

/**
 * Types of symptoms that can be extracted
 */
enum class SymptomType {
    CHEST_PAIN,
    BREATHING_DIFFICULTY,
    LOSS_OF_CONSCIOUSNESS,
    SEVERE_BLEEDING,
    STROKE_SYMPTOMS,
    HEAD_TRAUMA,
    SEVERE_ALLERGIC_REACTION,
    SEIZURE,
    GENERAL,
    UNCERTAIN
}
