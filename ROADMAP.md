# Voice Launcher Roadmap

From current mLauncher to a fully voice-controlled Android launcher.

**Architecture:** On-device STT → Cloud LLM (swappable) → Action Layer (Intents + AccessibilityService)

---

## Phase 0: Foundation

### 0.1 Add RECORD_AUDIO permission
- Add `android.permission.RECORD_AUDIO` to `AndroidManifest.xml`
- Add runtime permission request flow in `HomeFragment` or onboarding

### 0.2 Add INTERNET permission verification
- Already declared in manifest. Confirm no network security config blocks API calls.

### 0.3 Create the voice module package structure
```
mlauncher/
  voice/
    stt/           → Speech-to-text abstraction + Android impl
    nlu/           → Natural language understanding (LLM interface + impl)
    action/        → Action resolution and dispatch
    feedback/      → TTS and visual feedback
    VoiceService.kt          → Orchestrator tying it all together
    VoiceSessionManager.kt   → Lifecycle, wake/sleep, state machine
```

### 0.4 Add dependencies
- OkHttp or Ktor for LLM API calls (Moshi already present for JSON)
- Android TTS is built-in, no dependency needed
- No STT dependency needed — using `android.speech.SpeechRecognizer`

---

## Phase 1: Speech-to-Text Layer

### 1.1 Define the STT interface
```kotlin
interface SpeechToText {
    fun startListening(locale: Locale, callback: STTCallback)
    fun stopListening()
    fun isAvailable(): Boolean
}

interface STTCallback {
    fun onPartialResult(text: String)
    fun onFinalResult(text: String, confidence: Float)
    fun onError(error: STTError)
}
```

### 1.2 Implement AndroidSpeechToText
- Wrap `android.speech.SpeechRecognizer` behind the `SpeechToText` interface
- Use `EXTRA_PARTIAL_RESULTS` for real-time feedback
- Handle `ERROR_NO_MATCH`, `ERROR_NETWORK`, `ERROR_AUDIO` gracefully
- Respect `LauncherLocaleManager` for multi-language STT

### 1.3 Add microphone trigger to HomeFragment
- Add a floating mic button (Compose) to the home screen, positioned via prefs
- Also wire a configurable gesture (e.g., long swipe up) to trigger voice via the existing `Action` enum — add `Action.VoiceCommand`
- Add the new action to `handleOtherAction()` in HomeFragment

### 1.4 Visual listening indicator
- Pulsing animation on the mic button while STT is active
- Show partial transcript text overlaid on home screen in real-time

---

## Phase 2: Natural Language Understanding (LLM Layer)

### 2.1 Define the NLU interface
```kotlin
interface VoiceNLU {
    suspend fun resolveIntent(
        transcript: String,
        context: PhoneContext
    ): VoiceAction
}

data class PhoneContext(
    val installedApps: List<AppListItem>,
    val recentApps: List<String>,
    val contacts: List<ContactListItem>,
    val availableActions: List<String>,
    val currentTime: String
)

sealed class VoiceAction {
    data class LaunchApp(val packageName: String, val profileType: String) : VoiceAction()
    data class CallContact(val name: String, val number: String?) : VoiceAction()
    data class SendMessage(val recipient: String, val body: String) : VoiceAction()
    data class SystemAction(val action: Action) : VoiceAction()
    data class OpenUrl(val url: String) : VoiceAction()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String?) : VoiceAction()
    data class DeviceSetting(val setting: String, val enable: Boolean) : VoiceAction()
    data class AccessibilityAction(val description: String) : VoiceAction()
    data class Clarification(val message: String) : VoiceAction()
    data class Unsupported(val message: String) : VoiceAction()
}
```

### 2.2 Implement CloudLLMNlu (Claude API)
- Build the system prompt dynamically from `PhoneContext`:
  - Serialize installed app names + aliases (from `prefs.getAppAlias()`)
  - Include recent apps from `AppUsageMonitor.getLastTenAppsUsed()`
  - List available `Action` enum values
  - Include contacts if permission granted
- Send transcript + context to Claude API via Moshi-serialized request
- Parse structured JSON response into `VoiceAction`
- Target latency: <2s round trip

### 2.3 System prompt design
The LLM prompt must:
- Map user speech to exactly one `VoiceAction` subclass
- Use fuzzy matching logic for app names (leverage alias system)
- Handle ambiguity by returning `Clarification` with a follow-up question
- Never hallucinate app names — only match against the provided list
- Support compound commands later (Phase 5)

### 2.4 Implement FallbackLocalNlu
- Keyword-based fallback for when there's no internet
- Pattern matching: "open {app}", "call {contact}", "lock screen", "take screenshot"
- Route app names through `FuzzyFinder.scoreApp()` directly
- Handles the 80% case (simple app launches and system actions) offline

### 2.5 API key management
- Store API key in encrypted SharedPreferences (EncryptedSharedPreferences from AndroidX Security)
- Settings UI for entering/pasting the key
- No hardcoded keys, no keys in the APK

---

## Phase 3: Action Dispatch Layer

### 3.1 Build ActionDispatcher
```kotlin
class ActionDispatcher(
    private val context: Context,
    private val viewModel: MainViewModel,
    private val actionService: ActionService?,
    private val prefs: Prefs
) {
    suspend fun execute(action: VoiceAction): ActionResult
}
```

This is the bridge between NLU output and Android system calls.

### 3.2 Map VoiceAction to existing infrastructure

| VoiceAction | Existing Code Path |
|---|---|
| `LaunchApp` | `MainViewModel.launchApp()` — fuzzy match packageName against app list |
| `CallContact` | `Intent(ACTION_CALL)` or `openDialerApp()` from SystemUtils |
| `SendMessage` | `Intent(ACTION_SENDTO)` with sms: URI |
| `SystemAction.LockScreen` | `ActionService.lockScreen()` |
| `SystemAction.ShowRecents` | `ActionService.showRecents()` |
| `SystemAction.ShowNotification` | `ActionService.openNotifications()` |
| `SystemAction.TakeScreenShot` | `ActionService.takeScreenShot()` |
| `SystemAction.OpenQuickSettings` | `ActionService.openQuickSettings()` |
| `SystemAction.OpenPowerDialog` | `ActionService.openPowerDialog()` |
| `SetAlarm` | `Intent(ACTION_SET_ALARM)` with extras |
| `OpenUrl` | `Intent(ACTION_VIEW)` with URI |
| `DeviceSetting` | Quick Settings intents or Settings panel intents (API 29+) |
| `AccessibilityAction` | AccessibilityService node traversal (Phase 5) |

### 3.3 Handle biometric-locked apps
- Check `prefs.lockedApps` before launching
- If locked, trigger biometric prompt, then launch on success
- Voice feedback: "This app requires biometric unlock"

### 3.4 Handle multi-profile apps
- If an app exists in both SYSTEM and WORK profiles, prefer SYSTEM unless user says "work" (e.g., "open work Gmail")
- Pass `profileType` from `VoiceAction.LaunchApp` through to `launchApp()`

### 3.5 ActionResult and error handling
```kotlin
sealed class ActionResult {
    object Success : ActionResult()
    data class NeedsConfirmation(val message: String) : ActionResult()
    data class NeedsPermission(val permission: String) : ActionResult()
    data class Failed(val reason: String) : ActionResult()
}
```

---

## Phase 4: Feedback Layer

### 4.1 Voice feedback (TTS)
- Use `android.speech.tts.TextToSpeech` for confirmations
- Speak action confirmations: "Opening Gmail", "Calling Mom", "Screen locked"
- Speak errors: "I didn't find an app called X", "No internet connection"
- Respect device volume and Do Not Disturb state
- Make TTS optional via prefs (some users want silent operation)

### 4.2 Visual feedback
- Overlay a Compose-based transcript card on HomeFragment showing:
  - What was heard (STT result)
  - What's happening (action being taken)
  - Error state if something failed
- Auto-dismiss after 2 seconds on success
- Match mLauncher's existing theming (use `prefs.appColor`, `prefs.appSize`)

### 4.3 Haptic feedback
- Short vibration on voice activation start
- Distinct pattern on success vs failure
- Uses existing `VIBRATE` permission already declared

---

## Phase 5: VoiceService Orchestrator

### 5.1 Build VoiceService
Ties everything together in a single entry point:

```
User triggers mic
  → VoiceSessionManager starts session
    → SpeechToText.startListening()
      → onFinalResult(transcript)
        → VoiceNLU.resolveIntent(transcript, buildPhoneContext())
          → ActionDispatcher.execute(action)
            → FeedbackManager.report(result)
```

### 5.2 State machine
```
IDLE → LISTENING → PROCESSING → EXECUTING → FEEDBACK → IDLE
                 ↘ TIMEOUT → IDLE
                 ↘ ERROR → FEEDBACK → IDLE
```

### 5.3 Integrate into HomeFragment
- Wire mic button tap → `VoiceService.startSession()`
- Wire `Action.VoiceCommand` gesture → same
- Add voice prefs to SettingsFragment:
  - Enable/disable voice
  - Choose NLU backend (Cloud / Offline)
  - API key entry
  - TTS on/off
  - Mic button visibility
  - Trigger gesture selection

### 5.4 Wake word support (optional)
- Use Android's `VoiceInteractionService` or a lightweight keyword spotter (Porcupine by Picovoice has a free tier)
- Always-listening wake word → starts VoiceService session
- Battery-conscious: only enable when screen is on and mLauncher is visible

---

## Phase 6: Extended Phone Control via AccessibilityService

### 6.1 Extend ActionService for UI automation
Add methods to the existing `ActionService`:
- `findAndClickNode(description: String)` — find a UI element by content description or text, click it
- `typeIntoFocused(text: String)` — input text into the currently focused field
- `scrollDirection(direction: Direction)` — scroll up/down/left/right
- `getScreenContent(): List<NodeInfo>` — read current screen for context

### 6.2 Notification actions
- Extend existing `NotificationManager` (notification listener service) to:
  - List active notifications by voice ("read my notifications")
  - Execute notification actions ("reply to WhatsApp", "dismiss alarm")
  - Use `Notification.Action` objects to trigger inline replies

### 6.3 Advanced compound commands
- Support multi-step commands via LLM: "Open WhatsApp and send Mom a message saying I'll be late"
  1. LLM decomposes into: LaunchApp(whatsapp) → AccessibilityAction(find contact Mom) → AccessibilityAction(type "I'll be late") → AccessibilityAction(click send)
  2. ActionDispatcher executes sequentially with delays between steps
  3. Verify each step succeeded before proceeding (read screen state)

### 6.4 Quick Settings control
- Map voice commands to Quick Settings tiles: "turn on WiFi", "enable Bluetooth", "flashlight on"
- Use `Settings.Panel` intents (API 29+) or AccessibilityService to toggle tiles

---

## Phase 7: Settings, Polish, Testing

### 7.1 Voice settings in SettingsFragment
Add a "Voice Control" section:
- Master toggle: enable/disable voice
- NLU backend selector: Cloud (Claude) / Offline
- API key field (encrypted storage)
- TTS toggle + voice selector
- Mic button position (left/center/right/hidden)
- Trigger gesture dropdown (reuse existing gesture config pattern)
- Wake word toggle (if implemented)

### 7.2 Onboarding update
Add a voice setup screen to `OnboardingActivity`:
- Request RECORD_AUDIO permission
- Test microphone
- Optional API key entry
- Quick demo of voice commands

### 7.3 Testing
- Unit tests for `FallbackLocalNlu` command parsing
- Unit tests for `VoiceAction` → `ActionDispatcher` mapping
- Integration tests for STT → NLU → Action pipeline with mocked STT
- Manual test matrix: app launch, calls, messages, system actions, settings toggles, multi-profile apps, biometric-locked apps

### 7.4 Error recovery
- No internet → fall back to `FallbackLocalNlu`
- STT returns empty → prompt retry with TTS
- LLM returns malformed JSON → fall back to fuzzy search on raw transcript
- App not found → suggest closest match via `FuzzyFinder`
- Permission missing → explain what's needed and open settings

---

## Phase order and dependencies

```
Phase 0 ──→ Phase 1 ──→ Phase 2 ──→ Phase 3 ──→ Phase 5
                                  ↗               ↓
                         Phase 4 ─┘          Phase 7
                                                  ↑
                                   Phase 6 ───────┘
```

- **Phase 0-1** can be done first (STT works standalone for testing)
- **Phase 2-3** are the core intelligence + execution
- **Phase 4** (feedback) can be built in parallel with Phase 3
- **Phase 5** wires everything together
- **Phase 6** (extended control) is additive — launch without it, add later
- **Phase 7** runs throughout but finalizes at the end

---

## What already exists and will be reused

| Existing | How it's reused |
|---|---|
| `ActionService` (accessibility) | Direct voice → system action bridge. Already has lock, recents, notifications, screenshot, power dialog, quick settings |
| `FuzzyFinder.scoreApp()` | Offline app name matching from voice transcript |
| `Prefs.getAppAlias()` | Voice-friendly app names already user-configurable |
| `MainViewModel.launchApp()` | App launch with biometric + multi-profile support |
| `AppUsageMonitor` | Feed recent apps to LLM for context |
| `Action` enum + `handleOtherAction()` | Existing action routing — voice just adds a new trigger |
| `SystemUtils` (dialer, camera, browser, etc.) | Direct intent helpers for common voice commands |
| `NotificationManager` service | Foundation for "read notifications" voice command |
| `LauncherLocaleManager` | Multi-language STT locale selection |
| Moshi | JSON serialization for LLM API requests/responses |
