package com.aayush.emergency.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aayush.emergency.ui.theme.EmergencyRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * DIAGNOSTIC ONLY - Isolated SpeechRecognizer Test
 * 
 * Purpose: Determine if Android SpeechRecognizer is working at all
 * Does NOT use: TTS, countdown, symptom extraction, emergency session, triage, or navigation
 * 
 * This screen ONLY tests:
 * 1. Can we create a SpeechRecognizer?
 * 2. Does onReadyForSpeech fire?
 * 3. Does onBeginningOfSpeech fire when user speaks?
 * 4. Does onResults or onError eventually fire?
 * 5. What is the actual transcript or error code?
 */

private const val DIAG_TAG = "AAYUSH_VOICE_DIAG"

enum class DiagnosticState {
    INIT,
    PERMISSION_REQUIRED,
    READY,
    LISTENING,
    RESULT_SUCCESS,
    RESULT_ERROR,
    TIMEOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticVoiceScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var state by remember { mutableStateOf(DiagnosticState.INIT) }
    var logMessages by remember { mutableStateOf(listOf<String>()) }
    var recognizer: SpeechRecognizer? by remember { mutableStateOf(null) }
    var transcript by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var partialText by remember { mutableStateOf("") }
    var timeoutJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $message"
        Log.d(DIAG_TAG, logEntry)
        logMessages = logMessages + logEntry
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        addLog("Permission result: $isGranted")
        if (isGranted) {
            state = DiagnosticState.READY
        } else {
            addLog("❌ Permission DENIED")
            state = DiagnosticState.PERMISSION_REQUIRED
        }
    }
    
    // Check permission on start
    LaunchedEffect(Unit) {
        addLog("=== DIAGNOSTIC SCREEN OPENED ===")
        addLog("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        addLog("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        addLog("Language: ${Locale.getDefault()}")
        
        val hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        addLog("Microphone permission: $hasPermission")
        
        if (hasPermission) {
            state = DiagnosticState.READY
        } else {
            state = DiagnosticState.PERMISSION_REQUIRED
        }
    }
    
    // Start listening function
    fun startDiagnosticListening() {
        addLog("=== START LISTENING REQUESTED ===")
        
        // Check if recognition available
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            addLog("❌ SpeechRecognizer.isRecognitionAvailable() = FALSE")
            errorMessage = "Speech recognition not available on this device"
            state = DiagnosticState.RESULT_ERROR
            return
        }
        addLog("✓ SpeechRecognizer.isRecognitionAvailable() = TRUE")
        
        // Cleanup old recognizer
        recognizer?.destroy()
        
        // Create new recognizer
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            addLog("✓ SpeechRecognizer created (hash: ${recognizer.hashCode()})")
        } catch (e: Exception) {
            addLog("❌ EXCEPTION creating SpeechRecognizer: ${e.message}")
            errorMessage = "Failed to create recognizer: ${e.message}"
            state = DiagnosticState.RESULT_ERROR
            return
        }
        
        // Reset state
        transcript = ""
        errorMessage = ""
        partialText = ""
        
        // Set up listener
        var resultReceived = false
        
        recognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    addLog("→ onReadyForSpeech()")
                }
            }
            
            override fun onBeginningOfSpeech() {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    addLog("→ onBeginningOfSpeech() ✓ USER STARTED SPEAKING")
                }
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Don't log every RMS change - too noisy
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Don't log buffer
            }
            
            override fun onEndOfSpeech() {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    addLog("→ onEndOfSpeech() - User stopped speaking, WAITING FOR RESULTS...")
                }
            }
            
            override fun onError(error: Int) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (resultReceived) {
                        addLog("→ onError($error) - IGNORED (result already received)")
                        return@post
                    }
                    resultReceived = true
                    
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH (5) - No speech detected or no match found"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT (6) - No input detected"
                        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK (2) - Network error (requires internet)"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT (2) - Network timeout"
                        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER (4) - Server unavailable"
                        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO (3) - Audio recording error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS (9)"
                        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT (1) - Client side error"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY (8)"
                        else -> "UNKNOWN_ERROR ($error)"
                    }
                    
                    addLog("→ onError($error): $errorMsg")
                    errorMessage = errorMsg
                    state = DiagnosticState.RESULT_ERROR
                    timeoutJob?.cancel()
                }
            }
            
            override fun onResults(results: Bundle?) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (resultReceived) {
                        addLog("→ onResults() - IGNORED (result already received)")
                        return@post
                    }
                    resultReceived = true
                    
                    addLog("→ onResults() ✓✓✓ FINAL RESULTS RECEIVED")
                    
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    addLog("  Results bundle contains ${matches?.size ?: 0} matches")
                    
                    if (matches != null && matches.isNotEmpty()) {
                        matches.forEachIndexed { index, match ->
                            addLog("  Match[$index]: \"$match\"")
                        }
                        transcript = matches[0]
                        addLog("✓ SUCCESS - Transcript: \"$transcript\"")
                        state = DiagnosticState.RESULT_SUCCESS
                    } else {
                        addLog("❌ Results bundle was empty or null")
                        errorMessage = "Empty results"
                        state = DiagnosticState.RESULT_ERROR
                    }
                    
                    timeoutJob?.cancel()
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (partial != null && partial.isNotEmpty()) {
                        partialText = partial[0]
                        addLog("→ onPartialResults: \"$partialText\"")
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    addLog("→ onEvent($eventType)")
                }
            }
        })
        
        addLog("✓ RecognitionListener registered")
        
        // Create intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        
        addLog("Recognition intent configured:")
        addLog("  Language: ${Locale.getDefault()} (${Locale.getDefault().displayName})")
        addLog("  Model: FREE_FORM")
        addLog("  Partial results: true")
        
        // Start listening
        try {
            recognizer?.startListening(intent)
            addLog("✓ startListening() called successfully")
            state = DiagnosticState.LISTENING
            
            // Start 30-second timeout
            timeoutJob?.cancel()
            timeoutJob = scope.launch {
                delay(30000)
                addLog("❌ TIMEOUT after 30 seconds - NO callback received")
                errorMessage = "30-second timeout - no callback from SpeechRecognizer"
                state = DiagnosticState.TIMEOUT
                recognizer?.cancel()
            }
            
        } catch (e: Exception) {
            addLog("❌ EXCEPTION calling startListening(): ${e.message}")
            Log.e(DIAG_TAG, "Exception starting listening", e)
            errorMessage = "Exception: ${e.message}"
            state = DiagnosticState.RESULT_ERROR
        }
    }
    
    // Stop listening function
    fun stopListening() {
        addLog("=== STOP REQUESTED ===")
        timeoutJob?.cancel()
        recognizer?.stopListening()
        recognizer?.cancel()
        state = DiagnosticState.READY
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            addLog("=== SCREEN CLOSING - CLEANUP ===")
            timeoutJob?.cancel()
            recognizer?.destroy()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Recognition Diagnostic") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (state) {
                        DiagnosticState.LISTENING -> Color(0xFFE3F2FD)
                        DiagnosticState.RESULT_SUCCESS -> Color(0xFFE8F5E9)
                        DiagnosticState.RESULT_ERROR, DiagnosticState.TIMEOUT -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF5F5F5)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (state) {
                            DiagnosticState.INIT -> "Initializing..."
                            DiagnosticState.PERMISSION_REQUIRED -> "Permission Required"
                            DiagnosticState.READY -> "Ready to Test"
                            DiagnosticState.LISTENING -> "🎤 LISTENING..."
                            DiagnosticState.RESULT_SUCCESS -> "✓ SUCCESS"
                            DiagnosticState.RESULT_ERROR -> "✗ ERROR"
                            DiagnosticState.TIMEOUT -> "⏱ TIMEOUT"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (partialText.isNotEmpty() && state == DiagnosticState.LISTENING) {
                        Text(
                            text = "Partial: \"$partialText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    
                    if (transcript.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transcript:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\"$transcript\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state == DiagnosticState.PERMISSION_REQUIRED) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Grant Permission")
                    }
                }
                
                if (state == DiagnosticState.READY || state == DiagnosticState.RESULT_SUCCESS || 
                    state == DiagnosticState.RESULT_ERROR || state == DiagnosticState.TIMEOUT) {
                    Button(
                        onClick = { startDiagnosticListening() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyRed
                        )
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START TEST")
                    }
                }
                
                if (state == DiagnosticState.LISTENING) {
                    Button(
                        onClick = { stopListening() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STOP")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Log messages
            Text(
                text = "Event Log (${logMessages.size} entries):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    logMessages.forEach { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
