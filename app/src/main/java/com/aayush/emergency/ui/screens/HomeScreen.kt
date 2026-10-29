package com.aayush.emergency.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.R
import com.aayush.emergency.ui.theme.EmergencyRed
import com.aayush.emergency.ui.viewmodel.AuthViewModel

/**
 * Main home screen for AAYUSH
 * Senior-first design with large, clear controls
 */
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToJourney: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToVoiceAssistance: () -> Unit,
    onNavigateToDiagnostic: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Logout button at top right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = stringResource(R.string.auth_logout),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // App branding
            AppHeader()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // SOS Emergency button - most prominent
            SOSButton(onClick = onNavigateToEmergency)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Voice assistance
            VoiceAssistanceButton(onClick = onNavigateToVoiceAssistance)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Diagnostic button (for testing speech recognition)
            DiagnosticButton(onClick = onNavigateToDiagnostic)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Quick access cards
            QuickAccessSection(
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToLanguage = onNavigateToLanguage,
                onNavigateToJourney = onNavigateToJourney
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Prototype notice
            PrototypeNotice()
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppHeader() {
    val logoContentDesc = stringResource(R.string.cd_app_logo)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // AAYUSH branding with Devanagari
        Text(
            text = stringResource(R.string.app_name_devanagari),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                contentDescription = logoContentDesc
            }
        )
        
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SOSButton(onClick: () -> Unit) {
    val contentDesc = stringResource(R.string.cd_sos_button)
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .semantics { contentDescription = contentDesc },
        colors = ButtonDefaults.buttonColors(
            containerColor = EmergencyRed
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .padding(end = 12.dp)
        )
        Text(
            text = stringResource(R.string.home_sos_button),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VoiceAssistanceButton(onClick: () -> Unit) {
    val contentDesc = stringResource(R.string.cd_voice_button)
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { contentDescription = contentDesc },
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .padding(end = 12.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.home_voice_button),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun QuickAccessSection(
    onNavigateToProfile: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToJourney: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAccessCard(
                title = stringResource(R.string.home_profile_button),
                icon = Icons.Default.Person,
                onClick = onNavigateToProfile,
                modifier = Modifier.weight(1f)
            )
            
            QuickAccessCard(
                title = stringResource(R.string.home_contacts_button),
                icon = Icons.Default.ContactEmergency,
                onClick = onNavigateToContacts,
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAccessCard(
                title = stringResource(R.string.home_language_button),
                icon = Icons.Default.Language,
                onClick = onNavigateToLanguage,
                modifier = Modifier.weight(1f)
            )
            
            QuickAccessCard(
                title = stringResource(R.string.home_journey_button),
                icon = Icons.Default.LocalHospital,
                onClick = onNavigateToJourney,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PrototypeNotice() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MedicalServices,
                contentDescription = stringResource(R.string.cd_warning_icon),
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.prototype_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiagnosticButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
        )
    ) {
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp),
            tint = Color(0xFFFF9800)
        )
        Text(
            text = "🔧 Voice Diagnostic (Debug)",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFFF9800)
        )
    }
}
