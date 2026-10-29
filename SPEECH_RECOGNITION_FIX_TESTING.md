# Speech Recognition Fix - Physical Device Testing Guide

## Commit
**Commit Hash**: d0b5046  
**Message**: Fix speech recognition stuck on listening - main thread callbacks and proper recognizer lifecycle

---

## Root Causes Fixed

### 1. **SpeechRecognizer Reference Not Stored**
- **Problem**: Created new `SpeechRecognizer` instance but never saved it to `speechRecognizer` variable
- **Fix**: Changed `startListening()` to RETURN `SpeechRecognizer?` and stored it: `speechRecognizer = startListening(...)`

### 2. **Callbacks Not Running on Main Thread**
- **Problem**: Compose state updates MUST happen on main thread, but callbacks may run on background threads
- **Fix**: Wrapped ALL callback logic in `Handler(Looper.getMainLooper()).post { }` to ensure main thread execution

### 3. **Insufficient Logging**
- **Problem**: Impossible to debug physical device issues without detailed logs
- **Fix**: Added comprehensive logging with consistent `TAG = "AAYUSH_VOICE"` for every callback, state transition, and error

### 4. **Premature Result Handling**
- **Problem**: `onEndOfSpeech()` doesn't mean results arrived - must wait for `onResults()` or `onError()`
- **Fix**: Centralized callback handling with `handleRecognitionResult` lambda, added `resultReceived` flag to prevent duplicate processing

### 5. **Poor Error Handling**
- **Problem**: Error codes not logged or explained
- **Fix**: Added detailed error code logging with readable explanations for all `SpeechRecognizer` error codes

---

## Installation Instructions

### Step 1: Install APK on Physical Device

```bash
# Connect your Android phone via USB
# Enable USB debugging on your phone (Settings > Developer Options > USB Debugging)

# Install the fixed APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected output:
```
Performing Streamed Install
Success
```

### Step 2: Start Log Capture

Open a **separate terminal window** and run:

```bash
adb logcat -s AAYUSH_VOICE
```

This will show ONLY the AAYUSH voice recognition logs in real-time.

---

## Testing Procedure

### Test Case 1: English Symptom Recognition

1. **Open the app** on your phone
2. **Navigate** to Voice Assistance (from home screen or emergency start)
3. **Observe** the greeting: "Hello, I am AAYUSH..."
4. **Wait** for the microphone icon to appear and "Listening..." message
5. **Speak clearly**: "I have chest pain and difficulty breathing"
6. **Stop speaking** and wait
7. **Check the terminal** for log output

**Expected Log Sequence:**
```
AAYUSH_VOICE → onReadyForSpeech: recognizer is ready
AAYUSH_VOICE → onBeginningOfSpeech: user started speaking
AAYUSH_VOICE → onRmsChanged: volume level = [number]
AAYUSH_VOICE → onEndOfSpeech: user stopped speaking
AAYUSH_VOICE ✓ onResults: recognized text = "I have chest pain and difficulty breathing"
AAYUSH_VOICE ✓ Processing transcript: "I have chest pain and difficulty breathing"
AAYUSH_VOICE ✓ Extracted symptoms: [chest_pain, breathing_difficulty]
AAYUSH_VOICE ✓ Urgency: RED (critical)
AAYUSH_VOICE ✓ Navigating to urgency assessment
```

**Expected App Behavior:**
- Screen transitions from "Listening..." to "Processing transcript..."
- App navigates to Urgency Assessment screen showing RED urgency
- Symptoms "chest pain" and "breathing difficulty" are displayed

### Test Case 2: Hindi Symptom Recognition

1. **Repeat** the voice assistance flow
2. **Speak in Hindi**: "Mujhe seene mein dard hai aur saans lene mein takleef hai"
3. **Check logs** for proper processing

**Expected Log Sequence:**
```
AAYUSH_VOICE ✓ onResults: recognized text = "मुझे सीने में दर्द है और सांस लेने में तकलीफ है"
AAYUSH_VOICE ✓ Processing transcript: "मुझे सीने में दर्द है और सांस लेने में तकलीफ है"
AAYUSH_VOICE ✓ Extracted symptoms: [chest_pain, breathing_difficulty]
AAYUSH_VOICE ✓ Urgency: RED (critical)
```

### Test Case 3: Error Handling - No Speech

1. **Start** voice assistance
2. **Wait** for "Listening..."
3. **Do NOT speak** - remain silent
4. **Wait** 5-10 seconds

**Expected Log Sequence:**
```
AAYUSH_VOICE ✗ onError: ERROR_NO_MATCH (No speech input detected)
AAYUSH_VOICE ✓ State transition: LISTENING → ERROR
```

**Expected App Behavior:**
- Screen shows error message
- Retry button appears

### Test Case 4: Ambient Noise

1. **Start** voice assistance in a noisy environment
2. **Speak** your symptoms
3. **Check** if recognition still works or shows appropriate error

---

## What to Look For

### ✅ SUCCESS Indicators

1. **Logs show complete callback sequence**:
   - `onReadyForSpeech` → `onBeginningOfSpeech` → `onEndOfSpeech` → `onResults`

2. **State transitions happen**:
   - GREETING → LISTENING → PROCESSING_TRANSCRIPT → (navigation)

3. **App navigates** to Urgency Assessment screen

4. **Symptoms extracted** correctly from transcript

5. **No duplicate processing** - single transcript processed once

### ❌ FAILURE Indicators

1. **Stuck on "Listening..."** after speaking
   - Check logs: Which callback fired last?
   - Did `onResults()` get called?
   - Was there an `onError()` instead?

2. **No logs appear**
   - Recognizer not starting
   - Check if `isRecognitionAvailable(context)` returns false

3. **App crashes**
   - Check main logcat: `adb logcat` (without filter)
   - Look for exception stack traces

4. **Recognition timeout**
   - Check for `ERROR_SPEECH_TIMEOUT` or `ERROR_NO_MATCH`
   - May need to adjust recognition parameters

---

## Reporting Issues

If the bug persists, capture and report:

1. **Exact log output** from `adb logcat -s AAYUSH_VOICE`
2. **Which callback** fired last before getting stuck
3. **Phone model** and **Android version**
4. **Language** used for recognition
5. **Environment** (quiet room vs noisy area)

### Example Issue Report:

```
Phone: Samsung Galaxy S21, Android 13
Language: English (en-IN)
Environment: Quiet room

Log output:
AAYUSH_VOICE → onReadyForSpeech: recognizer is ready
AAYUSH_VOICE → onBeginningOfSpeech: user started speaking
AAYUSH_VOICE → onEndOfSpeech: user stopped speaking
[STUCK HERE - no onResults or onError]

Screen still shows "Listening..." after 30 seconds.
```

---

## Technical Details

### Changed Callback Pattern

**BEFORE (Broken):**
```kotlin
override fun onResults(results: Bundle?) {
    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    val transcript = matches?.firstOrNull()
    // State update happens on background thread ❌
    // speechRecognizer reference was null ❌
}
```

**AFTER (Fixed):**
```kotlin
override fun onResults(results: Bundle?) {
    Handler(Looper.getMainLooper()).post {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val transcript = matches?.firstOrNull()
        Log.d("AAYUSH_VOICE", "✓ onResults: recognized text = $transcript")
        // All state updates on main thread ✅
        // Proper error handling and logging ✅
        handleRecognitionResult(transcript)
    }
}
```

### Recognizer Lifecycle

```kotlin
// Start recognition and STORE the instance
speechRecognizer = startListening(
    context = context,
    selectedLanguage = selectedLanguage,
    recognizerRef = speechRecognizer,
    handleRecognitionResult = { transcript ->
        // Centralized result processing
    }
)

// Stop and cleanup on screen disposal
DisposableEffect(Unit) {
    onDispose {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
```

---

## Next Steps After Testing

### If Test PASSES ✅
- Mark Phase 2 as COMPLETE
- Move to Phase 3: Hospital Dashboard and Case Management
- Document the fix in final report

### If Test FAILS ❌
- Analyze the exact callback sequence from logs
- Identify which callback is missing or incorrect
- Consider alternative recognition approaches:
  - Different `RecognizerIntent` parameters
  - Fallback to text input after repeated failures
  - Different speech recognition library/API

---

**Current Status**: Waiting for physical device test results
**Last Updated**: Phase 2 - Speech Recognition Fix Commit d0b5046
