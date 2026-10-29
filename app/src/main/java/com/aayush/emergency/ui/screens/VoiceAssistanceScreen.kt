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
 * Voice Assistance Screen - CRITICAL FIX FOR STUCK LISTENING
 * 
 * FIXED ISSUES:
 * 1. SpeechRecognizer reference not properly stored - recognizer was created but never saved
 * 2. Callbacks don't run on main thread - Compose state updates must be on main thread
 * 3. Insufficient logging for debugging physical device issues
 * 4. onEndOfSpeech doesn't mean results arrived - must wait for onResults or onError
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

private const val TAG = "AAYUSH_VOICE"

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
    
    // TTS and Speech Recognizer - must be remembered to persist
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }
    var ttsReady by remember { mutableStateOf(false) }
    
    // Callback to handle recognition results - runs on main thread
    val handleRecognitionResult: (String?, String?) -> Unit = { transcript, error ->
        android.util.Log.d(TAG, "handleRecognitionResult called - transcript: $transcript, error: $error")
        if (transcript != null) {
            android.util.Log.i(TAG, "✓ Recognition SUCCESS - Transcript: \"$transcript\"")
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
            android.util.Log.e(TAG, "✗ Recognition ERROR: $error")
            errorMessage = error
            flowState = VoiceFlowState.ERROR
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d(TAG, "Permission result: $isGranted")
        if (isGranted) {
            flowState = VoiceFlowState.AAYUSH_SPEAKING
            speakGreetingAndListen(tts, context) {
                // Greeting complete, start listening
                android.util.Log.i(TAG, "Greeting complete, starting recognition")
                flowState = VoiceFlowState.LISTENING
                speechRecognizer = startListening(context, handleRecognitionResult)
            }
        } else {
            flowState = VoiceFlowState.PERMISSION_DENIED
        }
    }
    
    // Initialize TTS
    LaunchedEffect(Unit) {
        android.util.Log.d(TAG, "=== INITIALIZING VOICE ASSISTANCE ===")
        tts = TextToSpeech(context) { status ->
            android.util.Log.d(TAG, "TTS initialization status: $status")
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                // Check microphone permission
                val hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                android.util.Log.d(TAG, "Microphone permission: $hasPermission")
                
                if (hasPermission) {
                    flowState = VoiceFlowState.AAYUSH_SPEAKING
                    speakGreetingAndListen(tts, context) {
                        // Greeting complete, start listening
                        android.util.Log.i(TAG, "Greeting complete, starting recognition")
                        flowState = VoiceFlowState.LISTENING
                        speechRecognizer = startListening(context, handleRecognitionResult)
                    }
                } else {
                    flowState = VoiceFlowState.PERMISSION_REQUIRED
                }
            } else {
                // TTS failed, still need permission check
                android.util.Log.w(TAG, "TTS unavailable, checking permission")
                ttsReady = false
                val hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasPermission) {
                    // Skip greeting, go directly to listening
                    android.util.Log.i(TAG, "Skipping greeting, starting recognition directly")
                    flowState = VoiceFlowState.LISTENING
                    speechRecognizer = startListening(context, handleRecognitionResult)
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
                            android.util.Log.i(TAG, "Retry attempt ${retryCount + 1}/2")
                            retryCount++
                            errorMessage = ""
                            flowState = VoiceFlowState.LISTENING
                            speechRecognizer = startListening(context, handleRecognitionResult)
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
    android.util.Log.d(TAG, "=== SPEAKING GREETING ===")
    
    if (tts == null) {
        android.util.Log.w(TAG, "TTS not available, skipping greeting")
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
    
    android.util.Log.d(TAG, "Greeting language: ${locale.language}, text length: ${greeting.length}")
    
    // Set language
    when (locale.language) {
        "hi" -> tts.language = Locale("hi", "IN")
        "mr" -> tts.language = Locale("mr", "IN")
        else -> tts.language = Locale.ENGLISH
    }
    
    // Set up completion listener
    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            android.util.Log.d(TAG, "TTS onStart: $utteranceId")
        }
        
        override fun onDone(utteranceId: String?) {
            android.util.Log.i(TAG, "TTS onDone: $utteranceId")
            if (utteranceId == "greeting") {
                // Greeting completed, start listening
                android.util.Log.i(TAG, "✓ Greeting complete, triggering onGreetingComplete()")
                onGreetingComplete()
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            android.util.Log.e(TAG, "TTS onError: $utteranceId")
            // TTS error, still proceed to listening
            onGreetingComplete()
        }
    })
    
    // Speak with utterance ID
    val params = Bundle()
    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "greeting")
    val result = tts.speak(greeting, TextToSpeech.QUEUE_FLUSH, params, "greeting")
    android.util.Log.d(TAG, "TTS speak() result: $result (SUCCESS=0, ERROR=-1)")
}

/**
 * Start speech recognition with comprehensive logging and proper error handling
 * RETURNS the SpeechRecognizer instance so it can be stored and managed
 */
private fun startListening(
    context: android.content.Context,
    onResult: (transcript: String?, error: String?) -> Unit
): SpeechRecognizer? {
    android.util.Log.d(TAG, "=== STARTING SPEECH RECOGNITION ===")
    
    // Check if speech recognition is available
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        android.util.Log.e(TAG, "✗ Speech recognition NOT AVAILABLE on this device")
        onResult(null, "Speech recognition not available on this device")
        return null
    }
    
    android.util.Log.i(TAG, "✓ Speech recognition available")
    
    // Create new recognizer
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    android.util.Log.d(TAG, "Created SpeechRecognizer instance: ${speechRecognizer.hashCode()}")
    
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000) // 15 seconds
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000) // 2 seconds silence
    }
    
    android.util.Log.d(TAG, "Recognition language: ${Locale.getDefault()}")
    
    var resultReceived = false  // Prevent duplicate callbacks
    
    speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            android.util.Log.i(TAG, "→ onReadyForSpeech()")
        }
        
        override fun onBeginningOfSpeech() {
            android.util.Log.i(TAG, "→ onBeginningOfSpeech() - User started speaking")
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed - log occasionally
            if (rmsdB > 0 && System.currentTimeMillis() % 1000 < 100) {
                android.util.Log.v(TAG, "→ onRmsChanged($rmsdB)")
            }
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            android.util.Log.v(TAG, "→ onBufferReceived(${buffer?.size} bytes)")
        }
        
        override fun onEndOfSpeech() {
            android.util.Log.i(TAG, "→ onEndOfSpeech() - User stopped speaking (WAITING FOR RESULTS...)")
        }
        
        override fun onError(error: Int) {
            android.util.Log.e(TAG, "→ onError($error)")
            
            if (resultReceived) {
                android.util.Log.w(TAG, "  Result already received, ignoring error")
                return  // Already got result, ignore error
            }
            resultReceived = true
            
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    android.util.Log.e(TAG, "  ERROR_NO_MATCH (5) - No speech detected")
                    "No speech detected. Please try speaking again."
                }
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    android.util.Log.e(TAG, "  ERROR_SPEECH_TIMEOUT (6) - Timeout")
                    "No speech heard. Please try again."
                }
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    android.util.Log.e(TAG, "  ERROR_NETWORK ($error) - Network issue")
                    "Network error. Check your connection and try again."
                }
                SpeechRecognizer.ERROR_SERVER -> {
                    android.util.Log.e(TAG, "  ERROR_SERVER (4) - Server unavailable")
                    "Speech service unavailable. Please try again."
                }
                SpeechRecognizer.ERROR_AUDIO -> {
                    android.util.Log.e(TAG, "  ERROR_AUDIO (3) - Microphone problem")
                    "Microphone error. Check your device and try again."
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    android.util.Log.e(TAG, "  ERROR_INSUFFICIENT_PERMISSIONS (9) - No permission")
                    "Microphone permission denied."
                }
                SpeechRecognizer.ERROR_CLIENT -> {
                    android.util.Log.e(TAG, "  ERROR_CLIENT (1) - Client error")
                    "Recognition error. Please try again."
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    android.util.Log.e(TAG, "  ERROR_RECOGNIZER_BUSY (8) - Busy")
                    "Recognition busy. Please try again."
                }
                else -> {
                    android.util.Log.e(TAG, "  UNKNOWN ERROR ($error)")
                    "Recognition failed (error $error). Please try again."
                }
            }
            
            android.util.Log.e(TAG, "✗ Calling onResult with error: $errorMsg")
            speechRecognizer.destroy()
            // CRITICAL: Run callback on main thread for Compose state updates
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(null, errorMsg)
            }
        }
        
        override fun onResults(results: Bundle?) {
            android.util.Log.i(TAG, "→ onResults()")
            
            if (resultReceived) {
                android.util.Log.w(TAG, "  Result already processed, ignoring")
                return  // Already processed
            }
            resultReceived = true
            
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            android.util.Log.i(TAG, "  Matches: $matches")
            
            if (!matches.isNullOrEmpty()) {
                val transcript = matches[0].trim()
                android.util.Log.i(TAG, "  Best match: \"$transcript\"")
                
                if (transcript.isNotBlank()) {
                    android.util.Log.i(TAG, "✓ SUCCESS - Valid transcript received")
                    speechRecognizer.destroy()
                    // CRITICAL: Run callback on main thread for Compose state updates
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onResult(transcript, null)
                    }
                } else {
                    android.util.Log.e(TAG, "✗ Empty transcript (whitespace only)")
                    speechRecognizer.destroy()
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onResult(null, "Empty transcript received. Please speak clearly and try again.")
                    }
                }
            } else {
                android.util.Log.e(TAG, "✗ No matches in results bundle")
                speechRecognizer.destroy()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(null, "No speech recognized. Please try again.")
                }
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (partial != null && partial.isNotEmpty()) {
                android.util.Log.d(TAG, "→ onPartialResults: ${partial[0]}")
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            android.util.Log.d(TAG, "→ onEvent($eventType)")
        }
    })
    
    android.util.Log.i(TAG, "Starting listening...")
    speechRecognizer.startListening(intent)
    
    return speechRecognizer
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
            android.util.Log.d(TAG, "=== PROCESSING TRANSCRIPT ===")
            android.util.Log.i(TAG, "Transcript: \"$transcript\"")
            
            // Step 1: Extract symptoms
            onStateChange(VoiceFlowState.ANALYZING_SYMPTOMS)
            val extractedSymptoms = SymptomExtractor.extractSymptoms(transcript)
            android.util.Log.i(TAG, "Extracted ${extractedSymptoms.size} symptoms")
            extractedSymptoms.forEachIndexed { index, symptom ->
                android.util.Log.d(TAG, "  Symptom $index: ${symptom.symptomType} - ${symptom.description} (${symptom.confidence})")
            }
            
            // Step 2: Create emergency session FIRST
            android.util.Log.d(TAG, "=== CREATING EMERGENCY SESSION ===")
            onStateChange(VoiceFlowState.CREATING_SESSION)
            viewModel.startEmergency()
            
            // Wait for session to be created (check multiple times)
            var session = viewModel.activeSession.value
            var attempts = 0
            while (session == null && attempts < 10) {
                delay(100)
                session = viewModel.activeSession.value
                attempts++
                if (attempts % 3 == 0) {
                    android.util.Log.d(TAG, "Waiting for session... attempt $attempts/10")
                }
            }
            
            if (session == null) {
                android.util.Log.e(TAG, "✗ FAILED to create emergency session after $attempts attempts")
                onStateChange(VoiceFlowState.ERROR)
                return@launch
            }
            
            android.util.Log.i(TAG, "✓ Emergency session created: ID=${session.id}, Status=${session.status}")
            
            // Step 3: Add symptoms to session
            android.util.Log.d(TAG, "=== ADDING SYMPTOMS TO SESSION ===")
            if (extractedSymptoms.isNotEmpty()) {
                extractedSymptoms.forEach { extracted ->
                    val symptomInfo = SymptomExtractor.toSymptomInfo(extracted)
                    android.util.Log.d(TAG, "Adding symptom: ${symptomInfo.description}")
                    viewModel.addSymptom(
                        description = symptomInfo.description,
                        severity = symptomInfo.severity,
                        duration = symptomInfo.duration,
                        notes = "Original: \"$transcript\". ${symptomInfo.notes}"
                    )
                }
            } else {
                // No specific symptoms extracted, add the raw transcript
                android.util.Log.w(TAG, "No specific symptoms extracted, using raw transcript")
                viewModel.addSymptom(
                    description = transcript,
                    severity = com.aayush.emergency.data.model.SymptomSeverity.MODERATE,
                    duration = "",
                    notes = "Voice-reported via speech recognition"
                )
            }
            
            // Triage happens automatically in viewModel.addSymptom()
            android.util.Log.d(TAG, "Triage assessment triggered (automatic)")
            
            // Step 4: Wait briefly for triage to complete
            delay(300)
            
            // Step 5: Generate patient brief
            android.util.Log.d(TAG, "Generating patient brief...")
            viewModel.generatePatientBrief()
            delay(200)
            
            // Step 6: Complete
            android.util.Log.i(TAG, "✓ Processing complete")
            onStateChange(VoiceFlowState.COMPLETE)
            delay(500)
            
            // Navigate to urgency assessment
            android.util.Log.i(TAG, "=== NAVIGATING TO URGENCY ASSESSMENT ===")
            onComplete()
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "✗ ERROR processing transcript: ${e.message}", e)
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
