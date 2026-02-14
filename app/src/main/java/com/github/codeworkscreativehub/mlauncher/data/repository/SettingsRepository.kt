package com.github.codeworkscreativehub.mlauncher.data.repository

import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.Constants
import com.github.codeworkscreativehub.mlauncher.data.Message
import com.github.codeworkscreativehub.mlauncher.data.MessageWrong
import com.github.codeworkscreativehub.mlauncher.helper.receivers.LocationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for all launcher settings.
 * Exposes reactive [StateFlow]s for UI consumption and
 * mutation methods for writes.
 */
interface SettingsRepository {

    // --- General ---
    val appVersion: StateFlow<Int>
    val firstOpen: StateFlow<Boolean>
    val firstSettingsOpen: StateFlow<Boolean>
    val autoOpenApp: StateFlow<Boolean>
    val forceWallpaper: StateFlow<Boolean>
    val openAppOnEnter: StateFlow<Boolean>
    val homePager: StateFlow<Boolean>
    val recentAppsDisplayed: StateFlow<Boolean>
    val iconRainbowColors: StateFlow<Boolean>
    val recentCounter: StateFlow<Int>
    val enableFilterStrength: StateFlow<Boolean>
    val filterStrength: StateFlow<Int>
    val shortSwipeThreshold: StateFlow<Float>
    val longSwipeThreshold: StateFlow<Float>
    val searchFromStart: StateFlow<Boolean>
    val autoShowKeyboard: StateFlow<Boolean>
    val homeAppsNum: StateFlow<Int>
    val homePagesNum: StateFlow<Int>
    val opacityNum: StateFlow<Int>
    val appUsageStats: StateFlow<Boolean>
    val lockOrientation: StateFlow<Boolean>
    val lockOrientationPortrait: StateFlow<Boolean>
    val hapticFeedback: StateFlow<Boolean>
    val showAZSidebar: StateFlow<Boolean>
    val homeLocked: StateFlow<Boolean>
    val settingsLocked: StateFlow<Boolean>
    val hideSearchView: StateFlow<Boolean>
    val autoExpandNotes: StateFlow<Boolean>
    val clickToEditDelete: StateFlow<Boolean>
    val enableExpertOptions: StateFlow<Boolean>
    val wordList: StateFlow<String>

    // --- Display ---
    val showStatusBar: StateFlow<Boolean>
    val showNavigationBar: StateFlow<Boolean>
    val showDate: StateFlow<Boolean>
    val showClock: StateFlow<Boolean>
    val showClockFormat: StateFlow<Boolean>
    val showAlarm: StateFlow<Boolean>
    val showDailyWord: StateFlow<Boolean>
    val showFloating: StateFlow<Boolean>
    val showBattery: StateFlow<Boolean>
    val showBatteryIcon: StateFlow<Boolean>
    val showWeather: StateFlow<Boolean>
    val showBackground: StateFlow<Boolean>
    val gpsLocation: StateFlow<Boolean>
    val extendHomeAppsArea: StateFlow<Boolean>

    // --- Alignment ---
    val homeAlignment: StateFlow<Constants.Gravity>
    val homeAlignmentBottom: StateFlow<Boolean>
    val clockAlignment: StateFlow<Constants.Gravity>
    val dateAlignment: StateFlow<Constants.Gravity>
    val alarmAlignment: StateFlow<Constants.Gravity>
    val dailyWordAlignment: StateFlow<Constants.Gravity>
    val drawerAlignment: StateFlow<Constants.Gravity>

    // --- Gestures ---
    val shortSwipeUpAction: StateFlow<Constants.Action>
    val shortSwipeDownAction: StateFlow<Constants.Action>
    val shortSwipeLeftAction: StateFlow<Constants.Action>
    val shortSwipeRightAction: StateFlow<Constants.Action>
    val longSwipeUpAction: StateFlow<Constants.Action>
    val longSwipeDownAction: StateFlow<Constants.Action>
    val longSwipeLeftAction: StateFlow<Constants.Action>
    val longSwipeRightAction: StateFlow<Constants.Action>
    val clickClockAction: StateFlow<Constants.Action>
    val clickAppUsageAction: StateFlow<Constants.Action>
    val clickFloatingAction: StateFlow<Constants.Action>
    val clickDateAction: StateFlow<Constants.Action>
    val doubleTapAction: StateFlow<Constants.Action>

    // --- Enum settings ---
    val appTheme: StateFlow<Constants.Theme>
    val tempUnit: StateFlow<Constants.TempUnits>
    val appLanguage: StateFlow<Constants.Language>
    val searchEngines: StateFlow<Constants.SearchEngines>
    val fontFamily: StateFlow<Constants.FontFamily>
    val iconPackHome: StateFlow<Constants.IconPacks>
    val iconPackAppList: StateFlow<Constants.IconPacks>
    val customIconPackHome: StateFlow<String>
    val customIconPackAppList: StateFlow<String>

    // --- Voice ---
    val voiceEnabled: StateFlow<Boolean>
    val voiceCloudEnabled: StateFlow<Boolean>
    val voiceTtsEnabled: StateFlow<Boolean>
    val voiceHapticEnabled: StateFlow<Boolean>

    // --- Gesture app bindings ---
    fun getHomeAppModel(index: Int): AppListItem
    fun setHomeAppModel(index: Int, app: AppListItem)
    fun getSwipeApp(key: String): AppListItem
    fun setSwipeApp(key: String, app: AppListItem)

    // --- Notes / Messages ---
    fun saveMessages(messages: List<Message>)
    fun loadMessages(): List<Message>
    fun loadMessagesWrong(): List<MessageWrong>
    fun saveNotesSettings(category: String, priority: String)
    fun loadNotesSettings(): Pair<String, String>

    // --- Location / Weather ---
    fun saveLocation(result: LocationResult)
    fun loadLocation(): Pair<Double, Double>?
    fun loadLocationName(): String

    // --- Menu flags ---
    fun saveMenuFlags(key: String, flags: List<Boolean>)
    fun getMenuFlags(key: String, default: String = "0"): List<Boolean>

    // --- Import/Export ---
    fun saveToString(): String
    fun loadFromString(json: String)

    // --- Onboarding ---
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)

    // --- Generic setters (for all StateFlow-backed prefs) ---
    fun <T> update(property: StateFlow<T>, value: T)

    /** Observe a specific key change. */
    fun observeKey(key: String): Flow<Unit>
}
