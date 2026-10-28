package com.aayush.emergency.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.ui.screens.profile.ProfileOverviewScreen
import com.aayush.emergency.ui.viewmodel.ProfileViewModel

/**
 * Senior Health Profile screen - Phase 2 implementation
 */
@Composable
fun ProfileScreen(
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
    ProfileOverviewScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToPersonalInfo = onNavigateToPersonalInfo,
        onNavigateToConditions = onNavigateToConditions,
        onNavigateToMedications = onNavigateToMedications,
        onNavigateToAllergies = onNavigateToAllergies,
        onNavigateToSurgeries = onNavigateToSurgeries,
        onNavigateToContacts = onNavigateToContacts,
        onNavigateToConsent = onNavigateToConsent,
        viewModel = viewModel
    )
}
