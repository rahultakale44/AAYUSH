# AAYUSH Voice-First Testing Guide

## Quick Start - Install on Device

### Step 1: Connect Your Android Phone
```bash
# Enable USB Debugging on phone first:
# Settings → About Phone → Tap "Build Number" 7 times
# Settings → Developer Options → Enable "USB Debugging"

# Verify connection
adb devices
```

### Step 2: Install the APK
```bash
cd D:\AAYUSH
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected output:
```
Performing Streamed Install
Success
```

### Step 3: Launch the App
```bash
adb shell am start -n com.aayush.emergency/.MainActivity
```

---

## Test Scenarios

### 🎤 Test 1: Voice Assistance (Primary Flow)

**Goal:** Verify completely hands-free voice capture

**Steps:**
1. Open AAYUSH app
2. Select "Senior Citizen" role
3. Tap "Voice Assistance" button (big blue microphone)
4. **DO NOT TAP ANYTHING ELSE**
5. Wait for AAYUSH to speak greeting
6. Listen for: "I am listening" at the end
7. Immediately speak your symptoms (without tapping)

**Test Cases:**

| Test | Say This | Expected Result |
|------|----------|----------------|
| Normal symptom | "I have a headache for 2 days" | Transcript appears, 10-second countdown starts |
| Critical - chest | "I have severe chest pain" | RED WARNING "CALL 112" appears immediately |
| Critical - breathing | "I cannot breathe properly" | RED WARNING "CALL 112" appears immediately |
| Critical - unconscious | "Someone is unconscious" | RED WARNING "CALL 112" appears immediately |
| Critical - bleeding | "Severe bleeding from injury" | RED WARNING "CALL 112" appears immediately |

**Verify:**
- ✅ TTS speaks greeting audibly
- ✅ Microphone activates automatically (you see pulsing red mic)
- ✅ NO NEED TO TAP microphone button
- ✅ Your speech converts to text
- ✅ 10-second countdown appears with big buttons
- ✅ Critical warnings show RED warning box instantly

### 🔄 Test 2: Speak Again Flow

**Goal:** Verify re-recording works without manual tap

**Steps:**
1. Complete Test 1 above
2. When countdown appears, tap "Speak Again" button
3. **DO NOT TAP MICROPHONE**
4. Wait 500ms
5. Speak different symptoms

**Verify:**
- ✅ Microphone auto-starts after tapping Speak Again
- ✅ No need to tap microphone icon
- ✅ New transcript replaces old one
- ✅ 10-second countdown restarts

### 🆘 Test 3: SOS Emergency

**Goal:** Verify immediate emergency creation

**Steps:**
1. From Home screen, tap red "SOS Emergency" button
2. Observe the screen for 2 seconds
3. Note what appears

**Verify:**
- ✅ "CALL 112 IMMEDIATELY" warning appears
- ✅ Shows "Creating emergency session..."
- ✅ After 2 seconds, auto-navigates to Patient Brief
- ✅ Patient Brief shows: "SOS emergency button pressed - immediate assistance requested"
- ✅ Severity: SEVERE
- ✅ NO FORM to fill out

### ⏱️ Test 4: 10-Second Countdown

**Goal:** Verify countdown and submission options

**Steps:**
1. Say a symptom via voice
2. Wait for countdown to appear
3. Test each button

| Button | Expected Behavior |
|--------|-------------------|
| **Cancel** (red, top) | Cancels emergency, returns to home |
| **Speak Again** (left, bottom) | Clears transcript, auto-starts listening again |
| **Submit Now** (right, bottom) | Immediately submits, goes to Patient Brief |
| Wait 10 seconds | Auto-submits when countdown reaches 0 |

**Verify:**
- ✅ Large countdown number (10...9...8...)
- ✅ Cancel button is RED and prominent (safety)
- ✅ All three options work correctly
- ✅ Auto-submit at 0 works

### 🌐 Test 5: Multi-Language

**Goal:** Verify TTS and STT work in all languages

**Steps:**
1. Go to Settings → Language
2. Select Hindi
3. Return to Home
4. Tap Voice Assistance
5. Verify greeting in Hindi
6. Speak symptoms in Hindi

**Test Languages:**

| Language | Greeting Should Say | Test Phrase |
|----------|---------------------|-------------|
| English | "Hello! I am AAYUSH. Please tell me what is troubling you. I am listening." | "I have fever" |
| Hindi | "नमस्ते! मैं आयुष हूँ। आपको क्या तकलीफ़ हो रही है? कृपया बोलकर बताइए, मैं सुन रहा हूँ।" | "मुझे बुखार है" |
| Marathi | "नमस्कार! मी आयुष आहे. तुम्हाला काय त्रास होत आहे? कृपया बोला, मी ऐकत आहे." | "मला ताप आहे" |

**Verify:**
- ✅ TTS speaks in selected language
- ✅ "I am listening" included at end
- ✅ STT recognizes speech in that language
- ✅ Transcript shows correctly

### 🚫 Test 6: Permission Handling

**Goal:** Verify graceful permission denial

**Steps:**
1. Uninstall AAYUSH
2. Reinstall: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Open app, select Senior Citizen
4. Tap Voice Assistance
5. When prompted for microphone permission, tap "Deny"

**Verify:**
- ✅ Shows "Microphone Permission Required" screen
- ✅ Explains why permission needed
- ✅ Text input box still available as fallback
- ✅ Can type symptoms manually
- ✅ "Grant Permission" button available
- ✅ "Go to Settings" button works if denied permanently

### 📝 Test 7: Text Fallback

**Goal:** Verify manual text entry always works

**Steps:**
1. Open Voice Assistance
2. Ignore voice, scroll down
3. Find "Or type your symptoms" card
4. Type: "I have stomach pain for 3 days"
5. Tap "Continue"

**Verify:**
- ✅ Text input always visible
- ✅ Can type while voice is available
- ✅ "Continue" button enables when text entered
- ✅ Same 10-second countdown appears
- ✅ Same navigation to Patient Brief

### 🏥 Test 8: Patient Brief Generation

**Goal:** Verify emergency data flows correctly

**Steps:**
1. Create profile first:
   - Home → Profile → Personal Info
   - Add name, blood type, etc.
2. Add medical conditions (diabetes, hypertension, etc.)
3. Add medications
4. Add allergies
5. Go to Profile → Consent
6. Enable "Share with Hospital"
7. Return to Home → Voice Assistance
8. Say "I have chest pain"
9. Let it submit (countdown or Submit Now)

**Verify Patient Brief shows:**
- ✅ Your name from profile
- ✅ Blood type
- ✅ Medical conditions listed
- ✅ Current medications listed
- ✅ Allergies listed
- ✅ Emergency contacts
- ✅ Triage result (should be "CRITICAL" for chest pain)
- ✅ Symptom transcript

### 🔊 Test 9: TTS When Unavailable

**Goal:** Verify app works even if TTS fails

**Steps:**
1. Go to phone Settings → Apps → Android System WebView
2. Disable "Google Text-to-Speech Engine" (if possible)
3. Open AAYUSH, tap Voice Assistance

**Verify:**
- ✅ App doesn't crash
- ✅ Shows message about TTS unavailable
- ✅ Still attempts to start listening
- ✅ Text fallback available

### 📱 Test 10: Navigation Flows

**Goal:** Verify all navigation is correct

**Test A: Voice → Patient Brief**
```
Home → Voice Assistance → [Speak] → Countdown → Patient Brief → Back → Home
```

**Test B: SOS → Patient Brief**
```
Home → SOS Emergency → [Auto-create, 2s] → Patient Brief → Back → Home
```

**Test C: Back button safety**
```
Home → Voice Assistance → [Speak] → [Countdown active] → Back → [Cancels emergency] → Home
```

**Verify:**
- ✅ Voice Assistance ends at Patient Brief
- ✅ SOS ends at Patient Brief
- ✅ Back button during countdown cancels safely
- ✅ No duplicate emergencies created

---

## Debugging

### View Logs in Real-Time
```bash
# All AAYUSH logs
adb logcat | grep -i aayush

# Voice-specific logs
adb logcat | grep -i "voice\|tts\|speech"

# Crash logs
adb logcat | grep -E "AndroidRuntime|FATAL"
```

### Check TTS Status
```bash
# List TTS engines
adb shell settings list system | grep tts

# Test TTS directly
adb shell am start -n com.google.android.tts/.settings.TtsSettingsActivity
```

### Check Microphone Permission
```bash
adb shell dumpsys package com.aayush.emergency | grep -A 5 "granted=true"
```

### Clear App Data (Fresh Start)
```bash
adb shell pm clear com.aayush.emergency
```

---

## Known Issues & Workarounds

### Issue: TTS cuts off or starts too early
**Cause:** Using rough timing estimate  
**Workaround:** Adjust delay in `speakGreeting()` function  
**Proper fix:** Implement `UtteranceProgressListener.onDone()`

### Issue: STT doesn't recognize language
**Cause:** Phone's default language differs from app language  
**Workaround:** Change phone system language to match  
**Note:** `Locale.getDefault()` reads system language

### Issue: Auto-listen triggers too fast
**Cause:** 500ms delay might be too short for older devices  
**Workaround:** Increase delay in `ReadyToListenView` LaunchedEffect

### Issue: Countdown finishes before user reads transcript
**Cause:** 10 seconds might be too short for some users  
**Workaround:** User can tap "Submit Now" immediately if ready, or "Speak Again" to restart  
**Enhancement:** Make countdown duration configurable (10s/15s/20s)

---

## Success Criteria

✅ **Voice Assistance is truly voice-first:**
- TTS speaks greeting audibly
- Microphone auto-starts after greeting
- NO manual tap required
- Text fallback available but not primary

✅ **SOS is immediate:**
- No forms to fill
- Creates emergency automatically
- Default symptom added
- Navigates directly to brief

✅ **Safety features work:**
- Critical warnings appear immediately
- Large Cancel button prominent
- 10-second window to review
- Text remains as fallback

✅ **Accessible for elderly:**
- Large buttons
- High contrast text
- Voice-first (no typing needed)
- Clear, simple language
- Multiple languages supported

---

## Reporting Issues

If you find bugs, please capture:

1. **Device Info:**
   ```bash
   adb shell getprop ro.product.model
   adb shell getprop ro.build.version.release
   ```

2. **App Version:**
   ```bash
   adb shell dumpsys package com.aayush.emergency | grep versionName
   ```

3. **Logs:**
   ```bash
   adb logcat > aayush_log.txt
   # Then reproduce the issue
   # Press Ctrl+C to stop
   ```

4. **Screenshots/Screen Recording:**
   ```bash
   # Start recording (max 3 minutes)
   adb shell screenrecord /sdcard/aayush_test.mp4
   
   # Stop recording (Ctrl+C)
   
   # Pull to computer
   adb pull /sdcard/aayush_test.mp4
   ```

---

**Happy Testing! 🎉**

The voice-first emergency experience is ready for real-world validation on physical devices.
