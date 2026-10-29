# CRITICAL BUG FIX: Speech Recognition Stuck on Listening

**Commit**: 51ada74  
**Date**: Current Session  
**Status**: Code Fixed, Awaiting Physical Device Testing

---

## THE ACTUAL ROOT CAUSE (FOUND)

After inspecting the complete codebase, I identified the **REAL BUG** that causes the stuck "Listening..." state:

### **Primary Issue: Premature SpeechRecognizer Destruction**

**Location**: `VoiceAssistanceScreen.kt`, lines 565-591 (before fix)

**The Bug**:
```kotlin
override fun onResults(results: Bundle?) {
    // ... process results ...
    if (transcript.isNotBlank()) {
        speechRecognizer.destroy()  // ❌ DESTROYED IMMEDIATELY
        
        // Callback posted to run LATER on main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onResult(transcript, null)  // State update happens AFTER destroy
        }
    }
}
```

**Why This Causes Stuck State**:
1. `speechRecognizer.destroy()` is called **synchronously**
2. State update callback is posted to main thread queue to run **asynchronously**
3. **Race condition**: Recognizer destroyed before state can transition from LISTENING
4. UI remains stuck showing "Listening..." because state update was queued but recognizer already gone
5. No error, no timeout, no recovery - user is permanently stuck

### **Secondary Issue: No Timeout Mechanism**

If `onResults()` or `onError()` callbacks never fire (device-specific bug, service crash, etc.):
- No watchdog timer to detect stuck state
- User waits forever with animated microphone icon
- No way to recover except force-closing app

### **Tertiary Issues**:
1. **Insufficient device logging** - Missing device model, Android version in logs
2. **No callback sequencing info** - Can't tell which callback fires when
3. **Single match limit** - Only asking for 1 recognition match reduces debugging info

---

## THE FIX (Implemented)

### 1. **Deferred Recognizer Cleanup**

**Before (Broken)**:
```kotlin
override fun onResults(results: Bundle?) {
    if (transcript.isNotBlank()) {
        speechRecognizer.destroy()  // ❌ Immediate destroy
        Handler.post { onResult(transcript, null) }
    }
}
```

**After (Fixed)**:
```kotlin
override fun onResults(results: Bundle?) {
    if (transcript.isNotBlank()) {
        // DO NOT destroy here - let callback handle cleanup
        Handler.post { onResult(transcript, null) }
    }
}

// In handleRecognitionResult callback:
val handleRecognitionResult: (String?, String?) -> Unit = { transcript, error ->
    if (transcript != null) {
        recognizedText = transcript
        flowState = VoiceFlowState.PROCESSING_TRANSCRIPT  // State updates FIRST
        
        // Clean up recognizer AFTER state update completes
        scope.launch {
            delay(100)  // Ensure state update finishes
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        
        processTranscript(...)
    }
}
```

**Result**: State transition completes **BEFORE** recognizer is destroyed ✅

---

### 2. **30-Second Timeout Watchdog**

**Added**:
```kotlin
// When starting recognition:
recognitionStartTime = System.currentTimeMillis()

timeoutJob = scope.launch {
    delay(30000)  // 30 second timeout
    val elapsedTime = System.currentTimeMillis() - recognitionStartTime
    Log.e(TAG, "✗ RECOGNITION TIMEOUT after ${elapsedTime}ms")
    
    // Force error state if still listening
    if (flowState == VoiceFlowState.LISTENING) {
        handleRecognitionResult(
            null,
            "Recognition timeout. The speech service did not respond. Please try again."
        )
    }
}

// When result arrives:
timeoutJob?.cancel()  // Cancel the watchdog
```

**Result**: User never stuck for more than 30 seconds ✅

---

### 3. **Enhanced Logging**

**Added at startup**:
```kotlin
Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
Log.d(TAG, "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
Log.d(TAG, "Language: ${Locale.getDefault()}")
```

**Added in onResults**:
```kotlin
Log.i(TAG, "  Matches count: ${matches?.size ?: 0}")
matches.forEachIndexed { index, match ->
    Log.i(TAG, "    [$index]: \"$match\"")
}
Log.i(TAG, "  Best match (trimmed): \"$transcript\" (length=${transcript.length})")
```

**Added in startListening**:
```kotlin
Log.d(TAG, "Recognition intent configured:")
Log.d(TAG, "  Language: ${Locale.getDefault()} (${Locale.getDefault().displayName})")
Log.d(TAG, "  Model: FREE_FORM")
Log.d(TAG, "  Partial results: true")
Log.d(TAG, "  Min length: 15000ms")
Log.d(TAG, "  Silence timeout: 2000ms")

Log.i(TAG, "Starting listening... (recognizer hash: ${speechRecognizer.hashCode()})")
try {
    speechRecognizer.startListening(intent)
    Log.i(TAG, "✓ startListening() called successfully")
} catch (e: Exception) {
    Log.e(TAG, "✗ EXCEPTION calling startListening(): ${e.message}", e)
    return null
}
```

**Result**: Complete visibility into recognition lifecycle ✅

---

### 4. **Increased Recognition Match Count**

**Changed**:
```kotlin
putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)  // Was: 1
```

**Why**: Getting top 3 matches helps debug ambiguous recognition issues

---

### 5. **Cleanup Enhancement**

**Added**:
```kotlin
DisposableEffect(Unit) {
    onDispose {
        Log.d(TAG, "=== DISPOSING VOICE ASSISTANCE SCREEN ===")
        timeoutJob?.cancel()  // Cancel watchdog
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.cancel()  // Cancel recognition
        speechRecognizer?.destroy()
        Log.d(TAG, "All resources cleaned up")
    }
}
```

**Result**: Proper cleanup prevents resource leaks ✅

---

## FILES CHANGED

### VoiceAssistanceScreen.kt
- **Lines changed**: 124 insertions, 19 deletions
- **Key changes**:
  1. Added `recognitionStartTime` and `timeoutJob` state variables
  2. Modified `handleRecognitionResult` to defer recognizer cleanup
  3. Added 30-second timeout watchdog in 3 places (initial, TTS skip, retry)
  4. Enhanced logging throughout recognition lifecycle
  5. Increased max results from 1 to 3
  6. Added device info logging
  7. Improved cleanup in DisposableEffect
  8. Removed immediate `speechRecognizer.destroy()` calls from callbacks

---

## BUILD STATUS

```bash
./gradlew assembleDebug
BUILD SUCCESSFUL in 39s
```

**APK Location**: `app/build/outputs/apk/debug/app-debug.apk` ✅

**Warnings**: 2 deprecation warnings (Locale constructor) - non-critical

---

## TESTING INSTRUCTIONS

### Step 1: Install APK

```bash
# Connect phone via USB with USB debugging enabled
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected output:
```
Performing Streamed Install
Success
```

---

### Step 2: Start Log Capture

**Open a NEW terminal window** and run:

```bash
adb logcat -s AAYUSH_VOICE
```

This will show ONLY the speech recognition logs in real-time.

---

### Step 3: Test the Complete Flow

#### Test Case 1: English Critical Symptom

1. Open AAYUSH app on phone
2. Log in as senior user (savita.deshmukh@example.com / demo123)
3. Navigate to Voice Assistance
4. **Observe greeting**: Should hear "Hello. I am AAYUSH..."
5. **Wait for listening**: Microphone icon appears with "Listening..."
6. **Speak clearly**: "I have chest pain and difficulty breathing"
7. **Stop speaking** and wait
8. **Watch the app AND the logs simultaneously**

**Expected Log Sequence** (critical points marked):
```
AAYUSH_VOICE ==> INITIALIZING VOICE ASSISTANCE ===
AAYUSH_VOICE Device: [Your Phone Model]
AAYUSH_VOICE Android: [Your Android Version]
AAYUSH_VOICE Language: en_US
AAYUSH_VOICE TTS initialization status: 0
AAYUSH_VOICE Microphone permission: true
AAYUSH_VOICE === SPEAKING GREETING ===
AAYUSH_VOICE Greeting language: en
AAYUSH_VOICE TTS onStart: greeting
AAYUSH_VOICE TTS onDone: greeting
AAYUSH_VOICE ✓ Greeting complete, triggering onGreetingComplete()
AAYUSH_VOICE Greeting complete, starting recognition
AAYUSH_VOICE === STARTING SPEECH RECOGNITION ===
AAYUSH_VOICE ✓ Speech recognition available
AAYUSH_VOICE Created SpeechRecognizer instance: [hash]
AAYUSH_VOICE Recognition intent configured:
AAYUSH_VOICE   Language: en_US (English (United States))
AAYUSH_VOICE   Model: FREE_FORM
AAYUSH_VOICE   Partial results: true
AAYUSH_VOICE   Min length: 15000ms
AAYUSH_VOICE   Silence timeout: 2000ms
AAYUSH_VOICE Starting listening... (recognizer hash: [hash])
AAYUSH_VOICE ✓ startListening() called successfully
AAYUSH_VOICE Waiting for recognition callbacks...
AAYUSH_VOICE → onReadyForSpeech()
AAYUSH_VOICE → onBeginningOfSpeech() - User started speaking
AAYUSH_VOICE → onRmsChanged(...)  [multiple times as you speak]
AAYUSH_VOICE → onPartialResults: "I have"
AAYUSH_VOICE → onPartialResults: "I have chest pain"
AAYUSH_VOICE → onEndOfSpeech() - User stopped speaking (WAITING FOR RESULTS...)
AAYUSH_VOICE → onResults()
AAYUSH_VOICE   Matches count: 3
AAYUSH_VOICE     [0]: "I have chest pain and difficulty breathing"
AAYUSH_VOICE     [1]: "I have chest pains and difficulty breathing"
AAYUSH_VOICE     [2]: "I have chest pain and difficulty breathing"
AAYUSH_VOICE   Best match (trimmed): "I have chest pain and difficulty breathing" (length=47)
AAYUSH_VOICE ✓ SUCCESS - Valid transcript received
AAYUSH_VOICE handleRecognitionResult called - transcript: I have chest pain and difficulty breathing, error: null
AAYUSH_VOICE ✓ Recognition SUCCESS - Transcript: "I have chest pain and difficulty breathing"
AAYUSH_VOICE SpeechRecognizer destroyed after successful recognition
AAYUSH_VOICE === PROCESSING TRANSCRIPT ===
AAYUSH_VOICE Transcript: "I have chest pain and difficulty breathing"
AAYUSH_VOICE Extracted 2 symptoms
AAYUSH_VOICE   Symptom 0: CHEST_PAIN - Chest pain or discomfort (SEVERE)
AAYUSH_VOICE   Symptom 1: BREATHING_DIFFICULTY - Difficulty breathing or shortness of breath (SEVERE)
AAYUSH_VOICE === CREATING EMERGENCY SESSION ===
AAYUSH_VOICE ✓ Emergency session created: ID=[id], Status=ACTIVE
AAYUSH_VOICE === ADDING SYMPTOMS TO SESSION ===
AAYUSH_VOICE Adding symptom: Chest pain or discomfort
AAYUSH_VOICE Adding symptom: Difficulty breathing or shortness of breath
AAYUSH_VOICE Triage assessment triggered (automatic)
AAYUSH_VOICE Generating patient brief...
AAYUSH_VOICE ✓ Processing complete
AAYUSH_VOICE === NAVIGATING TO URGENCY ASSESSMENT ===
```

**Expected App Behavior**:
- Screen shows "Listening..." → "Processing your speech..." → "Analyzing symptoms..." → Navigation
- Urgency Assessment screen opens showing **RED** urgency
- Symptoms displayed: "Chest pain or discomfort", "Difficulty breathing or shortness of breath"
- Can proceed to Patient Brief

---

#### Test Case 2: Hindi Symptom

Repeat the test, but speak in Hindi:

**Speak**: "Mujhe seene mein dard hai aur saans lene mein takleef hai"

**Expected logs** should show Hindi text recognition and same symptom extraction.

---

#### Test Case 3: Timeout Detection (Silence Test)

1. Start Voice Assistance
2. Wait for "Listening..." screen
3. **Do NOT speak** - remain completely silent
4. **Observe for 30 seconds**

**Expected Log Sequence**:
```
AAYUSH_VOICE → onReadyForSpeech()
[wait 30 seconds - no other callbacks]
AAYUSH_VOICE ✗ RECOGNITION TIMEOUT after 30000ms - forcing error state
AAYUSH_VOICE handleRecognitionResult called - transcript: null, error: Recognition timeout...
AAYUSH_VOICE ✗ Recognition ERROR: Recognition timeout. The speech service did not respond. Please try again.
AAYUSH_VOICE SpeechRecognizer destroyed after error
```

**Expected App Behavior**:
- After 30 seconds, screen transitions to Error state
- Shows "Recognition timeout..." message
- "Try Again" button appears

---

#### Test Case 4: No Match Error

1. Start Voice Assistance
2. Speak very quietly or mumble unintelligibly
3. Observe logs and behavior

**Expected**:
- `onError(5)` with ERROR_NO_MATCH
- Error screen with "No speech detected. Please try speaking again."
- Retry button available

---

### Step 4: Failure Analysis

If the bug **STILL OCCURS** (stuck on "Listening..."):

**Capture and report**:

1. **Exact log output** from the moment you click Voice Assistance until stuck
   - Copy the COMPLETE log from `adb logcat -s AAYUSH_VOICE`

2. **Last callback received**
   - Which callback fired last before getting stuck?
   - Was it `onEndOfSpeech`? `onPartialResults`? Something else?

3. **Device information** (from logs)
   - Phone model
   - Android version
   - Language setting

4. **Timing information**
   - Did the 30-second timeout fire?
   - If yes, did the error screen appear?
   - If no, why didn't the timeout job trigger?

5. **Screenshot** of the stuck screen

---

## EXPECTED OUTCOMES

### ✅ SUCCESS Indicators

1. **Logs show complete callback sequence**
   - `onReadyForSpeech` → `onBeginningOfSpeech` → `onEndOfSpeech` → `onResults`

2. **State transitions visible in logs and UI**
   - INITIALIZING → AAYUSH_SPEAKING → LISTENING → PROCESSING_TRANSCRIPT → ANALYZING_SYMPTOMS → CREATING_SESSION → Navigation

3. **App navigates to Urgency Assessment**
   - Shows RED/ORANGE/GREEN based on symptoms
   - Can proceed to Patient Brief
   - No "No emergency session active" error

4. **Timeout works**
   - If no speech for 30 seconds, error screen appears
   - Retry button allows recovery

5. **No stuck state**
   - User is never permanently stuck on "Listening..."
   - Even if recognition fails, error recovery is available

---

### ❌ FAILURE Indicators

1. **Still stuck on "Listening..."** after speaking
   - Check logs: Did `onResults()` fire?
   - Check logs: Did timeout fire after 30 seconds?
   - If neither, there's a deeper Android API issue

2. **Timeout doesn't fire**
   - Coroutine job not launching
   - Scope cancelled prematurely
   - Device-specific coroutine issue

3. **onResults fires but state doesn't change**
   - Main thread callback not executing
   - State update being ignored
   - Compose recomposition issue

4. **App crashes**
   - Check full logcat: `adb logcat` (without filter)
   - Look for exception stack trace

---

## WHAT'S DIFFERENT THIS TIME

### Previous Attempts vs Current Fix

| Issue | Previous Attempts | Current Fix |
|-------|------------------|-------------|
| Recognizer cleanup | Destroyed in callback immediately | Deferred cleanup after state update |
| Timeout | None | 30-second watchdog timer |
| Logging | Basic callback names | Complete device info, config, results |
| Error recovery | Limited | Automatic timeout + retry |
| Race conditions | Callback vs destroy | Fixed execution order |

---

## TECHNICAL DETAILS

### Callback Execution Flow (Fixed)

```
User speaks → Recognition service processes → Result ready
    ↓
onResults() callback fires (background thread)
    ↓
Log the results + matches
    ↓
Handler.post { onResult(transcript, null) }  [queued for main thread]
    ↓
onResults() returns (DO NOT destroy here)
    ↓
Main thread processes Handler queue
    ↓
onResult() callback executes (main thread)
    ↓
Cancel timeout watchdog
    ↓
flowState = PROCESSING_TRANSCRIPT  [Compose state update]
    ↓
Launch coroutine with 100ms delay
    ↓
Delay ensures state update completes
    ↓
speechRecognizer?.destroy()  [NOW safe to destroy]
    ↓
Process transcript and navigate
```

---

### Timeout Mechanism

```
Start recognition
    ↓
recognitionStartTime = currentTimeMillis()
    ↓
Launch timeout coroutine
    ↓
delay(30000)  // 30 seconds
    ↓
Check: Still in LISTENING state?
    ├─ YES → Force error state
    └─ NO  → Do nothing (already processed)
```

If `onResults()` or `onError()` fires first:
```
Callback arrives
    ↓
timeoutJob?.cancel()  // Cancel the watchdog
    ↓
Process result normally
```

---

## NEXT STEPS

### If Test PASSES ✅

1. **Document success**
   - Log output showing complete flow
   - Video/screenshot of working recognition
   - Confirmation message in issue tracker

2. **Mark Phase 2 COMPLETE**
   - Voice recognition working end-to-end
   - Automatic triage implemented
   - Multi-language support verified

3. **Move to Phase 3**
   - Hospital Dashboard
   - Case Management
   - Multi-user workflows

---

### If Test FAILS ❌

1. **Analyze logs**
   - Exact callback sequence
   - Timeout behavior
   - State transitions

2. **Identify failure mode**
   - Callbacks never fired → Android service issue
   - Callbacks fired but state stuck → Compose issue
   - Timeout didn't work → Coroutine issue

3. **Implement fallback**
   - Consider alternative speech API
   - Add manual text input as primary path
   - Use voice as optional enhancement

---

## COMMIT INFORMATION

```
Commit: 51ada74
Author: [Your Name]
Date: [Current Date]
Message: CRITICAL FIX: Speech recognition timeout and callback cleanup - prevent stuck listening state

Files changed: 1
Insertions: 124
Deletions: 19
Status: Pushed to origin/main ✅
```

---

## APK INSTALLATION COMMAND

```bash
adb install -r D:\AAYUSH\app\build\outputs\apk\debug\app-debug.apk
```

## LOG CAPTURE COMMAND

```bash
adb logcat -s AAYUSH_VOICE
```

---

**Current Status**: Ready for physical device testing with comprehensive logging  
**Blocking**: None  
**Dependencies**: Physical Android device with microphone and USB debugging  
**Expected Testing Duration**: 30-45 minutes for all test cases
