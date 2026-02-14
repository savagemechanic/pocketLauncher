# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Guardrails

**Read [`docs/guardrails.md`](docs/guardrails.md) before making any code change.** It is the authoritative guide for code quality, architecture, and Kotlin idioms in this project. All code must conform to the guardrails — no exceptions.

## Project Overview

mLauncher is a minimal Android launcher forked from Olauncher. It's a fully open-source (GPL-3.0) privacy-focused home screen replacement with gesture support, app renaming, custom icon packs, widgets, and biometric-protected settings.

## Build Commands

```bash
./gradlew clean assembleProdRelease          # Production release APK
./gradlew clean bundleProdRelease            # Production App Bundle (AAB)
./gradlew clean assembleNightlyRelease       # Nightly APK
./gradlew test                               # Unit tests
./gradlew connectedAndroidTest               # Instrumented tests
```

## Build Variants

Flavor dimension `channel` with four flavors: `prod` (app.mlauncher), `beta`, `alpha`, `nightly`. Debug builds append `.debug` to applicationId. Release builds use R8 minification and resource shrinking.

## Architecture

**MVVM with Fragments** — Single `MainActivity` hosts fragments via Navigation Component.

- **`MainViewModel`** — Central ViewModel managing app list, contacts, settings state, and caching (memory + file-based JSON). Uses LiveData for reactive UI updates.
- **Fragments**: `HomeFragment` (launcher screen), `AppDrawerFragment` (app list), `SettingsFragment` (extensive settings — 157KB), `FavoriteFragment`, `LocationSearchFragment`, `NotesManagerFragment`
- **Data layer**: `Prefs` (SharedPreferences + Moshi serialization), `WidgetDatabase` (Room ORM with `SavedWidgetEntity`/`WidgetDao`)
- **Services**: `ActionService` (accessibility), `NotificationManager` (notification listener)
- **`fuzzywuzzy` package**: Custom fuzzy search algorithm (separate from main app package) with alias prioritization

## Key Technical Details

- **Language**: Kotlin (100%), JVM target 17
- **SDK**: minSdk 28, targetSdk 36, compileSdk 36
- **UI**: Mix of Android Views (ViewBinding) and Jetpack Compose
- **Dependencies managed via**: `gradle/libs.versions.toml` (Version Catalog)
- **Key libraries**: Room 2.8.4, Moshi 1.15.2, Navigation Component, Biometric, Compose 1.10.2
- **Code generation**: KSP for Room and Moshi codegen
- **Package**: `com.github.codeworkscreativehub.mlauncher`

## Code Style

- 4-space indentation, no tabs
- Kotlin official code style (`kotlin.code.style=official`)
- Lint configured with `abortOnError = false`

## CI/CD

GitHub Actions workflows: PR builds, main branch builds, tagged releases (pattern `*.*.*.*`), and nightly releases (daily at 00:00 UTC). All use JDK 17 Temurin. Release artifacts are uploaded to GitHub Releases; release notifications posted to Discord.
