package com.aayush.emergency.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aayush.emergency.ui.viewmodel.AuthViewModel
import com.aayush.emergency.ui.viewmodel.HospitalViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Hospital Operations Dashboard
 * 
 * Combined hospital staff and caregiver assignment interface
 * Hospital staff act as caregivers assigned to patient cases
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCaseDetail: (Long) -> Unit,
    viewModel: HospitalViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val cases by viewModel.cases.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val serverAvailable by viewModel.serverAvailable.collectAsState()
    val summary = viewModel.getSummary()
    
    var filterUrgency by remember { mutableStateOf<UrgencyLevel?>(null) }
    var filterStatus by remember { mutableStateOf<HospitalCaseStatus?>(null) }
    
    val filteredCases = remember(cases, filterUrgency, filterStatus) {
        var result = cases
        if (filterUrgency != null) {
            result = result.filter { it.session.triageResult?.urgencyLevel == filterUrgency }
        }
        if (filterStatus != null) {
            result = result.filter { it.hospitalStatus == filterStatus }
        }
        result
    }
    
    LaunchedEffect(Unit) {
        viewModel.checkServerAndLoadData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hospital_dashboard_title)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            item {
                Text(
                    text = stringResource(R.string.hospital_overview),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HospitalSummaryCard(
                        title = stringResource(R.string.hospital_total_cases),
                        count = summary.totalCases,
                        icon = Icons.Default.Assignment,
                        modifier = Modifier.weight(1f)
                    )
                    HospitalSummaryCard(
                        title = stringResource(R.string.hospital_critical_cases),
                        count = summary.criticalCases,
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HospitalSummaryCard(
                        title = stringResource(R.string.hospital_new_cases),
                        count = summary.newCases,
                        icon = Icons.Default.Notifications,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    HospitalSummaryCard(
                        title = stringResource(R.string.hospital_active_cases),
                        count = summary.activeCases,
                        icon = Icons.Default.LocalHospital,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                HospitalSummaryCard(
                    title = stringResource(R.string.hospital_available_staff),
                    count = summary.availableStaff,
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterUrgency == UrgencyLevel.IMMEDIATE,
                        onClick = {
                            filterUrgency = if (filterUrgency == UrgencyLevel.IMMEDIATE) null
                            else UrgencyLevel.IMMEDIATE
                        },
                        label = { Text("Critical") }
                    )
                    FilterChip(
                        selected = filterStatus == HospitalCaseStatus.NEW,
                        onClick = {
                            filterStatus = if (filterStatus == HospitalCaseStatus.NEW) null
                            else HospitalCaseStatus.NEW
                        },
                        label = { Text("New") }
                    )
                    if (filterUrgency != null || filterStatus != null) {
                        TextButton(onClick = {
                            filterUrgency = null
                            filterStatus = null
                        }) {
                            Text("Clear")
                        }
                    }
                }
            }
            
            // Case Queue
            item {
                Text(
                    text = stringResource(R.string.hospital_case_queue),
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
            } else if (filteredCases.isEmpty()) {
                item {
                    AAYUSHCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (filterUrgency != null || filterStatus != null) {
                                stringResource(R.string.hospital_no_filtered_cases)
                            } else {
                                stringResource(R.string.hospital_no_cases)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredCases) { case ->
                    HospitalCaseCard(
                        case = case,
                        onClick = { onNavigateToCaseDetail(case.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HospitalSummaryCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
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
}

@Composable
private fun HospitalCaseCard(
    case: EmergencyCase,
    onClick: () -> Unit
) {
    val urgencyColor = when (case.session.triageResult?.urgencyLevel) {
        UrgencyLevel.IMMEDIATE -> MaterialTheme.colorScheme.error
        UrgencyLevel.URGENT -> MaterialTheme.colorScheme.error
        UrgencyLevel.MODERATE -> WarningAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val isNewOrCritical = case.hospitalStatus == HospitalCaseStatus.NEW ||
            case.session.triageResult?.urgencyLevel in listOf(UrgencyLevel.IMMEDIATE, UrgencyLevel.URGENT)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isNewOrCritical) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNewOrCritical) 4.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = case.patientName.ifEmpty { "Case #${case.id}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(case.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
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
                    text = case.session.symptoms.joinToString(", ") { it.description },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hospital Status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = case.hospitalStatus.name.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Assigned Staff
                if (case.assignedStaff.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${case.assignedStaff.size} staff",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.hospital_unassigned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
