package com.aayush.emergency.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.R
import com.aayush.emergency.data.model.EmergencySession
import com.aayush.emergency.data.model.EmergencyStatus
import com.aayush.emergency.ui.components.AAYUSHButton
import com.aayush.emergency.ui.components.AAYUSHCard
import com.aayush.emergency.ui.components.ScreenHeader
import com.aayush.emergency.ui.viewmodel.EmergencyViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Emergency Journey tracking screen
 * Shows current emergency session status and progress
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    onNavigateBack: () -> Unit,
    onViewBrief: () -> Unit,
    viewModel: EmergencyViewModel = viewModel()
) {
    val activeSession by viewModel.activeSession.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journey_title)) },
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
                title = stringResource(R.string.journey_title)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (activeSession == null) {
                AAYUSHCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.journey_no_active_session),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                EmergencyJourneyContent(
                    session = activeSession!!,
                    onViewBrief = onViewBrief,
                    onComplete = { viewModel.completeEmergency() },
                    onCancel = { viewModel.cancelEmergency() }
                )
            }
        }
    }
}

@Composable
private fun EmergencyJourneyContent(
    session: EmergencySession,
    onViewBrief: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    AAYUSHCard {
        Column {
            Text(
                text = stringResource(R.string.journey_active_session),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Session Status
            StatusItem(
                label = stringResource(R.string.journey_status),
                value = getStatusString(session.status),
                completed = true
            )
            
            // Start Time
            StatusItem(
                label = stringResource(R.string.journey_started),
                value = dateFormat.format(Date(session.startTime)),
                completed = true
            )
            
            // Symptoms Captured
            StatusItem(
                label = stringResource(R.string.journey_symptoms),
                value = "${session.symptoms.size} ${stringResource(R.string.journey_symptoms_recorded)}",
                completed = session.symptoms.isNotEmpty()
            )
            
            // Triage Completed
            session.triageResult?.let { triage ->
                StatusItem(
                    label = stringResource(R.string.journey_triage),
                    value = "${stringResource(R.string.journey_urgency)}: ${triage.urgencyLevel.name}",
                    completed = true
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Actions
            if (session.status == EmergencyStatus.TRIAGED) {
                AAYUSHButton(
                    text = stringResource(R.string.journey_view_brief),
                    onClick = onViewBrief,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.journey_complete))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.journey_cancel))
                }
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String,
    completed: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = if (completed) "✓" else "○",
            style = MaterialTheme.typography.titleMedium,
            color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun getStatusString(status: EmergencyStatus): String {
    return when (status) {
        EmergencyStatus.STARTED -> stringResource(R.string.journey_status_started)
        EmergencyStatus.SYMPTOMS_CAPTURED -> stringResource(R.string.journey_status_symptoms)
        EmergencyStatus.TRIAGED -> stringResource(R.string.journey_status_triaged)
        EmergencyStatus.COMPLETED -> stringResource(R.string.journey_status_completed)
        EmergencyStatus.CANCELLED -> stringResource(R.string.journey_status_cancelled)
    }
}
