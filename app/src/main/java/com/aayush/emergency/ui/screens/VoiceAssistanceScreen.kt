package com.aayush.emergency.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.R
import com.aayush.emergency.data.model.SymptomSeverity
import com.aayush.emergency.ui.components.AAYUSHCard
import com.aayush.emergency.ui.theme.EmergencyRed
import com.aayush.emergency.ui.viewmodel.EmergencyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * Voice Assistance Screen
 * 
 * Features:
 * - Text-to-Speech greeting in user's language
 * - Speech-to-Text for symptom capture
 * - 10-second cancellation window with countdown
 * - Fallback to text input
 * - Runtime microphone permission handling
 * - Safety warnings displayed immediately for critical symptoms
 */

enum class VoiceState {
    INITIALIZING,           // Setting up TTS and checking permissions
    AAYUSH_SPEAKING,        // TTS is speaking the greeting
    READY_TO_LISTEN,        // Ready for user to tap microphone
    LISTENING,              // Actively recording speech
    PROCESSING,             // Processing speech recognition results
    REVIEW_COUNTDOWN,       // Showing transcript with 10-second countdown
    SUBMITTING,             // Submitting to emergency workflow
    SUBMITTED,              // Successfully submitted
    CANCELLED,              // User cancelled
    PERMISSION_REQUIRED,    // Needs microphone permission
    PERMISSION_DENIED,      // Permission denied
    ERROR                   // Recognition or TTS error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistanceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPatientBrief: () -> Unit,
    viewModel: EmergencyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State management
    var voiceState by remember { mutableStateOf(VoiceState.INITIALIZING) }
    var recognizedText by remember { mutableStateOf("") }
    var manualText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(10) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }
    var hasSpoken by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceState = VoiceState.READY_TO_LISTEN
        } else {
            voiceState = VoiceState.PERMISSION_DENIED
        }
    }
    
    // Initialize TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Check microphone permission
                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    voiceState = VoiceState.AAYUSH_SPEAKING
                    speakGreeting(tts, context) {
                        // Automatically start listening after greeting
                        voiceState = VoiceState.LISTENING
                        startSpeechRecognition(
                            context = context,
                            onResult = { text ->
                                recognizedText = text
                                countdown = 10
                                voiceState = VoiceState.REVIEW_COUNTDOWN
                            },
                            onError = { error ->
                                errorMessage = error
                                voiceState = VoiceState.ERROR
                            },
                            onStateChange = { state ->
                                voiceState = state
                            },
                            speechRecognizerRef = { sr -> speechRecognizer = sr }
                        )
                    }
                } else {
                    voiceState = VoiceState.PERMISSION_REQUIRED
                }
            } else {
                // TTS unavailable, go directly to permission check
                errorMessage = context.getString(R.string.voice_tts_unavailable)
                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    // Skip greeting, start listening immediately
                    voiceState = VoiceState.LISTENING
                    startSpeechRecognition(
                        context = context,
                        onResult = { text ->
                            recognizedText = text
                            countdown = 10
                            voiceState = VoiceState.REVIEW_COUNTDOWN
                        },
                        onError = { error ->
                            errorMessage = error
                            voiceState = VoiceState.ERROR
                        },
                        onStateChange = { state ->
                            voiceState = state
                        },
                        speechRecognizerRef = { sr -> speechRecognizer = sr }
                    )
                } else {
                    voiceState = VoiceState.PERMISSION_REQUIRED
                }
            }
        }
    }
    
    // Countdown timer effect
    LaunchedEffect(voiceState, countdown) {
        if (voiceState == VoiceState.REVIEW_COUNTDOWN && countdown > 0) {
            delay(1000)
            countdown--
        } else if (voiceState == VoiceState.REVIEW_COUNTDOWN && countdown == 0) {
            // Auto-submit after countdown
            submitEmergency(
                text = recognizedText,
                viewModel = viewModel,
                onStateChange = { voiceState = it },
                onNavigateToPatientBrief = onNavigateToPatientBrief
            )
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_assistance_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        // Cancel any pending submission
                        if (voiceState == VoiceState.REVIEW_COUNTDOWN) {
                            voiceState = VoiceState.CANCELLED
                        }
                        tts?.stop()
                        speechRecognizer?.cancel()
                        onNavigateBack()
                    }) {
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (voiceState) {
                VoiceState.INITIALIZING -> {
                    InitializingView()
                }
                VoiceState.AAYUSH_SPEAKING -> {
                    SpeakingView()
                }
                VoiceState.READY_TO_LISTEN -> {
                    ReadyToListenView(
                        onStartListening = {
                            startSpeechRecognition(
                                context = context,
                                onResult = { text ->
                                    recognizedText = text
                                    countdown = 10
                                    voiceState = VoiceState.REVIEW_COUNTDOWN
                                },
                                onError = { error ->
                                    errorMessage = error
                                    voiceState = VoiceState.ERROR
                                },
                                onStateChange = { state ->
                                    voiceState = state
                                },
                                speechRecognizerRef = { sr -> speechRecognizer = sr }
                            )
                        },
                        manualText = manualText,
                        onManualTextChange = { manualText = it },
                        onManualSubmit = {
                            if (manualText.isNotBlank()) {
                                recognizedText = manualText
                                countdown = 10
                                voiceState = VoiceState.REVIEW_COUNTDOWN
                            }
                        }
                    )
                }
                VoiceState.LISTENING -> {
                    ListeningView()
                }
                VoiceState.PROCESSING -> {
                    ProcessingView()
                }
                VoiceState.REVIEW_COUNTDOWN -> {
                    ReviewCountdownView(
                        recognizedText = recognizedText,
                        countdown = countdown,
                        onCancel = {
                            voiceState = VoiceState.CANCELLED
                            recognizedText = ""
                        },
                        onSpeakAgain = {
                            recognizedText = ""
                            voiceState = VoiceState.READY_TO_LISTEN
                        },
                        onSubmitNow = {
                            submitEmergency(
                                text = recognizedText,
                                viewModel = viewModel,
                                onStateChange = { voiceState = it },
                                onNavigateToPatientBrief = onNavigateToPatientBrief
                            )
                        }
                    )
                }
                VoiceState.SUBMITTING -> {
                    SubmittingView()
                }
                VoiceState.SUBMITTED -> {
                    SubmittedView(onNavigateToPatientBrief)
                }
                VoiceState.CANCELLED -> {
                    CancelledView(onNavigateBack)
                }
                VoiceState.PERMISSION_REQUIRED -> {
                    PermissionRequiredView(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        manualText = manualText,
                        onManualTextChange = { manualText = it },
                        onManualSubmit = {
                            if (manualText.isNotBlank()) {
                                recognizedText = manualText
                                countdown = 10
                                voiceState = VoiceState.REVIEW_COUNTDOWN
                            }
                        }
                    )
                }
                VoiceState.PERMISSION_DENIED -> {
                    PermissionDeniedView(
                        context = context,
                        manualText = manualText,
                        onManualTextChange = { manualText = it },
                        onManualSubmit = {
                            if (manualText.isNotBlank()) {
                                recognizedText = manualText
                                countdown = 10
                                voiceState = VoiceState.REVIEW_COUNTDOWN
                            }
                        }
                    )
                }
                VoiceState.ERROR -> {
                    ErrorView(
                        errorMessage = errorMessage,
                        onRetry = {
                            errorMessage = ""
                            voiceState = VoiceState.READY_TO_LISTEN
                        },
                        manualText = manualText,
                        onManualTextChange = { manualText = it },
                        onManualSubmit = {
                            if (manualText.isNotBlank()) {
                                recognizedText = manualText
                                countdown = 10
                                voiceState = VoiceState.REVIEW_COUNTDOWN
                            }
                        }
                    )
                }
            }
        }
    }
}

// Helper function to speak greeting
private fun speakGreeting(
    tts: TextToSpeech?,
    context: android.content.Context,
    onComplete: () -> Unit
) {
    val locale = Locale.getDefault()
    val greeting = when (locale.language) {
        "hi" -> context.getString(R.string.voice_greeting_hi)
        "mr" -> context.getString(R.string.voice_greeting_mr)
        else -> context.getString(R.string.voice_greeting_en)
    }
    
    // Set language if available
    when (locale.language) {
        "hi" -> tts?.language = Locale("hi", "IN")
        "mr" -> tts?.language = Locale("mr", "IN")
        else -> tts?.language = Locale.ENGLISH
    }
    
    tts?.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "greeting")
    
    // Wait for speech to complete
    kotlinx.coroutines.GlobalScope.launch {
        delay(greeting.length * 50L) // Rough estimate
        onComplete()
    }
}

// Helper function to start speech recognition
private fun startSpeechRecognition(
    context: android.content.Context,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onStateChange: (VoiceState) -> Unit,
    speechRecognizerRef: (SpeechRecognizer) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        onError(context.getString(R.string.voice_error_service_unavailable))
        return
    }
    
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    speechRecognizerRef(speechRecognizer)
    
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    
    speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onStateChange(VoiceState.LISTENING)
        }
        
        override fun onBeginningOfSpeech() {
            onStateChange(VoiceState.LISTENING)
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Could use for visual feedback
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {}
        
        override fun onEndOfSpeech() {
            onStateChange(VoiceState.PROCESSING)
        }
        
        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> 
                    context.getString(R.string.voice_error_no_speech)
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    context.getString(R.string.voice_error_network)
                SpeechRecognizer.ERROR_SERVER ->
                    context.getString(R.string.voice_error_service_unavailable)
                else ->
                    context.getString(R.string.voice_error_recognition_failed)
            }
            onError(errorMsg)
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                if (text.isNotBlank()) {
                    onResult(text)
                } else {
                    onError(context.getString(R.string.voice_empty_transcript))
                }
            } else {
                onError(context.getString(R.string.voice_empty_transcript))
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {}
        
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    
    speechRecognizer.startListening(intent)
}

// Helper function to submit emergency
private fun submitEmergency(
    text: String,
    viewModel: EmergencyViewModel,
    onStateChange: (VoiceState) -> Unit,
    onNavigateToPatientBrief: () -> Unit
) {
    if (text.isBlank()) return
    
    onStateChange(VoiceState.SUBMITTING)
    
    // Start emergency session if not already started
    val session = viewModel.activeSession.value
    if (session == null) {
        viewModel.startEmergency()
    }
    
    // Add symptom with speech-recognized text
    viewModel.addSymptom(
        description = text,
        severity = SymptomSeverity.MODERATE, // Default, will be assessed by triage
        duration = "",
        notes = "Voice-reported via speech recognition"
    )
    
    // Generate patient brief
    viewModel.generatePatientBrief()
    
    // Navigate to patient brief
    kotlinx.coroutines.GlobalScope.launch {
        delay(1000) // Brief delay for submission
        onStateChange(VoiceState.SUBMITTED)
        delay(500)
        onNavigateToPatientBrief()
    }
}

// UI Components for each state

@Composable
private fun InitializingView() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.voice_initializing),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun SpeakingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "speaking")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
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
        Icon(
            imageVector = Icons.Default.RecordVoiceOver,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.voice_aayush_speaking),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (Locale.getDefault().language) {
                "hi" -> stringResource(R.string.voice_greeting_hi)
                "mr" -> stringResource(R.string.voice_greeting_mr)
                else -> stringResource(R.string.voice_greeting_en)
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadyToListenView(
    onStartListening: () -> Unit,
    manualText: String,
    onManualTextChange: (String) -> Unit,
    onManualSubmit: () -> Unit
) {
    // Auto-start listening when this view appears
    LaunchedEffect(Unit) {
        delay(500) // Brief delay to show state
        onStartListening()
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.voice_ready_to_listen),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Microphone button
        FilledIconButton(
            onClick = onStartListening,
            modifier = Modifier.size(120.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(R.string.voice_tap_to_speak),
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.voice_tap_to_speak),
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Manual text entry fallback
        AAYUSHCard {
            Column {
                Text(
                    text = stringResource(R.string.voice_or_type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = manualText,
                    onValueChange = onManualTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.voice_symptom_hint)) },
                    minLines = 3,
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onManualSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = manualText.isNotBlank()
                ) {
                    Text(stringResource(R.string.button_continue))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.voice_help_text),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ListeningView() {
    val infiniteTransition = rememberInfiniteTransition(label = "listening")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(
                    color = EmergencyRed.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = EmergencyRed
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.voice_listening),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = EmergencyRed
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Speak clearly and describe your symptoms",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProcessingView() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.voice_processing),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun ReviewCountdownView(
    recognizedText: String,
    countdown: Int,
    onCancel: () -> Unit,
    onSpeakAgain: () -> Unit,
    onSubmitNow: () -> Unit
) {
    // Check for critical symptoms and show warning immediately
    val isCritical = recognizedText.contains("chest pain", ignoreCase = true) ||
            recognizedText.contains("difficulty breathing", ignoreCase = true) ||
            recognizedText.contains("unconscious", ignoreCase = true) ||
            recognizedText.contains("severe bleeding", ignoreCase = true)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Critical warning (if applicable) - shown immediately, not waiting for countdown
        if (isCritical) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EmergencyRed,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ CALL 112 IMMEDIATELY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "If this is a real emergency, dial India's emergency number 112 now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            text = stringResource(R.string.voice_i_heard),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Recognized text
        AAYUSHCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = recognizedText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Countdown
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = if (countdown <= 3) EmergencyRed.copy(alpha = 0.2f) 
                   else MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdown.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (countdown <= 3) EmergencyRed 
                            else MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.voice_countdown_submitting, countdown),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button (most prominent for safety)
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed
                )
            ) {
                Icon(Icons.Default.Cancel, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.voice_cancel_emergency),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSpeakAgain,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Mic, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.voice_speak_again))
                }
                
                Button(
                    onClick = onSubmitNow,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(stringResource(R.string.voice_submit_now))
                }
            }
        }
    }
}

@Composable
private fun SubmittingView() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.voice_submitting),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun SubmittedView(onNavigateToPatientBrief: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.voice_emergency_submitted),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateToPatientBrief,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Patient Brief")
        }
    }
}

@Composable
private fun CancelledView(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.voice_cancelled),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Return to Home")
        }
    }
}

@Composable
private fun PermissionRequiredView(
    onRequestPermission: () -> Unit,
    manualText: String,
    onManualTextChange: (String) -> Unit,
    onManualSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.voice_permission_required),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.voice_permission_explanation),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.voice_grant_permission))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Manual text entry
        AAYUSHCard {
            Column {
                Text(
                    text = stringResource(R.string.voice_or_type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = manualText,
                    onValueChange = onManualTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.voice_symptom_hint)) },
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onManualSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = manualText.isNotBlank()
                ) {
                    Text(stringResource(R.string.button_continue))
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedView(
    context: android.content.Context,
    manualText: String,
    onManualTextChange: (String) -> Unit,
    onManualSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MicOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.voice_permission_denied),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.voice_permission_denied_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.voice_go_to_settings))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Manual text entry
        AAYUSHCard {
            Column {
                Text(
                    text = stringResource(R.string.voice_or_type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = manualText,
                    onValueChange = onManualTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.voice_symptom_hint)) },
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onManualSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = manualText.isNotBlank()
                ) {
                    Text(stringResource(R.string.button_continue))
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    manualText: String,
    onManualTextChange: (String) -> Unit,
    onManualSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try Again")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Manual text entry
        AAYUSHCard {
            Column {
                Text(
                    text = stringResource(R.string.voice_or_type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = manualText,
                    onValueChange = onManualTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.voice_symptom_hint)) },
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onManualSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = manualText.isNotBlank()
                ) {
                    Text(stringResource(R.string.button_continue))
                }
            }
        }
    }
}
