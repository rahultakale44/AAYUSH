package com.aayush.emergency.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aayush.emergency.ui.screens.*
import com.aayush.emergency.ui.screens.profile.*

/**
 * Main navigation host for AAYUSH application
 */
@Composable
fun AAYUSHNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Phase 1: Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Phase 1: Role Selection (entry point after splash)
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    // For demo: Navigate directly to role-specific screens
                    // In full implementation, would navigate to role-specific auth
                    when (role.name) {
                        "SENIOR_CITIZEN" -> navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = false }
                        }
                        "TRANSPORT_COORDINATOR" -> navController.navigate(Screen.TransportDashboard.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = false }
                        }
                        "HOSPITAL_STAFF" -> navController.navigate(Screen.HospitalDashboard.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = false }
                        }
                    }
                }
            )
        }
        
        // Phase 4: Transport Coordinator Dashboard
        composable(Screen.TransportDashboard.route) {
            TransportDashboardScreen(
                onNavigateBack = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Phase 4: Hospital Dashboard
        composable(Screen.HospitalDashboard.route) {
            HospitalDashboardScreen(
                onNavigateBack = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToCaseDetail = { caseId ->
                    navController.navigate(Screen.HospitalCaseDetail.createRoute(caseId))
                }
            )
        }
        
        // Phase 4: Hospital Case Detail
        composable(
            route = Screen.HospitalCaseDetail.route,
            arguments = listOf(
                navArgument("caseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val caseId = backStackEntry.arguments?.getLong("caseId") ?: 0L
            HospitalCaseDetailScreen(
                caseId = caseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                onNavigateToLanguage = { navController.navigate(Screen.Language.route) },
                onNavigateToJourney = { navController.navigate(Screen.Journey.route) },
                onNavigateToEmergency = { navController.navigate(Screen.EmergencyStart.route) },
                onNavigateToVoiceAssistance = { navController.navigate(Screen.VoiceAssistance.route) }
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
                onNavigateBack = { navController.popBackStack() },
                onViewBrief = {
                    navController.navigate(Screen.PatientBrief.route)
                }
            )
        }
        
        composable(Screen.EmergencyStart.route) {
            EmergencyStartScreen(
                onNavigateBack = { navController.popBackStack() },
                onContinue = { 
                    // After SOS, navigate to Patient Brief (emergency already created with default symptom)
                    navController.navigate(Screen.PatientBrief.route)
                }
            )
        }
        
        composable(Screen.SymptomCapture.route) {
            SymptomCaptureScreen(
                onNavigateBack = { navController.popBackStack() },
                onViewBrief = {
                    navController.navigate(Screen.PatientBrief.route)
                }
            )
        }
        
        composable(Screen.PatientBrief.route) {
            PatientBriefScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Voice Assistance
        composable(Screen.VoiceAssistance.route) {
            VoiceAssistanceScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPatientBrief = {
                    navController.navigate(Screen.PatientBrief.route)
                }
            )
        }
    }
}
