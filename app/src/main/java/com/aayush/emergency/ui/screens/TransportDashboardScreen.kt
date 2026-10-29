package com.aayush.emergency.ui.screens

import androidx.compose.foundation.background
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
import com.aayush.emergency.ui.components.ScreenHeader
import com.aayush.emergency.ui.theme.WarningAmber
import com.aayush.emergency.ui.viewmodel.AuthViewModel
import com.aayush.emergency.ui.viewmodel.TransportViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transport Coordinator Dashboard
 * 
 * SIMULATED TRANSPORT COORDINATION
 * This does NOT dispatch real ambulances or contact emergency services
 * All transport status updates are demonstration only
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransportViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val cases by viewModel.cases.collectAsState()
    val selectedCase by viewModel.selectedCase.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val serverAvailable by viewModel.serverAvailable.collectAsState()
    val summary = viewModel.getTransportSummary()
    
    LaunchedEffect(Unit) {
        viewModel.checkServerAndLoadData()
    }
    
    if (selectedCase != null) {
        TransportCaseDetailScreen(
            case = selectedCase!!,
            onBack = { viewModel.clearSelectedCase() },
            onUpdateStatus = { status ->
                viewModel.updateTransportStatus(selectedCase!!.id, status)
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.transport_dashboard_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.checkServerAndLoadData() }) {
                            Icon(
                                Icons.Default.Refresh,
                                "Refresh",
                                tint = if (serverAvailable) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        IconButton(
                            onClick = {
                                authViewModel.logout()
                                onNavigateBack()
                            }
                        ) {
                            Icon(Icons.Default.Logout, stringResource(R.string.auth_logout))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Simulation Warning
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarningAmber.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.transport_simulation_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryCard(
                                title = stringResource(R.string.transport_pending),
                                count = summary.pendingRequests,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                title = stringResource(R.string.transport_en_route),
                                count = summary.enRoute,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryCard(
                                title = stringResource(R.string.transport_transporting),
                                count = summary.transporting,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                title = stringResource(R.string.transport_completed),
                                count = summary.completed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Cases List
                    item {
                        Text(
                            text = stringResource(R.string.transport_cases_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (cases.isEmpty()) {
                        item {
                            AAYUSHCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.transport_no_cases),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(cases) { case ->
                            TransportCaseCard(
                                case = case,
                                onClick = { viewModel.selectCase(case.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TransportCaseCard(
    case: EmergencyCase,
    onClick: () -> Unit
) {
    val urgencyColor = when (case.session.triageResult?.urgencyLevel) {
        UrgencyLevel.IMMEDIATE -> MaterialTheme.colorScheme.error
        UrgencyLevel.URGENT -> MaterialTheme.colorScheme.error
        UrgencyLevel.MODERATE -> WarningAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = case.patientName.ifEmpty { "Case #${case.id}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Badge(
                    containerColor = urgencyColor.copy(alpha = 0.2f),
                    contentColor = urgencyColor
                ) {
                    Text(
                        text = case.session.triageResult?.urgencyLevel?.name ?: "UNKNOWN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Symptoms
            if (case.session.symptoms.isNotEmpty()) {
                Text(
                    text = case.session.symptoms.firstOrNull()?.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Transport Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportStatusChip(status = case.transportStatus)
                
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(case.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransportStatusChip(status: TransportStatus) {
    val color = when (status) {
        TransportStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant
        TransportStatus.REQUESTED -> MaterialTheme.colorScheme.primary
        TransportStatus.ACKNOWLEDGED -> MaterialTheme.colorScheme.primary
        TransportStatus.EN_ROUTE -> MaterialTheme.colorScheme.tertiary
        TransportStatus.ARRIVED -> MaterialTheme.colorScheme.tertiary
        TransportStatus.TRANSPORTING -> MaterialTheme.colorScheme.secondary
        TransportStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = status.name.replace("_", " "),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransportCaseDetailScreen(
    case: EmergencyCase,
    onBack: () -> Unit,
    onUpdateStatus: (TransportStatus) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(case.patientName.ifEmpty { "Case #${case.id}" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Urgency Alert
            if (case.session.triageResult?.urgencyLevel == UrgencyLevel.IMMEDIATE) {
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
                        Text(
                            text = stringResource(R.string.transport_immediate_case),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Case Info
            AAYUSHCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.transport_case_info),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    InfoRow("Case ID", case.id.toString())
                    InfoRow("Patient", case.patientName.ifEmpty { "Anonymous" })
                    InfoRow(
                        "Started",
                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(case.session.startTime))
                    )
                    InfoRow(
                        "Urgency",
                        case.session.triageResult?.urgencyLevel?.name ?: "UNKNOWN"
                    )
                }
            }
            
            // Symptoms
            AAYUSHCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.transport_symptoms),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    case.session.symptoms.forEach { symptom ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("• ", fontWeight = FontWeight.Bold)
                            Text(
                                text = symptom.description,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            
            // Transport Status Updates
            AAYUSHCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.transport_status_updates),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = stringResource(R.string.transport_current_status),
                        style = MaterialTheme.typography.titleSmall
                    )
                    TransportStatusChip(case.transportStatus)
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.transport_update_status),
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    // Status Buttons
                    val nextStatuses = getNextValidStatuses(case.transportStatus)
                    nextStatuses.forEach { status ->
                        Button(
                            onClick = { onUpdateStatus(status) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(status.name.replace("_", " "))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun getNextValidStatuses(current: TransportStatus): List<TransportStatus> {
    return when (current) {
        TransportStatus.NONE -> listOf(TransportStatus.REQUESTED)
        TransportStatus.REQUESTED -> listOf(TransportStatus.ACKNOWLEDGED)
        TransportStatus.ACKNOWLEDGED -> listOf(TransportStatus.EN_ROUTE)
        TransportStatus.EN_ROUTE -> listOf(TransportStatus.ARRIVED)
        TransportStatus.ARRIVED -> listOf(TransportStatus.TRANSPORTING)
        TransportStatus.TRANSPORTING -> listOf(TransportStatus.COMPLETED)
        TransportStatus.COMPLETED -> emptyList()
    }
}
