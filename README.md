<div align='center'>
	<h2>pocketLauncher - Voice-Controlled Minimal Android Launcher</h2>
    <table align='center'>
        Click on any image to enlarge it. To know more, explore and see for yourself.
        <tr>
            <td><img src='fastlane/metadata/android/en-US/images/phoneScreenshots/0.png' height='200' alt=""></td>
            <td><img src='fastlane/metadata/android/en-US/images/phoneScreenshots/1.png' height='200' alt=""></td>
            <td><img src='fastlane/metadata/android/en-US/images/phoneScreenshots/2.png' height='200' alt=""></td>
            <td><img src='fastlane/metadata/android/en-US/images/phoneScreenshots/3.png' height='200' alt=""></td>
        </tr>
        <tr>
            <td><img src='fastlane/metadata/android/en-US/images/phoneScreenshots/4.png' height='200' alt=""></td>
            <td><img src='fastlane/metadata/android/en-US/images/phoneScreenshots/5.png' height='200' alt=""></td>
            <td><img src='fastlane/metadata/android/en-US/images/phoneScreenshots/6.png' height='200' alt=""></td>
            <td><img src='fastlane/metadata/android/en-US/images/phoneScreenshots/7.png' height='200' alt=""></td>
        </tr>
    </table>
    <div align='center'>
        <p>
            <img src='https://img.shields.io/badge/Android-SDK_36-BD93F9?style=flat-square&logo=android&logoColor=white' alt="SDK-36">
            <a href='https://github.com/savagemechanic/pocketLauncher/blob/main/LICENSE'><img src='https://img.shields.io/github/license/savagemechanic/pocketLauncher?color=BD93F9&style=flat-square' alt="LICENSE"></a>
            <br>
            <img src='https://img.shields.io/badge/Maintained-yes-FF5555?style=flat-square' alt="Maintained">
        </p>
    </div>
</div>

## What is pocketLauncher?

pocketLauncher is a privacy-focused, voice-controlled Android home screen replacement that puts your phone at your command — literally. Speak to launch apps, make calls, send messages, control system settings, and navigate your device hands-free. When you're not talking, you get a clean, distraction-free home screen with gesture navigation.

Built on the foundation of [mLauncher](https://github.com/CodeWorksCreativeHub/mLauncher) (itself a fork of [Olauncher](https://github.com/tanujnotes/Olauncher)), pocketLauncher extends the minimal launcher concept with a full voice intelligence layer — on-device speech recognition, cloud-powered natural language understanding via Claude, and deep system integration through Android's accessibility APIs.

### Why pocketLauncher?

- **Your phone, your voice.** Say "open camera", "call Mom", "set alarm for 7:30", or "turn on wifi" and it just works.
- **No cloud required.** The offline NLU handles the 80% case (app launches, calls, system actions) with zero network dependency. Cloud AI is optional for complex commands.
- **Zero data collection.** No telemetry, no analytics, no tracking. Your voice commands are processed and forgotten.
- **Minimal by design.** No app icons cluttering your screen. No widgets begging for attention. Just your clock, your apps, and your voice.

## Features

### Voice Control
- **Speech-to-text** via Android's built-in speech recognizer — no third-party STT service
- **Offline NLU** with keyword matching and fuzzy search for app/contact resolution
- **Cloud NLU** (optional) via Claude API for complex, multi-step, and conversational commands
- **Action dispatch** for app launches, phone calls, SMS, alarms, URLs, system actions, and device settings
- **Compound commands** — "open WhatsApp and call Mom" executes as a sequence
- **Accessibility actions** — interact with UI elements on screen via voice
- **Visual feedback** — animated Compose overlay showing listening state, transcript, and results
- **Audio + haptic feedback** — TTS confirmations and vibration patterns (both configurable)
- **Secure API key storage** via AndroidX EncryptedSharedPreferences

### Launcher
- Clutter-free home screen with configurable gesture navigation
- Swipe gestures (up/down/left/right, short and long) mapped to any action
- App renaming and aliases for voice-friendly names
- Fuzzy search in app drawer with alias prioritization
- Biometric-protected app locking
- Custom icon packs
- Widget support
- Multi-profile app support (work profile, private space)
- Notes manager
- Weather display
- 17+ language translations

### Privacy
- Fully open-source (GPL-3.0)
- No internet permission required for core launcher functionality
- Cloud voice features are opt-in with user-provided API key
- No ads, no tracking, no data collection

## Architecture

```
Voice Command Flow:
  Mic tap / gesture
    -> Android SpeechRecognizer (on-device STT)
      -> NluRouter (strategy pattern)
        -> FallbackLocalNlu (offline keyword + fuzzy matching)
        -> CloudLLMNlu (Claude API, optional, 5s timeout)
      -> ActionDispatcherImpl (sealed class exhaustive dispatch)
        -> Android Intents / ActionService / AccessibilityService
      -> FeedbackManagerImpl (visual overlay + TTS + haptics)

Session State Machine:
  IDLE -> LISTENING -> PROCESSING -> EXECUTING -> FEEDBACK -> IDLE
```

**Tech stack:** Kotlin, MVVM, Jetpack Compose + Views, Room, Moshi, OkHttp, Navigation Component, Coroutines + StateFlow

## Build

```bash
./gradlew clean assembleProdRelease          # Production release APK
./gradlew clean assembleProdDebug            # Debug APK (no signing required)
./gradlew test                               # Unit tests
./gradlew connectedAndroidTest               # Instrumented tests
```

Requires: JDK 17, Android SDK 36, Kotlin 2.3.0

## Voice Settings

Configure voice control in **Settings > Voice Control**:

| Setting | Default | Description |
|---------|---------|-------------|
| Enable voice commands | On | Master toggle for all voice features |
| Use cloud AI | Off | Route commands through Claude API for complex intent resolution |
| Claude API key | — | Stored encrypted on-device, never transmitted except to Anthropic |
| Voice feedback (TTS) | On | Spoken confirmations for actions |
| Haptic feedback | On | Vibration patterns for listening/success/error states |

## Permissions

- `RECORD_AUDIO` — Voice command input via microphone
- `INTERNET` — Cloud NLU API calls (only when cloud AI is enabled)
- `EXPAND_STATUS_BAR` — Notification drawer gesture
- `QUERY_ALL_PACKAGES` — App list for launcher and voice matching
- `SET_ALARM` — Voice alarm setting
- `REQUEST_DELETE_PACKAGES` — App uninstall requests
- `PACKAGE_USAGE_STATS` — Recent apps for voice context
- `BIND_ACCESSIBILITY_SERVICE` — System actions (lock screen, recents, screenshots) and UI automation

## Contribute

- Open an [issue](https://github.com/savagemechanic/pocketLauncher/issues) for bugs, crashes, or feature requests
- Pull requests welcome to `main` branch — please discuss before large changes
- Translation contributions appreciated — see the [Wiki](https://github.com/CodeWorksCreativeHub/mLauncher/wiki) for guidance

## Credits

- [mLauncher](https://github.com/CodeWorksCreativeHub/mLauncher) by CodeWorksCreativeHub
- [Olauncher](https://github.com/tanujnotes/Olauncher) by tanujnotes
- [OlauncherCF](https://github.com/OlauncherCF/OlauncherCF)

## License

pocketLauncher is licensed under [GPL-3.0](LICENSE). You are free to use, study, modify, and distribute it. The copyleft provision ensures all derivatives remain open source.

pocketLauncher does not collect or transmit any user data. Cloud voice features send only voice transcripts to the Anthropic API when explicitly enabled by the user.
