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
