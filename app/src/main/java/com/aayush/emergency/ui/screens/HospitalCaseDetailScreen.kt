package com.aayush.emergency.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.R
import com.aayush.emergency.data.model.*
import com.aayush.emergency.ui.components.AAYUSHCard
import com.aayush.emergency.ui.theme.WarningAmber
import com.aayush.emergency.ui.viewmodel.HospitalViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Hospital Case Detail Screen
 * 
 * Shows full patient brief, assigns hospital staff as caregivers
 * Respects consent preferences for medical data display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalCaseDetailScreen(
    caseId: Long,
    onNavigateBack: () -> Unit,
    viewModel: HospitalViewModel = viewModel()
) {
    val selectedCase by viewModel.selectedCase.collectAsState()
    val staff by viewModel.staff.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showStaffDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(caseId) {
        viewModel.selectCase(caseId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(selectedCase?.patientName?.ifEmpty { "Case #$caseId" } ?: "Case #$caseId")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (selectedCase == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val case = selectedCase!!
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Urgency Alert
                    if (case.session.triageResult?.urgencyLevel in listOf(
                            UrgencyLevel.IMMEDIATE,
                            UrgencyLevel.URGENT
                        )
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = case.session.triageResult?.urgencyLevel?.name
                                                ?: "CRITICAL",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        if (case.session.triageResult?.shouldCall112 == true) {
                                            Text(
                                                text = stringResource(R.string.hospital_call_112_recommended),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Emergency Session Info
                    item {
                        AAYUSHCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.hospital_emergency_info),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                DetailRow(
                                    "Started",
                                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                        .format(Date(case.session.startTime))
                                )
                                DetailRow("Case ID", case.id.toString())
                                DetailRow(
                                    "Urgency",
                                    case.session.triageResult?.urgencyLevel?.name ?: "UNKNOWN"
                                )
                                DetailRow("Hospital Status", case.hospitalStatus.name.replace("_", " "))
                                DetailRow("Transport Status", case.transportStatus.name.replace("_", " "))
                            }
                        }
                    }
                    
                    // Symptoms & Triage
                    item {
                        AAYUSHCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.hospital_symptoms_triage),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                case.session.symptoms.forEach { symptom ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row {
                                            Text("• ", fontWeight = FontWeight.Bold)
                                            Text(
                                                text = symptom.description,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
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
                                
                                case.session.triageResult?.let { triage ->
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(
                                        text = stringResource(R.string.hospital_triage_assessment),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (triage.redFlagsDetected.isNotEmpty()) {
                                        Text(
                                            text = "⚠️ Red Flags: ${triage.redFlagsDetected.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Patient Brief (Consent-Aware)
                    if (case.patientBrief != null) {
                        val brief = case.patientBrief!!
                        
                        item {
                            AAYUSHCard {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = stringResource(R.string.hospital_patient_brief),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (!brief.consentGiven) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = WarningAmber.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.hospital_no_consent),
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(12.dp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        // Allergies (Critical!)
                                        if (brief.relevantAllergies.isNotEmpty()) {
                                            Text(
                                                text = "⚠️ ${stringResource(R.string.allergies_title)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            brief.relevantAllergies.forEach { allergy ->
                                                Text(
                                                    text = "• ${allergy.allergyName} (${allergy.severity})",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        
                                        // Medications
                                        if (brief.relevantMedications.isNotEmpty()) {
                                            Text(
                                                text = stringResource(R.string.medications_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            brief.relevantMedications.forEach { med ->
                                                Text(
                                                    text = "• ${med.medicationName}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                        
                                        // Conditions
                                        if (brief.relevantConditions.isNotEmpty()) {
                                            Text(
                                                text = stringResource(R.string.profile_conditions),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            brief.relevantConditions.forEach { condition ->
                                                Text(
                                                    text = "• ${condition.conditionName}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Emergency Contacts (Always shown)
                                    if (brief.emergencyContacts.isNotEmpty()) {
                                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                                        Text(
                                            text = stringResource(R.string.emergency_contacts_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        brief.emergencyContacts.forEach { contact ->
                                            Text(
                                                text = "${contact.name} (${contact.relationship}) - ${contact.phoneNumber}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        text = brief.disclaimer,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Assigned Staff
                    item {
                        AAYUSHCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.hospital_assigned_staff),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    IconButton(onClick = { showStaffDialog = true }) {
                                        Icon(Icons.Default.Add, "Assign Staff")
                                    }
                                }
                                
                                if (case.assignedStaff.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.hospital_no_staff_assigned),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    case.assignedStaff.forEach { assignment ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = assignment.staffName,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = assignment.service.name.replace("_", " "),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.removeStaffAssignment(
                                                            case.id,
                                                            assignment.staffId
                                                        )
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        "Remove",
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Status Actions
                    item {
                        AAYUSHCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.hospital_case_actions),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                if (case.hospitalStatus == HospitalCaseStatus.NEW) {
                                    Button(
                                        onClick = {
                                            viewModel.acknowledgeCase(case.id)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.hospital_acknowledge_case))
                                    }
                                }
                                
                                if (case.hospitalStatus in listOf(
                                        HospitalCaseStatus.ACKNOWLEDGED,
                                        HospitalCaseStatus.STAFF_ASSIGNED
                                    )
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.updateCaseStatus(
                                                case.id,
                                                HospitalCaseStatus.CARE_IN_PROGRESS
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.hospital_start_care))
                                    }
                                }
                                
                                if (case.hospitalStatus == HospitalCaseStatus.CARE_IN_PROGRESS) {
                                    Button(
                                        onClick = {
                                            viewModel.updateCaseStatus(
                                                case.id,
                                                HospitalCaseStatus.COMPLETED
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.hospital_complete_case))
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Error Message
                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
            
            // Staff Assignment Dialog
            if (showStaffDialog) {
                StaffAssignmentDialog(
                    availableStaff = staff,
                    assignedStaffIds = case.assignedStaff.map { it.staffId },
                    onDismiss = { showStaffDialog = false },
                    onAssign = { selectedStaff ->
                        viewModel.assignStaff(case.id, selectedStaff)
                        showStaffDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StaffAssignmentDialog(
    availableStaff: List<HospitalStaff>,
    assignedStaffIds: List<String>,
    onDismiss: () -> Unit,
    onAssign: (HospitalStaff) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hospital_assign_staff_dialog)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableStaff.filter { it.id !in assignedStaffIds }) { staff ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAssign(staff) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = staff.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = staff.service.name.replace("_", " "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = staff.availability.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (staff.availability == StaffAvailability.AVAILABLE) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}
