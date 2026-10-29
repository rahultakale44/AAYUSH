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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.R
import com.aayush.emergency.data.model.SymptomSeverity
import com.aayush.emergency.data.model.UrgencyLevel
import com.aayush.emergency.ui.components.AAYUSHButton
import com.aayush.emergency.ui.components.AAYUSHCard
import com.aayush.emergency.ui.components.ScreenHeader
import com.aayush.emergency.ui.theme.WarningAmber
import com.aayush.emergency.ui.viewmodel.EmergencyViewModel

/**
 * Symptom capture screen with text-based entry and automatic triage
 * Detects red-flag symptoms and provides immediate guidance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCaptureScreen(
    onNavigateBack: () -> Unit,
    onViewBrief: () -> Unit,
    viewModel: EmergencyViewModel = viewModel()
) {
    val activeSession by viewModel.activeSession.collectAsState()
    
    var symptomDescription by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(SymptomSeverity.MODERATE) }
    var showTriageResult by remember { mutableStateOf(false) }
    
    // Monitor triage result
    LaunchedEffect(activeSession?.triageResult) {
        if (activeSession?.triageResult != null) {
            showTriageResult = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.symptom_capture_title)) },
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
                title = stringResource(R.string.symptom_capture_title)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show triage result if available
            activeSession?.triageResult?.let { triage ->
                if (showTriageResult) {
                    TriageResultCard(triage, onViewBrief)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Symptom Entry Form
            AAYUSHCard {
                Column {
                    Text(
                        text = stringResource(R.string.symptom_entry_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Symptom Description
                    OutlinedTextField(
                        value = symptomDescription,
                        onValueChange = { symptomDescription = it },
                        label = { Text(stringResource(R.string.symptom_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text(stringResource(R.string.symptom_description_hint)) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Severity Selection
                    Text(
                        text = stringResource(R.string.symptom_severity),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeverityChip(
                            text = stringResource(R.string.severity_mild),
                            selected = selectedSeverity == SymptomSeverity.MILD,
                            onClick = { selectedSeverity = SymptomSeverity.MILD }
                        )
                        SeverityChip(
                            text = stringResource(R.string.severity_moderate),
                            selected = selectedSeverity == SymptomSeverity.MODERATE,
                            onClick = { selectedSeverity = SymptomSeverity.MODERATE }
                        )
                        SeverityChip(
                            text = stringResource(R.string.severity_severe),
                            selected = selectedSeverity == SymptomSeverity.SEVERE,
                            onClick = { selectedSeverity = SymptomSeverity.SEVERE }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Duration
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text(stringResource(R.string.symptom_duration)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.symptom_duration_hint)) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Additional Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.symptom_notes)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        placeholder = { Text(stringResource(R.string.symptom_notes_hint)) }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    AAYUSHButton(
                        text = stringResource(R.string.symptom_add_button),
                        onClick = {
                            if (symptomDescription.isNotBlank()) {
                                viewModel.addSymptom(
                                    description = symptomDescription,
                                    severity = selectedSeverity,
                                    duration = duration,
                                    notes = notes
                                )
                                // Clear form
                                symptomDescription = ""
                                duration = ""
                                notes = ""
                                selectedSeverity = SymptomSeverity.MODERATE
                            }
                        },
                        enabled = symptomDescription.isNotBlank()
                    )
                }
            }
            
            // Show recorded symptoms
            activeSession?.symptoms?.let { symptoms ->
                if (symptoms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AAYUSHCard {
                        Column {
                            Text(
                                text = stringResource(R.string.symptom_recorded),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            symptoms.forEach { symptom ->
                                Text(
                                    text = "• ${symptom.description}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (symptom.duration.isNotEmpty()) {
                                    Text(
                                        text = "  Duration: ${symptom.duration}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
private fun TriageResultCard(
    triage: com.aayush.emergency.data.model.TriageResult,
    onViewBrief: () -> Unit
) {
    val backgroundColor = when (triage.urgencyLevel) {
        UrgencyLevel.IMMEDIATE, UrgencyLevel.URGENT -> MaterialTheme.colorScheme.errorContainer
        UrgencyLevel.MODERATE -> WarningAmber.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when (triage.urgencyLevel) {
        UrgencyLevel.IMMEDIATE, UrgencyLevel.URGENT -> MaterialTheme.colorScheme.error
        UrgencyLevel.MODERATE -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            if (triage.shouldCall112) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.triage_call_112_now),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Text(
                text = "${stringResource(R.string.triage_urgency)}: ${triage.urgencyLevel.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = triage.guidance,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (triage.redFlagsDetected.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${stringResource(R.string.triage_red_flags)}: ${triage.redFlagsDetected.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AAYUSHButton(
                text = stringResource(R.string.triage_view_patient_brief),
                onClick = onViewBrief
            )
        }
    }
}
