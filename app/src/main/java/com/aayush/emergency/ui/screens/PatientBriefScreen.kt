package com.aayush.emergency.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.R
import com.aayush.emergency.data.model.*
import com.aayush.emergency.ui.components.AAYUSHCard
import com.aayush.emergency.ui.components.ScreenHeader
import com.aayush.emergency.ui.theme.WarningAmber
import com.aayush.emergency.ui.viewmodel.EmergencyViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Patient Brief Screen - Emergency Responder Information
 * Displays consolidated emergency and health information
 * Respects consent preferences and clearly marks data provenance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientBriefScreen(
    onNavigateBack: () -> Unit,
    viewModel: EmergencyViewModel = viewModel()
) {
    val patientBrief by viewModel.patientBrief.collectAsState()
    
    // Generate patient brief when screen loads
    LaunchedEffect(Unit) {
        viewModel.generatePatientBrief()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.patient_brief_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            ScreenHeader(
                title = stringResource(R.string.patient_brief_title)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Disclaimer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = WarningAmber.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.patient_brief_disclaimer),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (patientBrief == null) {
                AAYUSHCard {
                    Text(
                        text = stringResource(R.string.patient_brief_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                PatientBriefContent(brief = patientBrief!!)
            }
        }
    }
}

@Composable
private fun PatientBriefContent(brief: PatientBrief) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    // Emergency Session Info
    brief.emergencySession?.let { session ->
        AAYUSHCard {
            Column {
                SectionTitle(stringResource(R.string.patient_brief_emergency_info))
                
                InfoRow(
                    label = stringResource(R.string.patient_brief_session_started),
                    value = dateFormat.format(Date(session.startTime))
                )
                
                InfoRow(
                    label = stringResource(R.string.patient_brief_status),
                    value = session.status.name
                )
                
                if (session.symptoms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.patient_brief_symptoms),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    session.symptoms.forEach { symptom ->
                        SymptomItem(symptom)
                    }
                }
                
                session.triageResult?.let { triage ->
                    Spacer(modifier = Modifier.height(12.dp))
                    TriageResultSection(triage)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Consent Status
    AAYUSHCard {
        Column {
            SectionTitle(stringResource(R.string.patient_brief_data_sharing))
            if (brief.consentGiven) {
                Text(
                    text = stringResource(R.string.patient_brief_consent_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.patient_brief_consent_not_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Health Profile (if consent given)
    if (brief.consentGiven) {
        brief.healthProfile?.let { profile ->
            if (profile.fullName.isNotEmpty()) {
                AAYUSHCard {
                    Column {
                        SectionTitle(stringResource(R.string.patient_brief_personal_info))
                        if (profile.fullName.isNotEmpty()) {
                            InfoRow(stringResource(R.string.personal_info_name), profile.fullName)
                        }
                        if (profile.dateOfBirth.isNotEmpty()) {
                            InfoRow(stringResource(R.string.personal_info_dob), profile.dateOfBirth)
                        }
                        if (profile.notes.isNotEmpty()) {
                            InfoRow(stringResource(R.string.personal_info_notes), profile.notes)
                        }
                        VerificationBadge(false)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Allergies (Critical!)
        if (brief.relevantAllergies.isNotEmpty()) {
            AAYUSHCard {
                Column {
                    SectionTitle("⚠️ ${stringResource(R.string.allergies_title)}")
                    brief.relevantAllergies.forEach { allergy ->
                        AllergyItem(allergy)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Medications
        if (brief.relevantMedications.isNotEmpty()) {
            AAYUSHCard {
                Column {
                    SectionTitle(stringResource(R.string.medications_title))
                    brief.relevantMedications.forEach { medication ->
                        MedicationItem(medication)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Medical Conditions
        if (brief.relevantConditions.isNotEmpty()) {
            AAYUSHCard {
                Column {
                    SectionTitle(stringResource(R.string.profile_conditions))
                    brief.relevantConditions.forEach { condition ->
                        ConditionItem(condition)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Emergency Contacts (always shown)
    if (brief.emergencyContacts.isNotEmpty()) {
        AAYUSHCard {
            Column {
                SectionTitle(stringResource(R.string.emergency_contacts_title))
                brief.emergencyContacts.forEach { contact ->
                    EmergencyContactItem(contact)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun SymptomItem(symptom: SymptomInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "• ${symptom.description}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        if (symptom.duration.isNotEmpty()) {
            Text(
                text = "  Duration: ${symptom.duration}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "  Severity: ${symptom.severity.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TriageResultSection(triage: TriageResult) {
    Column {
        Text(
            text = stringResource(R.string.patient_brief_triage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        val urgencyColor = when (triage.urgencyLevel) {
            UrgencyLevel.IMMEDIATE -> MaterialTheme.colorScheme.error
            UrgencyLevel.URGENT -> MaterialTheme.colorScheme.error
            UrgencyLevel.MODERATE -> WarningAmber
            UrgencyLevel.LOW -> MaterialTheme.colorScheme.primary
            UrgencyLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        
        Text(
            text = "Urgency: ${triage.urgencyLevel.name}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = urgencyColor
        )
        
        if (triage.redFlagsDetected.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ Red Flags: ${triage.redFlagsDetected.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AllergyItem(allergy: Allergy) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "• ${allergy.allergyName}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        if (allergy.severity.isNotEmpty()) {
            Text(
                text = "  Severity: ${allergy.severity}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (allergy.reaction.isNotEmpty()) {
            Text(
                text = "  Reaction: ${allergy.reaction}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        VerificationBadge(allergy.isVerified)
    }
}

@Composable
private fun MedicationItem(medication: Medication) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "• ${medication.medicationName}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        if (medication.dosage.isNotEmpty()) {
            Text(
                text = "  Dosage: ${medication.dosage}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (medication.frequency.isNotEmpty()) {
            Text(
                text = "  Frequency: ${medication.frequency}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        VerificationBadge(medication.isVerified)
    }
}

@Composable
private fun ConditionItem(condition: MedicalCondition) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "• ${condition.conditionName}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        if (condition.notes.isNotEmpty()) {
            Text(
                text = "  ${condition.notes}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        VerificationBadge(condition.isVerified)
    }
}

@Composable
private fun EmergencyContactItem(contact: EmergencyContact) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${contact.relationship} • ${contact.phoneNumber}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VerificationBadge(isVerified: Boolean) {
    Text(
        text = if (isVerified) "✓ Verified" else "User-provided",
        style = MaterialTheme.typography.bodySmall,
        color = if (isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}
