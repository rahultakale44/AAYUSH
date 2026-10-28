package com.aayush.emergency.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.R
import com.aayush.emergency.ui.components.AAYUSHCard
import com.aayush.emergency.ui.viewmodel.ProfileViewModel

/**
 * Main profile overview screen showing all sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOverviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPersonalInfo: () -> Unit,
    onNavigateToConditions: () -> Unit,
    onNavigateToMedications: () -> Unit,
    onNavigateToAllergies: () -> Unit,
    onNavigateToSurgeries: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToConsent: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val healthProfile by viewModel.healthProfile.collectAsState()
    val conditions by viewModel.medicalConditions.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val allergies by viewModel.allergies.collectAsState()
    val surgeries by viewModel.surgeries.collectAsState()
    val contacts by viewModel.emergencyContacts.collectAsState()
    val consent by viewModel.consentPreferences.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_overview_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileSectionCard(
                title = stringResource(R.string.profile_personal_info),
                status = if (healthProfile?.fullName?.isNotEmpty() == true) 
                    stringResource(R.string.status_completed) 
                else 
                    stringResource(R.string.status_empty),
                onClick = onNavigateToPersonalInfo
            )
            
            ProfileSectionCard(
                title = stringResource(R.string.profile_conditions),
                status = stringResource(R.string.status_items_count, conditions.size),
                onClick = onNavigateToConditions
            )
            
            ProfileSectionCard(
                title = stringResource(R.string.profile_medications),
                status = stringResource(R.string.status_items_count, medications.size),
                onClick = onNavigateToMedications
            )
            
            ProfileSectionCard(
                title = stringResource(R.string.profile_allergies),
                status = stringResource(R.string.status_items_count, allergies.size),
                onClick = onNavigateToAllergies
            )
            
            ProfileSectionCard(
                title = stringResource(R.string.profile_surgeries),
                status = stringResource(R.string.status_items_count, surgeries.size),
                onClick = onNavigateToSurgeries
            )
            
            ProfileSectionCard(
                title = stringResource(R.string.profile_emergency_contacts),
                status = stringResource(R.string.status_items_count, contacts.size),
                onClick = onNavigateToContacts
            )
            
            ProfileSectionCard(
                title = stringResource(R.string.profile_consent),
                status = if (consent != null) 
                    stringResource(R.string.status_completed) 
                else 
                    stringResource(R.string.status_empty),
                onClick = onNavigateToConsent
            )
        }
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    status: String,
    onClick: () -> Unit
) {
    AAYUSHCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
