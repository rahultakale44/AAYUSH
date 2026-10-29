package com.aayush.emergency.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.R
import com.aayush.emergency.data.model.UrgencyLevel
import com.aayush.emergency.ui.components.AAYUSHCard
import com.aayush.emergency.ui.theme.EmergencyRed
import com.aayush.emergency.ui.theme.SafeGreen
import com.aayush.emergency.ui.theme.WarningAmber
import com.aayush.emergency.ui.viewmodel.EmergencyViewModel
import kotlinx.coroutines.delay

/**
 * Urgency Assessment Screen
 * 
 * Displays the triage result with appropriate urgency level:
 * - RED (IMMEDIATE): Critical red flags detected - Call 112
 * - ORANGE (URGENT): Needs prompt assessment
 * - GREEN (LOW/MODERATE): No critical flags detected
 * 
 * Shows:
 * - Large status symbol with animation
 * - Urgency level and explanation
 * - Recognized symptoms
 * - Clear guidance
 * - AAYUSH principle statement
 * 
 * IMPORTANT: Color is determined by actual assessment, not decorative.
 * "आयुष.ai extracts symptom facts and clinician-reviewed red-flag rules set the urgency. No diagnosis."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgencyAssessmentScreen(
    onNavigateToPatientBrief: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EmergencyViewModel = viewModel()
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val triageResult = activeSession?.triageResult
    
    // Auto-advance after a delay for non-critical cases
    val urgency = triageResult?.urgencyLevel ?: UrgencyLevel.UNKNOWN
    LaunchedEffect(urgency) {
        if (urgency != UrgencyLevel.IMMEDIATE && urgency != UrgencyLevel.UNKNOWN) {
            delay(5000) // 5 seconds for non-critical
            onNavigateToPatientBrief()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Urgency Assessment") },
                navigationIcon = {
                    if (urgency != UrgencyLevel.IMMEDIATE) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status indicator based on urgency
            when (urgency) {
                UrgencyLevel.IMMEDIATE -> ImmediateUrgencyView(triageResult)
                UrgencyLevel.URGENT -> UrgentView(triageResult)
                UrgencyLevel.MODERATE, UrgencyLevel.LOW -> LowToModerateView(triageResult)
                UrgencyLevel.UNKNOWN -> UnknownView()
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recognized symptoms
            val session = activeSession
            if (session != null && session.symptoms.isNotEmpty()) {
                AAYUSHCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Recognized Symptoms:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        session.symptoms.forEach { symptom ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = symptom.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // AAYUSH principle statement
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "How AAYUSH Works:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "आयुष.ai extracts symptom facts and clinician-reviewed red-flag rules set the urgency. This is NOT a diagnosis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AAYUSH is a prototype demonstration. For real emergencies, always call 112.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Continue button
            if (urgency != UrgencyLevel.IMMEDIATE) {
                Button(
                    onClick = onNavigateToPatientBrief,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Continue to Patient Brief")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ImmediateUrgencyView(triageResult: com.aayush.emergency.data.model.TriageResult?) {
    val infiniteTransition = rememberInfiniteTransition(label = "critical")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large red circle with warning icon
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .background(EmergencyRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "⚠️ IMMEDIATE EMERGENCY ⚠️",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = EmergencyRed,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = EmergencyRed,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CALL 112 NOW",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = triageResult?.guidance ?: "This is a medical emergency. Call India's emergency number 112 immediately.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        if (triageResult?.redFlagsDetected?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(16.dp))
            AAYUSHCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Critical Red Flags Detected:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    triageResult.redFlagsDetected.forEach { flag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = EmergencyRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = flag,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgentView(triageResult: com.aayush.emergency.data.model.TriageResult?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Orange circle with priority icon
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(WarningAmber, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PriorityHigh,
                contentDescription = null,
                modifier = Modifier.size(70.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "URGENT ASSESSMENT NEEDED",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = WarningAmber,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AAYUSHCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = triageResult?.guidance ?: "Seek immediate medical attention. Visit the nearest hospital emergency department or call 112 for guidance.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LowToModerateView(triageResult: com.aayush.emergency.data.model.TriageResult?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Green circle with check icon
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(SafeGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(70.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Critical Red Flags Detected",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SafeGreen,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AAYUSHCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = triageResult?.guidance ?: "No immediate life-threatening symptoms detected by our rules. However, if you feel your condition is urgent, seek medical care.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This assessment is based on limited information. Trust your judgment and seek help if needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun UnknownView() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Help,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Unable to Assess",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AAYUSHCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "We couldn't assess the urgency level. If you are experiencing a medical emergency, call 112 immediately.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
