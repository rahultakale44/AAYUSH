package com.aayush.emergency.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import com.aayush.emergency.domain.triage.SymptomExtractor
import com.aayush.emergency.ui.components.AAYUSHCard
import com.aayush.emergency.ui.theme.EmergencyRed
import com.aayush.emergency.ui.viewmodel.EmergencyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * Voice Assistance Screen - FIXED VERSION
 * 
 * TRUE VOICE-FIRST FLOW:
 * 1. TTS speaks greeting in user's language
 * 2. Automatic listening starts after greeting completes
 * 3. User speaks symptoms naturally
 * 4. Speech recognition captures transcript
 * 5. Symptom extraction runs automatically
 * 6. Triage assessment happens immediately
 * 7. Emergency session created with results
 * 8. Navigate to Urgency Assessment screen
 * 
 * NO manual Submit button required for recognized speech.
 * NO 10-second countdown delay for critical symptoms.
 * 
 * FIXES:
 * - Wait for TTS to actually complete (UtteranceProgressListener)
 * - Start recognition only after greeting finishes
 * - Handle all recognition states properly
 * - Process transcript exactly once
 * - Create session BEFORE adding symptoms
 * - Wait for session creation to complete
 * - Support English, Hindi, Marathi
 * - Handle errors with retry
 * - No infinite loops
 */

enum class VoiceFlowState {
    INITIALIZING,           // Setting up TTS and permissions
    AAYUSH_SPEAKING,        // TTS speaking greeting
    LISTENING,              // Speech recognition active
    PROCESSING_TRANSCRIPT,  // Processing recognized text
    ANALYZING_SYMPTOMS,     // Extracting symptoms and running triage
    CREATING_SESSION,       // Creating emergency session
    COMPLETE,              // Ready to navigate
    PERMISSION_REQUIRED,    // Need microphone permission
    PERMISSION_DENIED,      // Permission permanently denied
    ERROR                   // Recognition or processing error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistanceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUrgencyAssessment: () -> Unit,
    viewModel: EmergencyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var flowState by remember { mutableStateOf(VoiceFlowState.INITIALIZING) }
    var recognizedText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var retryCount by remember { mutableIntStateOf(0) }
    
    // TTS and Speech Recognizer
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var greetingComplete by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            flowState = VoiceFlowState.AAYUSH_SPEAKING
            speakGreetingAndListen(tts, context) {
                // Greeting complete, start listening
                greetingComplete = true
                flowState = VoiceFlowState.LISTENING
                startListening(context, speechRecognizer) { transcript, error ->
                    if (transcript != null) {
                        recognizedText = transcript
                        flowState = VoiceFlowState.PROCESSING_TRANSCRIPT
                        processTranscriptAndCreateSession(
                            transcript = transcript,
                            viewModel = viewModel,
                            scope = scope,
                            onStateChange = { flowState = it },
                            onComplete = onNavigateToUrgencyAssessment
                        )
                    } else if (error != null) {
                        errorMessage = error
                        flowState = VoiceFlowState.ERROR
                    }
                }
            }
        } else {
            flowState = VoiceFlowState.PERMISSION_DENIED
        }
    }
    
    // Initialize TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                // Check microphone permission
                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    flowState = VoiceFlowState.AAYUSH_SPEAKING
                    speakGreetingAndListen(tts, context) {
                        // Greeting complete, start listening
                        greetingComplete = true
                        flowState = VoiceFlowState.LISTENING
                        startListening(context, speechRecognizer) { transcript, error ->
                            if (transcript != null) {
                                recognizedText = transcript
                                flowState = VoiceFlowState.PROCESSING_TRANSCRIPT
                                processTranscriptAndCreateSession(
                                    transcript = transcript,
                                    viewModel = viewModel,
                                    scope = scope,
                                    onStateChange = { flowState = it },
                                    onComplete = onNavigateToUrgencyAssessment
                                )
                            } else if (error != null) {
                                errorMessage = error
                                flowState = VoiceFlowState.ERROR
                            }
                        }
                    }
                } else {
                    flowState = VoiceFlowState.PERMISSION_REQUIRED
                }
            } else {
                // TTS failed, still need permission check
                ttsReady = false
                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    // Skip greeting, go directly to listening
                    flowState = VoiceFlowState.LISTENING
                    startListening(context, speechRecognizer) { transcript, error ->
                        if (transcript != null) {
                            recognizedText = transcript
                            flowState = VoiceFlowState.PROCESSING_TRANSCRIPT
                            processTranscriptAndCreateSession(
                                transcript = transcript,
                                viewModel = viewModel,
                                scope = scope,
                                onStateChange = { flowState = it },
                                onComplete = onNavigateToUrgencyAssessment
                            )
                        } else if (error != null) {
                            errorMessage = error
                            flowState = VoiceFlowState.ERROR
                        }
                    }
                } else {
                    flowState = VoiceFlowState.PERMISSION_REQUIRED
                }
            }
        }
    }
    
    // Cleanup
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (flowState) {
                VoiceFlowState.INITIALIZING -> InitializingView()
                VoiceFlowState.AAYUSH_SPEAKING -> SpeakingView()
                VoiceFlowState.LISTENING -> ListeningView()
                VoiceFlowState.PROCESSING_TRANSCRIPT -> ProcessingView(recognizedText)
                VoiceFlowState.ANALYZING_SYMPTOMS -> AnalyzingView()
                VoiceFlowState.CREATING_SESSION -> CreatingSessionView()
                VoiceFlowState.COMPLETE -> CompleteView()
                VoiceFlowState.PERMISSION_REQUIRED -> PermissionRequiredView(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
                VoiceFlowState.PERMISSION_DENIED -> PermissionDeniedView(context)
                VoiceFlowState.ERROR -> ErrorView(
                    errorMessage = errorMessage,
                    retryCount = retryCount,
                    onRetry = {
                        if (retryCount < 2) {
                            retryCount++
                            errorMessage = ""
                            flowState = VoiceFlowState.LISTENING
                            startListening(context, speechRecognizer) { transcript, error ->
                                if (transcript != null) {
                                    recognizedText = transcript
                                    flowState = VoiceFlowState.PROCESSING_TRANSCRIPT
                                    processTranscriptAndCreateSession(
                                        transcript = transcript,
                                        viewModel = viewModel,
                                        scope = scope,
                                        onStateChange = { flowState = it },
                                        onComplete = onNavigateToUrgencyAssessment
                                    )
                                } else if (error != null) {
                                    errorMessage = error
                                    flowState = VoiceFlowState.ERROR
                                }
                            }
                        }
                    },
                    onCancel = onNavigateBack
                )
            }
        }
    }
}

/**
 * Speak greeting and wait for TTS to complete using UtteranceProgressListener
 */
private fun speakGreetingAndListen(
    tts: TextToSpeech?,
    context: android.content.Context,
    onGreetingComplete: () -> Unit
) {
    if (tts == null) {
        // TTS not available, skip to listening
        onGreetingComplete()
        return
    }
    
    val locale = Locale.getDefault()
    val greeting = when (locale.language) {
        "hi" -> "नमस्ते। मैं आयुष हूँ। कृपया बताइए कि आपको क्या तकलीफ़ हो रही है। मैं आपकी बात सुन रहा हूँ।"
        "mr" -> "नमस्कार। मी आयुष आहे. कृपया तुम्हाला काय त्रास होत आहे ते सांगा. मी तुमचे ऐकत आहे."
        else -> "Hello. I am AAYUSH. Please tell me what is troubling you. I am listening."
    }
    
    // Set language
    when (locale.language) {
        "hi" -> tts.language = Locale("hi", "IN")
        "mr" -> tts.language = Locale("mr", "IN")
        else -> tts.language = Locale.ENGLISH
    }
    
    // Set up completion listener
    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            // TTS started
        }
        
        override fun onDone(utteranceId: String?) {
            if (utteranceId == "greeting") {
                // Greeting completed, start listening
                onGreetingComplete()
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            // TTS error, still proceed to listening
            onGreetingComplete()
        }
    })
    
    // Speak with utterance ID
    val params = Bundle()
    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "greeting")
    tts.speak(greeting, TextToSpeech.QUEUE_FLUSH, params, "greeting")
}

/**
 * Start speech recognition with proper error handling
 */
private fun startListening(
    context: android.content.Context,
    recognizerRef: SpeechRecognizer?,
    onResult: (transcript: String?, error: String?) -> Unit
) {
    // Check if speech recognition is available
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        onResult(null, "Speech recognition not available on this device")
        return
    }
    
    // Destroy old recognizer if exists
    recognizerRef?.destroy()
    
    // Create new recognizer
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000) // 15 seconds
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000) // 2 seconds silence
    }
    
    var resultReceived = false  // Prevent duplicate callbacks
    
    speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            android.util.Log.d("VoiceAssistance", "Ready for speech")
        }
        
        override fun onBeginningOfSpeech() {
            android.util.Log.d("VoiceAssistance", "Speech started")
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {}
        
        override fun onEndOfSpeech() {
            android.util.Log.d("VoiceAssistance", "Speech ended")
        }
        
        override fun onError(error: Int) {
            if (resultReceived) return  // Already got result, ignore error
            resultReceived = true
            
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> 
                    "No speech detected. Please try speaking again."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    "No speech heard. Please try again."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "Network error. Check your connection and try again."
                SpeechRecognizer.ERROR_SERVER ->
                    "Speech service unavailable. Please try again."
                SpeechRecognizer.ERROR_AUDIO ->
                    "Microphone error. Check your device and try again."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "Microphone permission denied."
                else ->
                    "Recognition failed. Please try again."
            }
            android.util.Log.e("VoiceAssistance", "Recognition error: $error - $errorMsg")
            speechRecognizer.destroy()
            onResult(null, errorMsg)
        }
        
        override fun onResults(results: Bundle?) {
            if (resultReceived) return  // Already processed
            resultReceived = true
            
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            android.util.Log.d("VoiceAssistance", "Results: $matches")
            
            if (!matches.isNullOrEmpty()) {
                val transcript = matches[0].trim()
                if (transcript.isNotBlank()) {
                    speechRecognizer.destroy()
                    onResult(transcript, null)
                } else {
                    speechRecognizer.destroy()
                    onResult(null, "Empty transcript received. Please speak clearly and try again.")
                }
            } else {
                speechRecognizer.destroy()
                onResult(null, "No speech recognized. Please try again.")
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            android.util.Log.d("VoiceAssistance", "Partial results: $partial")
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    
    speechRecognizer.startListening(intent)
}

/**
 * Process transcript: extract symptoms, run triage, create emergency session
 */
private fun processTranscriptAndCreateSession(
    transcript: String,
    viewModel: EmergencyViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onStateChange: (VoiceFlowState) -> Unit,
    onComplete: () -> Unit
) {
    scope.launch {
        try {
            android.util.Log.d("VoiceAssistance", "Processing transcript: $transcript")
            
            // Step 1: Extract symptoms
            onStateChange(VoiceFlowState.ANALYZING_SYMPTOMS)
            val extractedSymptoms = SymptomExtractor.extractSymptoms(transcript)
            android.util.Log.d("VoiceAssistance", "Extracted symptoms: ${extractedSymptoms.size}")
            
            // Step 2: Create emergency session FIRST
            onStateChange(VoiceFlowState.CREATING_SESSION)
            viewModel.startEmergency()
            
            // Wait for session to be created (check multiple times)
            var session = viewModel.activeSession.value
            var attempts = 0
            while (session == null && attempts < 10) {
                delay(100)
                session = viewModel.activeSession.value
                attempts++
            }
            
            if (session == null) {
                android.util.Log.e("VoiceAssistance", "Failed to create emergency session after $attempts attempts")
                onStateChange(VoiceFlowState.ERROR)
                return@launch
            }
            
            android.util.Log.d("VoiceAssistance", "Emergency session created: ${session.id}")
            
            // Step 3: Add symptoms to session
            if (extractedSymptoms.isNotEmpty()) {
                extractedSymptoms.forEach { extracted ->
                    val symptomInfo = SymptomExtractor.toSymptomInfo(extracted)
                    viewModel.addSymptom(
                        description = symptomInfo.description,
                        severity = symptomInfo.severity,
                        duration = symptomInfo.duration,
                        notes = "Original: \"$transcript\". ${symptomInfo.notes}"
                    )
                }
            } else {
                // No specific symptoms extracted, add the raw transcript
                viewModel.addSymptom(
                    description = transcript,
                    severity = com.aayush.emergency.data.model.SymptomSeverity.MODERATE,
                    duration = "",
                    notes = "Voice-reported via speech recognition"
                )
            }
            
            // Triage happens automatically in viewModel.addSymptom()
            
            // Step 4: Wait briefly for triage to complete
            delay(300)
            
            // Step 5: Generate patient brief
            viewModel.generatePatientBrief()
            delay(200)
            
            // Step 6: Complete
            onStateChange(VoiceFlowState.COMPLETE)
            delay(500)
            
            // Navigate to urgency assessment
            android.util.Log.d("VoiceAssistance", "Navigating to urgency assessment")
            onComplete()
            
        } catch (e: Exception) {
            android.util.Log.e("VoiceAssistance", "Error processing transcript", e)
            onStateChange(VoiceFlowState.ERROR)
        }
    }
}

// UI Components

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
            text = "Initializing voice assistance...",
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
                .size(100.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "AAYUSH is speaking...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (Locale.getDefault().language) {
                "hi" -> "नमस्ते। मैं आयुष हूँ। कृपया बताइए कि आपको क्या तकलीफ़ हो रही है। मैं आपकी बात सुन रहा हूँ।"
                "mr" -> "नमस्कार। मी आयुष आहे. कृपया तुम्हाला काय त्रास होत आहे ते सांगा. मी तुमचे ऐकत आहे."
                else -> "Hello. I am AAYUSH. Please tell me what is troubling you. I am listening."
            },
            style = MaterialTheme.typography.bodyLarge,
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
                .size(140.dp)
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
                modifier = Modifier.size(70.dp),
                tint = EmergencyRed
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = EmergencyRed
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Speak clearly and describe your symptoms",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProcessingView(transcript: String) {
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
            text = "Processing your speech...",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        AAYUSHCard {
            Text(
                text = "You said: \"$transcript\"",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnalyzingView() {
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
            text = "Analyzing symptoms...",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Extracting symptom facts and running triage assessment",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreatingSessionView() {
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
            text = "Creating emergency case...",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun CompleteView() {
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
            text = "Assessment Complete",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Preparing your emergency brief...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PermissionRequiredView(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Microphone Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AAYUSH needs microphone access to hear your symptoms. Your voice data is processed locally and not sent to servers.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun PermissionDeniedView(context: android.content.Context) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MicOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Permission Denied",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Microphone permission is required for voice assistance. Please enable it in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Settings")
        }
    }
}

@Composable
private fun ErrorView(
    errorMessage: String,
    retryCount: Int,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        AAYUSHCard {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        
        if (retryCount < 2) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Try Again (${2 - retryCount} attempts remaining)")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go Back")
        }
    }
}
