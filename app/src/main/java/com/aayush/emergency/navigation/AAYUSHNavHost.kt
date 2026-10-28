package com.aayush.emergency.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aayush.emergency.ui.screens.ContactsScreen
import com.aayush.emergency.ui.screens.EmergencyStartScreen
import com.aayush.emergency.ui.screens.HomeScreen
import com.aayush.emergency.ui.screens.JourneyScreen
import com.aayush.emergency.ui.screens.LanguageScreen
import com.aayush.emergency.ui.screens.ProfileScreen
import com.aayush.emergency.ui.screens.SymptomCaptureScreen
import com.aayush.emergency.ui.screens.profile.*

/**
 * Main navigation host for AAYUSH application
 */
@Composable
fun AAYUSHNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                onNavigateToLanguage = { navController.navigate(Screen.Language.route) },
                onNavigateToJourney = { navController.navigate(Screen.Journey.route) },
                onNavigateToEmergency = { navController.navigate(Screen.EmergencyStart.route) },
                onNavigateToSymptomCapture = { navController.navigate(Screen.SymptomCapture.route) }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPersonalInfo = { navController.navigate(Screen.ProfilePersonalInfo.route) },
                onNavigateToConditions = { navController.navigate(Screen.ProfileConditions.route) },
                onNavigateToMedications = { navController.navigate(Screen.ProfileMedications.route) },
                onNavigateToAllergies = { navController.navigate(Screen.ProfileAllergies.route) },
                onNavigateToSurgeries = { navController.navigate(Screen.ProfileSurgeries.route) },
                onNavigateToContacts = { navController.navigate(Screen.ProfileContacts.route) },
                onNavigateToConsent = { navController.navigate(Screen.ProfileConsent.route) }
            )
        }
        
        composable(Screen.ProfilePersonalInfo.route) {
            PersonalInfoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ProfileConditions.route) {
            MedicalConditionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ProfileMedications.route) {
            MedicationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ProfileAllergies.route) {
            AllergiesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ProfileSurgeries.route) {
            SurgeriesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ProfileContacts.route) {
            EmergencyContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ProfileConsent.route) {
            ConsentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Contacts.route) {
            ContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Language.route) {
            LanguageScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Journey.route) {
            JourneyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.EmergencyStart.route) {
            EmergencyStartScreen(
                onNavigateBack = { navController.popBackStack() },
                onContinue = { navController.popBackStack() }
            )
        }
        
        composable(Screen.SymptomCapture.route) {
            SymptomCaptureScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
