# Voice Launcher Roadmap

From current mLauncher to a fully voice-controlled Android launcher.

**Architecture:** On-device STT → NLU (Cloud LLM + Local fallback) → Action Layer (Intents + AccessibilityService)

---

## Phase 0: Foundation ✅

### 0.1 Add RECORD_AUDIO permission ✅
- Added `android.permission.RECORD_AUDIO` to `AndroidManifest.xml`
- Runtime permission request flow in `HomeFragment`

### 0.2 Add INTERNET permission verification ✅
- Already declared in manifest. Confirmed no network security config blocks API calls.

### 0.3 Create the voice module package structure ✅
```
mlauncher/
  voice/
    stt/           → Speech-to-text abstraction + Android impl
    nlu/           → Natural language understanding (LLM interface + impl)
    action/        → Action resolution and dispatch
    feedback/      → TTS and visual feedback
    VoiceService.kt          → Orchestrator interface
    VoiceSessionManager.kt   → Lifecycle state machine interface
```

### 0.4 Add dependencies ✅
- OkHttp for LLM API calls (added to version catalog + build.gradle.kts)
- AndroidX Security Crypto for encrypted API key storage
- Android TTS is built-in, no dependency needed
- No STT dependency needed — using `android.speech.SpeechRecognizer`

---

## Phase 1: Speech-to-Text Layer ✅

### 1.1 Define the STT interface ✅
```kotlin
interface SpeechToText {
    fun startListening(locale: Locale, callback: STTCallback)
    fun stopListening()
    fun isAvailable(): Boolean
    fun destroy()
}
```

### 1.2 Implement AndroidSpeechToText ✅
- Wraps `android.speech.SpeechRecognizer` behind the `SpeechToText` interface
- Uses `EXTRA_PARTIAL_RESULTS` for real-time feedback
- Handles `ERROR_NO_MATCH`, `ERROR_NETWORK`, `ERROR_AUDIO` gracefully
- Respects locale selection for multi-language STT

### 1.3 Add microphone trigger to HomeFragment ✅
- Added mic FAB button to home screen shortcut bar
- Wired `Action.VoiceCommand` to gesture system via `handleOtherAction()`
- Created `ic_mic.xml` drawable

### 1.4 Visual listening indicator ✅
- Pulsing scale animation on mic button while STT is active
- Partial transcript overlay on home screen in real-time

---

## Phase 2: Natural Language Understanding (NLU Layer) ✅

### 2.1 Define the NLU interface ✅
- `VoiceNLU` interface with `resolveIntent(transcript, context): VoiceAction`
- `PhoneContext` data class with installed apps, recent apps, contacts, actions, time
- `VoiceAction` sealed class with 11 subtypes including `CompoundAction`

### 2.2 Implement CloudLLMNlu (Claude API) ✅
- OkHttp POST to Claude Messages API (`api.anthropic.com/v1/messages`)
- Dynamic system prompt built from `PhoneContext` (apps, aliases, contacts, recent apps)
- JSON schema of all `VoiceAction` subtypes as structured output format
- Moshi deserialization of Claude's response into `VoiceAction`
- Model: `claude-sonnet-4-5-20250929` (fast intent classification)
- 5-second timeout per request

### 2.3 Implement FallbackLocalNlu ✅
- Offline keyword/regex matching: "open X", "call X", "lock", "screenshot", etc.
- Fuzzy app matching via `FuzzyFinder.scoreApp()`
- Fuzzy contact matching via `FuzzyFinder.scoreContact()`
- Device setting toggles: "turn on wifi", "enable bluetooth"
- Alarm setting: "set alarm for 7:30"
- Falls through to raw fuzzy app search if no keyword pattern matches (80% case)

### 2.4 Implement NluRouter ✅
- Strategy pattern: if cloud enabled → try CloudLLMNlu (5s timeout), fallback to local
- If cloud disabled → FallbackLocalNlu directly
- Unsupported cloud results also trigger local fallback

### 2.5 API key management ✅
- Stored in `EncryptedSharedPreferences` (AndroidX Security, MasterKey AES256)
- Settings UI with masked display ("••••••last4")
- No hardcoded keys, no keys in APK

---

## Phase 3: Action Dispatch Layer ✅

### 3.1 Build ActionDispatcherImpl ✅
Exhaustive `when` dispatch on all `VoiceAction` sealed subtypes:

| VoiceAction | Implementation |
|---|---|
| `LaunchApp` | Find app in `viewModel.appList`, call `viewModel.launchApp()` |
| `CallContact` | `Intent(ACTION_DIAL)` with tel: URI, fuzzy contact lookup |
| `SendMessage` | `Intent(ACTION_SENDTO)` with smsto: URI + sms_body extra |
| `SystemAction` | Delegate to `ActionService` (lock, recents, notifications, etc.) |
| `OpenUrl` | `Intent(ACTION_VIEW)` with auto-https URI |
| `SetAlarm` | `Intent(AlarmClock.ACTION_SET_ALARM)` with hour/minute/label |
| `DeviceSetting` | Settings intents (wifi, bluetooth, airplane, location, etc.) |
| `AccessibilityAction` | Delegate to `AccessibilityActionHandler` |
| `CompoundAction` | Sequential execution with 500ms delays, abort on failure |
| `Clarification` | Returns `ActionResult.NeedsConfirmation` |
| `Unsupported` | Returns `ActionResult.Failed` |

### 3.2 Handle biometric-locked apps ✅
- Checks `prefs.lockedApps` before launch
- Biometric auth handled by `viewModel.launchApp()` internally

### 3.3 Handle multi-profile apps ✅
- `LaunchApp.profileType` passed through to app resolution

---

## Phase 4: Feedback Layer ✅

### 4.1 VoiceOverlayState ✅
- Sealed class: `Hidden`, `Listening(transcript)`, `Processing(transcript)`, `Success`, `Confirmation(message)`, `Error(message)`
- Exposed as `StateFlow` for reactive Compose consumption

### 4.2 FeedbackManagerImpl ✅
- **Visual**: Drives `MutableStateFlow<VoiceOverlayState>` for Compose
- **Audio**: `android.speech.tts.TextToSpeech` (respects `prefs.voiceTtsEnabled`)
- **Haptic**: Tick on listen start, success/error vibration patterns (respects `prefs.voiceHapticEnabled`)
- Maps `ActionResult` → appropriate overlay state + audio/haptic

### 4.3 VoiceOverlay Composable ✅
- Bottom-anchored floating card with `AnimatedVisibility` (fade + slide)
- Semi-transparent dark background, 16dp rounded corners
- **Listening**: Pulsing mic icon (`rememberInfiniteTransition`) + live transcript
- **Processing**: `CircularProgressIndicator` + transcript
- **Success**: Checkmark icon, auto-dismiss 1.5s
- **Error**: Red-tinted message, auto-dismiss 2s
- **Confirmation**: Accent-colored clarification text

---

## Phase 5: VoiceService Orchestrator ✅

### 5.1 VoiceSessionManagerImpl ✅
- `MutableStateFlow<VoiceSessionState>` as the FSM register
- Deterministic transitions: IDLE→LISTENING→PROCESSING→EXECUTING→FEEDBACK→IDLE
- Error/timeout paths: any state→ERROR→IDLE
- Invalid transitions logged and rejected

### 5.2 VoiceServiceImpl ✅
- Owns all components: `AndroidSpeechToText`, `NluRouter`, `ActionDispatcherImpl`, `FeedbackManagerImpl`
- `startVoiceCommand()`: checks IDLE state, starts STT with locale from prefs
- STT `onFinalResult` → coroutine: NLU resolve → dispatch → feedback
- `buildPhoneContext()`: pulls from ViewModel app/contact lists + `AppUsageMonitor`
- Auto-dismiss: 2s delay after feedback, reset to IDLE
- `cancel()` / `destroy()`: cleanup with scope cancellation

### 5.3 Integrate into HomeFragment ✅
- Replaced manual STT fields with single `voiceService: VoiceServiceImpl?`
- Lazy initialization on first voice command
- Replaced `voiceTranscript` TextView with `ComposeView` for overlay
- `onStop()`: calls `voiceService?.destroy()`

---

## Phase 6: Extended Phone Control via AccessibilityService ✅

### 6.1 AccessibilityActionHandler ✅
- `findAndClickNode(service, description)`: BFS traversal of accessibility node tree with fuzzy text/contentDescription matching
- `typeIntoFocused(service, text)`: Input text via `ACTION_SET_TEXT` on focused node
- `getScreenContent(service)`: Read visible text nodes for LLM context (capped at 2000 chars)
- Algorithm: BFS O(N) time, O(W) space where W = max tree width

### 6.2 CompoundAction support ✅
- Added `CompoundAction(actions: List<VoiceAction>)` to sealed class
- Sequential execution with 500ms delays between steps
- Abort on first failure

### 6.3 Notification actions
- **Not yet implemented** — extend `NotificationManager` for "read my notifications" voice command
- Future: execute notification inline reply actions

---

## Phase 7: Settings, Polish, Testing ✅

### 7.1 Voice settings in SettingsFragment ✅
Added "Voice Control" section with Compose UI:
- Toggle: Enable voice commands (`voiceEnabled`)
- Toggle: Use cloud AI (`voiceCloudEnabled`, reveals API key field)
- API key input: MaterialAlertDialog with masked EditText, stored in EncryptedSharedPreferences
- Toggle: Voice feedback / TTS (`voiceTtsEnabled`)
- Toggle: Haptic feedback (`voiceHapticEnabled`)

### 7.2 Preference keys ✅
- `VOICE_ENABLED`, `VOICE_CLOUD_ENABLED`, `VOICE_TTS_ENABLED`, `VOICE_HAPTIC_ENABLED` in `PrefsKeys.kt`
- Properties in `Prefs.kt` with SharedPreferences backing

### 7.3 String resources ✅
- Added 7 voice settings strings to `strings.xml`

### 7.4 Unit tests ✅
- `FallbackLocalNluTest` — VoiceAction data structure + pattern verification
- `ActionDispatcherImplTest` — ActionResult contract tests
- `VoiceSessionManagerImplTest` — Full FSM transition correctness (10 tests)
- `NluRouterTest` — Routing contract + sealed class exhaustiveness

### 7.5 Onboarding update
- **Not yet implemented** — add voice setup screen to onboarding flow

### 7.6 Wake word support
- **Not yet implemented** — optional always-listening trigger

---

## Implementation Status

| Phase | Status | Files |
|-------|--------|-------|
| Phase 0: Foundation | ✅ Complete | Module structure, permissions, dependencies |
| Phase 1: STT Layer | ✅ Complete | `stt/SpeechToText.kt`, `stt/AndroidSpeechToText.kt` |
| Phase 2: NLU Layer | ✅ Complete | `nlu/FallbackLocalNlu.kt`, `nlu/CloudLLMNlu.kt`, `nlu/NluRouter.kt` |
| Phase 3: Action Dispatch | ✅ Complete | `action/ActionDispatcherImpl.kt`, `action/AccessibilityActionHandler.kt` |
| Phase 4: Feedback Layer | ✅ Complete | `feedback/VoiceOverlayState.kt`, `feedback/FeedbackManagerImpl.kt`, `feedback/VoiceOverlay.kt` |
| Phase 5: Orchestrator | ✅ Complete | `VoiceSessionManagerImpl.kt`, `VoiceServiceImpl.kt` |
| Phase 6: Extended Control | ✅ Complete | `AccessibilityActionHandler.kt`, `CompoundAction` |
| Phase 7: Settings & Polish | ✅ Complete | Settings UI, prefs, tests |

---

## What already exists and is reused

| Existing | How it's reused |
|---|---|
| `ActionService` (accessibility) | Direct voice → system action bridge (lock, recents, notifications, screenshot, power, quick settings) |
| `FuzzyFinder.scoreApp()` | Offline app name matching from voice transcript |
| `FuzzyFinder.scoreContact()` | Offline contact name matching |
| `Prefs.getAppAlias()` | Voice-friendly app names already user-configurable |
| `MainViewModel.launchApp()` | App launch with biometric + multi-profile support |
| `MainViewModel.appList` / `contactList` | Live data feeds for NLU phone context |
| `AppUsageMonitor` | Feed recent apps to LLM for disambiguation |
| `Action` enum + `handleOtherAction()` | Existing action routing — voice is just a new trigger |
| Moshi | JSON serialization for LLM API requests/responses |
| OkHttp | HTTP client for Claude API |
| AndroidX Security Crypto | Encrypted API key storage |
