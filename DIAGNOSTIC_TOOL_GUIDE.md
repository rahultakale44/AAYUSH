# Voice Recognition Diagnostic Tool

**Purpose**: Isolate and diagnose SpeechRecognizer failures on physical Android devices

**Created**: Current Session  
**Build**: SUCCESS  
**APK**: `D:\AAYUSH\app\build\outputs\apk\debug\app-debug.apk`

---

## What This Tool Does

The diagnostic screen is a **minimal isolated test** of Android's SpeechRecognizer API.

**It does NOT use**:
- ❌ TextToSpeech greeting
- ❌ 10-second countdown
- ❌ Symptom extraction
- ❌ Emergency session creation
- ❌ Triage assessment
- ❌ Navigation to other screens

**It ONLY tests**:
- ✅ Can SpeechRecognizer be created?
- ✅ Does `onReadyForSpeech()` fire?
- ✅ Does `onBeginningOfSpeech()` fire when user speaks?
- ✅ Does `onResults()` or `onError()` eventually fire?
- ✅ What is the actual transcript or error code?

---

## Installation

```bash
adb install -r D:\AAYUSH\app\build\outputs\apk\debug\app-debug.apk
```

Expected output:
```
Performing Streamed Install
Success
```

---

## How to Access Diagnostic Mode

### Step 1: Login
- Open AAYUSH app
- Login as: `savita.deshmukh@example.com` / `demo123`

### Step 2: Open Diagnostic
- On Home screen, you'll see three buttons:
  1. **🚨 SOS EMERGENCY** (red button)
  2. **🎤 Start Voice Assistance** (outlined button)
  3. **🔧 Voice Diagnostic (Debug)** (orange outlined button) ← Click this

### Step 3: Grant Permission
- First time: App will request microphone permission
- Click "Grant Permission" when prompted
- Allow microphone access

---

## Using the Diagnostic Tool

### Interface Overview

**Status Card** (top):
- Shows current state: READY → LISTENING → SUCCESS/ERROR/TIMEOUT
- Displays partial text as you speak (if available)
- Shows final transcript if recognition succeeds
- Shows error code if recognition fails

**Control Buttons**:
- **START TEST** - Begins speech recognition test
- **STOP** - Cancels active listening

**Event Log** (scrollable):
- Real-time log of all SpeechRecognizer callbacks
- Timestamped entries
- Shows device info, permissions, callbacks, results, errors

---

## Test Procedure

### Test 1: Basic Recognition

1. Click **START TEST**
2. **Speak clearly**: "I have chest pain and difficulty breathing"
3. **Stop speaking** and wait
4. **Observe** the status card AND event log

**Expected SUCCESS logs**:
```
[HH:mm:ss.SSS] === START LISTENING REQUESTED ===
[HH:mm:ss.SSS] ✓ SpeechRecognizer.isRecognitionAvailable() = TRUE
[HH:mm:ss.SSS] ✓ SpeechRecognizer created (hash: 123456)
[HH:mm:ss.SSS] ✓ RecognitionListener registered
[HH:mm:ss.SSS] ✓ startListening() called successfully
[HH:mm:ss.SSS] → onReadyForSpeech()
[HH:mm:ss.SSS] → onBeginningOfSpeech() ✓ USER STARTED SPEAKING
[HH:mm:ss.SSS] → onPartialResults: "I have chest"
[HH:mm:ss.SSS] → onPartialResults: "I have chest pain"
[HH:mm:ss.SSS] → onEndOfSpeech() - User stopped speaking, WAITING FOR RESULTS...
[HH:mm:ss.SSS] → onResults() ✓✓✓ FINAL RESULTS RECEIVED
[HH:mm:ss.SSS]   Results bundle contains 5 matches
[HH:mm:ss.SSS]   Match[0]: "I have chest pain and difficulty breathing"
[HH:mm:ss.SSS]   Match[1]: "I have chest pains and difficulty breathing"
[HH:mm:ss.SSS]   Match[2]: "I have chest pain in difficulty breathing"
[HH:mm:ss.SSS] ✓ SUCCESS - Transcript: "I have chest pain and difficulty breathing"
```

**Screen should show**:
- Status: ✓ SUCCESS
- Transcript: "I have chest pain and difficulty breathing"

---

### Test 2: Silence (Error Detection)

1. Click **START TEST**
2. **Do NOT speak** - remain completely silent
3. Wait 30 seconds

**Expected TIMEOUT logs**:
```
[HH:mm:ss.SSS] === START LISTENING REQUESTED ===
[HH:mm:ss.SSS] ✓ startListening() called successfully
[HH:mm:ss.SSS] → onReadyForSpeech()
[after 30 seconds]
[HH:mm:ss.SSS] ❌ TIMEOUT after 30 seconds - NO callback received
```

**Screen should show**:
- Status: ⏱ TIMEOUT
- Error: "30-second timeout - no callback from SpeechRecognizer"

---

### Test 3: Mumble (Error Handling)

1. Click **START TEST**
2. **Speak very quietly or mumble** unintelligibly
3. Wait for result

**Expected ERROR logs**:
```
[HH:mm:ss.SSS] → onReadyForSpeech()
[HH:mm:ss.SSS] → onBeginningOfSpeech() ✓ USER STARTED SPEAKING
[HH:mm:ss.SSS] → onEndOfSpeech() - User stopped speaking, WAITING FOR RESULTS...
[HH:mm:ss.SSS] → onError(5): ERROR_NO_MATCH (5) - No speech detected or no match found
```

**Screen should show**:
- Status: ✗ ERROR
- Error: "ERROR_NO_MATCH (5) - No speech detected or no match found"

---

## Capturing Logs

### Method 1: Via ADB (External Logs)

Open a **separate terminal** and run:

```bash
adb logcat -s AAYUSH_VOICE_DIAG
```

This captures logs externally for detailed analysis.

### Method 2: On-Screen Logs

The diagnostic screen shows all logs in the scrollable **Event Log** section at the bottom. You can:
- Scroll through the log to see all entries
- Take screenshots of the log
- Look for specific callback sequences

---

## Diagnostic Outcomes

### Outcome A: Recognition Works ✅

**Logs show**:
- `onReadyForSpeech()` fires
- `onBeginningOfSpeech()` fires when speaking
- `onEndOfSpeech()` fires when stopping
- `onResults()` fires with transcript
- Status shows SUCCESS
- Transcript is displayed

**Conclusion**: SpeechRecognizer API is working correctly on this device.

**Next Step**: The bug is in VoiceAssistanceScreen integration (TTS, state management, navigation). The fix needs to focus on callback handling or state transitions, NOT on SpeechRecognizer itself.

---

### Outcome B: Recognition Hangs (No Callbacks) ❌

**Logs show**:
- `startListening()` called successfully
- NO `onReadyForSpeech()` callback
- NO other callbacks at all
- Timeout fires after 30 seconds

**Conclusion**: SpeechRecognizer service is not responding. This is a device-specific or system-level issue.

**Next Step**: 
1. Check if Google app is updated
2. Try changing device language
3. Check if speech recognition works in other apps
4. Consider alternative: Manual text input as primary, voice as optional

---

### Outcome C: Recognition Fires onError ❌

**Logs show**:
- `onReadyForSpeech()` fires
- `onBeginningOfSpeech()` fires
- `onEndOfSpeech()` fires
- `onError()` fires with specific error code
- NO `onResults()` callback

**Common Error Codes**:

| Code | Error | Meaning | Solution |
|------|-------|---------|----------|
| 1 | ERROR_CLIENT | Client-side error | Check app state, recreate recognizer |
| 2 | ERROR_NETWORK | Network error | Speech recognition requires internet |
| 3 | ERROR_AUDIO | Audio recording error | Check microphone hardware/permissions |
| 4 | ERROR_SERVER | Server unavailable | Google speech service down, retry later |
| 5 | ERROR_NO_MATCH | No match found | User didn't speak clearly, retry |
| 6 | ERROR_SPEECH_TIMEOUT | No input | User didn't speak, retry |
| 8 | ERROR_RECOGNIZER_BUSY | Recognizer busy | Previous session not cleaned up |
| 9 | ERROR_INSUFFICIENT_PERMISSIONS | Permission denied | Check microphone permission |

**Conclusion**: Recognition attempted but failed for specific reason (see error code).

**Next Step**: Handle the specific error code appropriately. Most errors require retry with better conditions.

---

### Outcome D: onEndOfSpeech But No onResults ❌

**Logs show**:
- `onReadyForSpeech()` fires
- `onBeginningOfSpeech()` fires
- User speaks
- `onEndOfSpeech()` fires
- NOTHING after `onEndOfSpeech()` - no `onResults()`, no `onError()`
- Timeout eventually fires

**Conclusion**: This is the critical bug. Recognition detected speech ending but never delivered final result or error. This is a known Android bug on some devices/configurations.

**Next Step**: 
1. Try shorter speech input (3-5 words max)
2. Try different recognition parameters
3. Consider using different speech API (e.g., Google Cloud Speech API)
4. Implement manual text input as fallback

---

## Logging Reference

All logs use tag: **AAYUSH_VOICE_DIAG**

### Device Information Logs
```
[timestamp] === DIAGNOSTIC SCREEN OPENED ===
[timestamp] Device: Samsung Galaxy S21
[timestamp] Android: 13 (API 33)
[timestamp] Language: en_US
[timestamp] Microphone permission: true
```

### Recognition Setup Logs
```
[timestamp] === START LISTENING REQUESTED ===
[timestamp] ✓ SpeechRecognizer.isRecognitionAvailable() = TRUE
[timestamp] ✓ SpeechRecognizer created (hash: 123456)
[timestamp] ✓ RecognitionListener registered
[timestamp] Recognition intent configured:
[timestamp]   Language: en_US (English (United States))
[timestamp]   Model: FREE_FORM
[timestamp]   Partial results: true
[timestamp] ✓ startListening() called successfully
```

### Callback Logs
```
[timestamp] → onReadyForSpeech()
[timestamp] → onBeginningOfSpeech() ✓ USER STARTED SPEAKING
[timestamp] → onPartialResults: "partial text"
[timestamp] → onEndOfSpeech() - User stopped speaking, WAITING FOR RESULTS...
[timestamp] → onResults() ✓✓✓ FINAL RESULTS RECEIVED
[timestamp] → onError(5): ERROR_NO_MATCH (5) - No speech detected or no match found
```

### Success Logs
```
[timestamp]   Results bundle contains 5 matches
[timestamp]   Match[0]: "transcript"
[timestamp] ✓ SUCCESS - Transcript: "transcript"
```

### Error Logs
```
[timestamp] ❌ onError(X): [Error description]
[timestamp] ❌ TIMEOUT after 30 seconds - NO callback received
[timestamp] ❌ SpeechRecognizer.isRecognitionAvailable() = FALSE
[timestamp] ❌ EXCEPTION creating SpeechRecognizer: [exception]
```

---

## What to Report

### If Diagnostic Works ✅

Report:
1. "Diagnostic tool shows SUCCESS"
2. Screenshot of success screen with transcript
3. Copy of log showing complete callback sequence
4. "SpeechRecognizer API works correctly"
5. "Bug is in VoiceAssistanceScreen integration, not SpeechRecognizer"

### If Diagnostic Fails ❌

Report:
1. "Diagnostic tool shows [ERROR/TIMEOUT/HANGS]"
2. Screenshot of error screen
3. **Complete log output** from event log or adb logcat
4. **Last callback** that fired before failure
5. **Device model** (from logs)
6. **Android version** (from logs)
7. **Error code** (if onError fired)

Example:
```
Device: Samsung Galaxy S21
Android: 13 (API 33)
Language: en_US

Last callback: → onEndOfSpeech() - User stopped speaking, WAITING FOR RESULTS...
Result: TIMEOUT after 30 seconds
No onResults() or onError() callback received after onEndOfSpeech()

This indicates Android SpeechRecognizer bug - speech detected but no final result delivered.
```

---

## Key Differences vs VoiceAssistanceScreen

| Feature | VoiceAssistanceScreen | DiagnosticVoiceScreen |
|---------|----------------------|----------------------|
| TTS Greeting | ✅ Yes | ❌ No |
| Countdown | ✅ Yes | ❌ No |
| Symptom Extraction | ✅ Yes | ❌ No |
| Emergency Session | ✅ Yes | ❌ No |
| Triage | ✅ Yes | ❌ No |
| Navigation | ✅ Yes | ❌ No |
| SpeechRecognizer | ✅ Yes | ✅ Yes |
| Event Logging | ✅ Yes | ✅ Yes (more detailed) |
| Timeout | ✅ 30s | ✅ 30s |
| On-Screen Logs | ❌ No | ✅ Yes |
| Purpose | Full workflow | Isolate SpeechRecognizer |

---

## Files Added

1. **DiagnosticVoiceScreen.kt** (413 lines)
   - Isolated speech recognition test
   - No dependencies on TTS, triage, or emergency workflow
   - Real-time event log display
   - 30-second timeout protection

2. **Screen.kt** - Added `DiagnosticVoice` route

3. **AAYUSHNavHost.kt** - Added diagnostic route

4. **HomeScreen.kt** - Added diagnostic button (orange, with bug icon)

---

## Next Steps Based on Results

### If Diagnostic Shows SUCCESS ✅

The SpeechRecognizer works fine. The bug is in VoiceAssistanceScreen. Focus on:
1. TTS completion timing
2. State management during callbacks
3. Navigation after successful recognition
4. Emergency session creation lifecycle

### If Diagnostic Shows TIMEOUT/ERROR ❌

The SpeechRecognizer itself is failing. Options:
1. **Short-term**: Add manual text input as primary path
2. **Medium-term**: Investigate device-specific workarounds
3. **Long-term**: Consider alternative speech API (Google Cloud Speech)

---

## Commit Information

**Files Changed**: 4 files
- `DiagnosticVoiceScreen.kt` (new)
- `Screen.kt` (1 line added)
- `AAYUSHNavHost.kt` (8 lines added)
- `HomeScreen.kt` (diagnostic button added)

**Build Status**: ✅ SUCCESS  
**APK Ready**: Yes

---

**This diagnostic tool isolates the SpeechRecognizer API to determine whether the bug is in Android's recognition service or in AAYUSH's integration code.**
