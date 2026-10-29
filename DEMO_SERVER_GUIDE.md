# AAYUSH Demo Server - Implementation Guide

**Purpose:** Enable real cross-device data sync for competition demo  
**Time Estimate:** 2-3 hours  
**Complexity:** Simple in-memory REST API

---

## Quick Start (Option A: Simple Ktor Server)

### 1. Create Server Directory Structure

```
demo-server/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/
│       │       └── aayush/
│       │           └── demo/
│       │               ├── Application.kt
│       │               ├── models/
│       │               │   ├── EmergencyCase.kt
│       │               │   ├── TransportStatus.kt
│       │               │   ├── HospitalCaseStatus.kt
│       │               │   └── StaffAssignment.kt
│       │               ├── routes/
│       │               │   └── CaseRoutes.kt
│       │               └── storage/
│       │                   └── CaseStore.kt
│       └── resources/
│           └── application.conf
```

### 2. build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
}

group = "com.aayush.demo"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.5")
    implementation("io.ktor:ktor-server-netty:2.3.5")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-gson:2.3.5")
    implementation("io.ktor:ktor-server-cors:2.3.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

application {
    mainClass.set("com.aayush.demo.ApplicationKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
```

### 3. Application.kt (Main Server)

```kotlin
package com.aayush.demo

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.aayush.demo.routes.configureCaseRoutes
import com.aayush.demo.storage.CaseStore

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    // Initialize case store
    val caseStore = CaseStore()
    
    // Install plugins
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        }
    }
    
    install(CORS) {
        anyHost() // For demo only - restrict in production
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }
    
    // Configure routes
    routing {
        get("/api/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }
        
        configureCaseRoutes(caseStore)
    }
    
    environment.log.info("Demo server started on http://0.0.0.0:8080")
}
```

### 4. CaseStore.kt (In-Memory Storage)

```kotlin
package com.aayush.demo.storage

import com.aayush.demo.models.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class CaseStore {
    private val cases = ConcurrentHashMap<Long, EmergencyCase>()
    private val idCounter = AtomicLong(1)
    
    fun addCase(case: EmergencyCase): EmergencyCase {
        val id = idCounter.getAndIncrement()
        val withId = case.copy(id = id)
        cases[id] = withId
        return withId
    }
    
    fun getCase(id: Long): EmergencyCase? = cases[id]
    
    fun getAllCases(): List<EmergencyCase> = cases.values.toList()
    
    fun updateCase(case: EmergencyCase): EmergencyCase? {
        return if (cases.containsKey(case.id)) {
            cases[case.id] = case
            case
        } else {
            null
        }
    }
    
    fun updateTransportStatus(caseId: Long, status: TransportStatus): EmergencyCase? {
        return cases[caseId]?.let { case ->
            val updated = case.copy(transportStatus = status)
            cases[caseId] = updated
            updated
        }
    }
    
    fun updateHospitalStatus(caseId: Long, status: HospitalCaseStatus): EmergencyCase? {
        return cases[caseId]?.let { case ->
            val updated = case.copy(hospitalStatus = status)
            cases[caseId] = updated
            updated
        }
    }
    
    fun addStaffAssignment(caseId: Long, assignment: StaffAssignment): EmergencyCase? {
        return cases[caseId]?.let { case ->
            val assignments = case.assignedStaff.toMutableList()
            if (!assignments.any { it.staffId == assignment.staffId }) {
                assignments.add(assignment)
                val updated = case.copy(assignedStaff = assignments)
                cases[caseId] = updated
                updated
            } else {
                null
            }
        }
    }
    
    fun removeStaffAssignment(caseId: Long, staffId: String): EmergencyCase? {
        return cases[caseId]?.let { case ->
            val assignments = case.assignedStaff.filter { it.staffId != staffId }
            val updated = case.copy(assignedStaff = assignments)
            cases[caseId] = updated
            updated
        }
    }
}
```

### 5. CaseRoutes.kt (REST Endpoints)

```kotlin
package com.aayush.demo.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import com.aayush.demo.models.*
import com.aayush.demo.storage.CaseStore

fun Route.configureCaseRoutes(caseStore: CaseStore) {
    route("/api/cases") {
        // Get all cases
        get {
            val cases = caseStore.getAllCases()
            call.respond(cases)
        }
        
        // Get specific case
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                return@get
            }
            
            val case = caseStore.getCase(id)
            if (case != null) {
                call.respond(case)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Case not found"))
            }
        }
        
        // Create new case
        post {
            val case = call.receive<EmergencyCase>()
            val created = caseStore.addCase(case)
            call.respond(HttpStatusCode.Created, created)
        }
        
        // Update case
        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                return@put
            }
            
            val case = call.receive<EmergencyCase>()
            val updated = caseStore.updateCase(case.copy(id = id))
            
            if (updated != null) {
                call.respond(updated)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Case not found"))
            }
        }
        
        // Update transport status
        patch("/{id}/transport") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                return@patch
            }
            
            val body = call.receive<Map<String, String>>()
            val statusStr = body["status"]
            
            if (statusStr == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing status"))
                return@patch
            }
            
            val status = try {
                TransportStatus.valueOf(statusStr)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status"))
                return@patch
            }
            
            val updated = caseStore.updateTransportStatus(id, status)
            if (updated != null) {
                call.respond(updated)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Case not found"))
            }
        }
        
        // Update hospital status
        patch("/{id}/hospital") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                return@patch
            }
            
            val body = call.receive<Map<String, String>>()
            val statusStr = body["status"]
            
            if (statusStr == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing status"))
                return@patch
            }
            
            val status = try {
                HospitalCaseStatus.valueOf(statusStr)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status"))
                return@patch
            }
            
            val updated = caseStore.updateHospitalStatus(id, status)
            if (updated != null) {
                call.respond(updated)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Case not found"))
            }
        }
        
        // Assign staff
        post("/{id}/staff") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                return@post
            }
            
            val assignment = call.receive<StaffAssignment>()
            val updated = caseStore.addStaffAssignment(id, assignment)
            
            if (updated != null) {
                call.respond(updated)
            } else {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Staff already assigned or case not found"))
            }
        }
        
        // Remove staff
        delete("/{id}/staff/{staffId}") {
            val id = call.parameters["id"]?.toLongOrNull()
            val staffId = call.parameters["staffId"]
            
            if (id == null || staffId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid parameters"))
                return@delete
            }
            
            val updated = caseStore.removeStaffAssignment(id, staffId)
            if (updated != null) {
                call.respond(updated)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Case not found"))
            }
        }
    }
}
```

### 6. Copy Model Classes from Android App

Copy these files from the Android app to `demo-server/src/main/kotlin/com/aayush/demo/models/`:
- `EmergencyCase.kt`
- `EmergencySession.kt`
- `TriageResult.kt`
- `SymptomInfo.kt`
- `PatientBrief.kt`
- `TransportStatus.kt`
- `HospitalCaseStatus.kt`
- `StaffAssignment.kt`
- `UrgencyLevel.kt`

**Note:** Remove Android-specific imports (like `@Parcelize`) and use simple data classes.

### 7. Run the Server

```bash
cd demo-server
./gradlew run
```

Server will start on `http://0.0.0.0:8080` (accessible from local network)

### 8. Configure Android App

Update `DemoApiClient.kt`:

```kotlin
class DemoApiClient(
    private val serverUrl: String = "http://192.168.1.105:8080" // Your laptop IP
)
```

Update `network_security_config.xml` to include your laptop IP:

```xml
<domain includeSubdomains="true">192.168.1.105</domain>
```

---

## Quick Start (Option B: Mock Backend with ngrok)

If building the server is too time-consuming, use a mock API service:

### 1. Install ngrok
```bash
# Download from https://ngrok.com/download
ngrok http 8080
```

### 2. Use Mockoon or Postman Mock Server
- Create endpoints matching the API client
- Return sample JSON responses
- Use ngrok URL in DemoApiClient

---

## Quick Start (Option C: Local-Only Demo with Disclaimers)

If backend is not feasible, add clear disclaimers:

### 1. Add Banner to All Dashboards

In `TransportDashboardScreen.kt`, `HospitalDashboardScreen.kt`:

```kotlin
// Add after TopAppBar
Surface(
    color = MaterialTheme.colorScheme.errorContainer,
    modifier = Modifier.fillMaxWidth()
) {
    Text(
        text = "⚠️ LOCAL DEMO MODE - Cases stored locally, not synced between devices",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(8.dp)
    )
}
```

### 2. Prepare Demo Data on Each Device

**Senior Device:**
- Create emergency session
- Note the case ID (display on screen)

**Transport Device:**
- Manually create a demo case with same symptoms
- Use case ID = 1 for consistency

**Hospital Device:**
- Manually create same demo case
- Demonstrate UI and staff assignment

### 3. Demo Script
- Acknowledge limitations upfront
- Focus on UI/UX, consent model, multilingual support
- Emphasize architecture and scalability potential

---

## Testing the Server

### Health Check
```bash
curl http://localhost:8080/api/health
```

Expected: `{"status":"healthy"}`

### Create Case
```bash
curl -X POST http://localhost:8080/api/cases \
  -H "Content-Type: application/json" \
  -d '{"id": 0, "session": {...}, "transportStatus": "PENDING", ...}'
```

### Get All Cases
```bash
curl http://localhost:8080/api/cases
```

### Update Transport Status
```bash
curl -X PATCH http://localhost:8080/api/cases/1/transport \
  -H "Content-Type: application/json" \
  -d '{"status": "DISPATCHED"}'
```

---

## Network Configuration for Physical Devices

### 1. Find Your Laptop IP

**Windows:**
```powershell
ipconfig
# Look for "Wireless LAN adapter Wi-Fi" → IPv4 Address
# Example: 192.168.1.105
```

**Mac/Linux:**
```bash
ifconfig
# Look for en0 or wlan0 → inet
# Example: 192.168.1.105
```

### 2. Update Android Configuration

File: `app/src/main/res/xml/network_security_config.xml`

```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">192.168.1.105</domain>
</domain-config>
```

File: `app/src/main/java/com/aayush/emergency/data/api/DemoApiClient.kt`

```kotlin
class DemoApiClient(
    private val serverUrl: String = "http://192.168.1.105:8080"
)
```

### 3. Firewall Rules

**Windows:**
```powershell
# Allow port 8080 on private network
New-NetFirewallRule -DisplayName "AAYUSH Demo Server" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

**Mac:**
```bash
# Firewall typically allows local network by default
# Check System Preferences → Security & Privacy → Firewall
```

### 4. Test from Phone

```bash
# Install Termux or use browser
curl http://192.168.1.105:8080/api/health
```

Or open in Chrome: `http://192.168.1.105:8080/api/health`

---

## Troubleshooting

### "Connection refused"
- Check server is running: `netstat -an | grep 8080`
- Check firewall allows port 8080
- Verify both devices on same WiFi network

### "Network unreachable"
- Verify laptop IP address is correct
- Check network_security_config.xml includes laptop IP
- Ensure cleartext traffic allowed for that domain

### "Timeout"
- Increase timeout in DemoApiClient (currently 5 seconds)
- Check WiFi signal strength
- Test with curl first to isolate issue

### "JSON parsing error"
- Ensure model classes match exactly
- Check Gson configuration in both client and server
- Use simple data classes without complex nested structures

---

## Demo Day Checklist

### Morning of Demo
- [ ] Start laptop, connect to WiFi
- [ ] Note laptop IP address: `__________________`
- [ ] Update Android app with laptop IP
- [ ] Rebuild APK: `./gradlew assembleDebug`
- [ ] Install on all 3 devices
- [ ] Start demo server: `cd demo-server && ./gradlew run`
- [ ] Test health endpoint from browser
- [ ] Create test case from senior device
- [ ] Verify case appears on transport device
- [ ] Verify case appears on hospital device
- [ ] Test staff assignment and status updates

### 5 Minutes Before Demo
- [ ] Server running and responsive
- [ ] All devices connected to WiFi
- [ ] Test data cleared (or demo-ready)
- [ ] Battery charged on all devices
- [ ] Backup screenshots/video ready

### During Demo
- Keep laptop awake (disable sleep)
- Monitor server logs for errors
- Have backup plan if network fails

---

## Estimated Time

| Task | Time | Priority |
|------|------|----------|
| Create server directory structure | 15 min | High |
| Copy model classes | 30 min | High |
| Implement CaseStore | 30 min | High |
| Implement REST routes | 45 min | High |
| Test with curl | 15 min | High |
| Configure Android app | 15 min | High |
| Test with physical devices | 30 min | High |
| **TOTAL** | **3 hours** | |

---

## Recommendations

**For Tomorrow's Demo:**

1. **If you have 3+ hours:** Build the server (Option A)
   - Most impressive demonstration
   - Real cross-device sync
   - Shows full architecture

2. **If you have 1-2 hours:** Use local mode with disclaimers (Option C)
   - Focus on UI/UX quality
   - Emphasize consent model and multilingual support
   - Be honest about prototype limitations
   - Still demonstrates all screens and functionality

3. **Backup Plan:** Have screenshots/video
   - Record working demo in advance
   - Show video if live demo fails
   - Walk through code architecture

**Remember:** Judges care more about:
- Problem understanding
- Solution design
- User experience
- Code quality
- Presentation clarity

Than perfect working cross-device sync in a prototype.

---

**End of Guide**
