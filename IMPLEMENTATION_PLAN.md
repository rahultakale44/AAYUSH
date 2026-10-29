# Phase 4 Completion Plan

## Status: IN PROGRESS

### Just Completed
- ✅ TransportDashboardScreen.kt - Full transport coordinator interface

### Next Immediate Steps (In Order)

1. **HospitalDashboardScreen.kt** (30 mins)
   - Summary cards from HospitalViewModel
   - Case queue list
   - Filter by urgency/status
   - Navigate to case detail

2. **HospitalCaseDetailScreen.kt** (45 mins)
   - Patient Brief display (consent-aware)
   - Symptoms & triage
   - Staff assignment UI
   - Status updates

3. **Navigation Integration** (30 mins)
   - Update Screen.kt with new routes
   - Update AAYUSHNavHost.kt
   - Update MainActivity.kt to start with RoleSelectionScreen
   - Store selected role preference

4. **Add Strings** (20 mins)
   - English strings for all new screens
   - Hindi translations
   - Marathi translations

5. **Network Configuration** (15 mins)
   - Add INTERNET permission to AndroidManifest.xml
   - Add network_security_config.xml for cleartext HTTP

6. **Backend Server** (Option B - Skip for now)
   - Document that fallback mode is being used
   - Cases are demo-only, not synced between devices
   - Honest limitation disclosure

7. **Testing & Build** (30 mins)
   - Build debug APK
   - Test navigation flow
   - Test role switching
   - Verify all screens render

Total Estimated Time: ~3 hours for minimal viable demo

## Demo Strategy

Use **Fallback Mode** with clear disclosure:
- Pre-load demo cases in SharedCaseRepository
- Each device works independently
- Role switching demonstrates UI concepts
- No false claims about real-time sync
- Honest about prototype limitations
