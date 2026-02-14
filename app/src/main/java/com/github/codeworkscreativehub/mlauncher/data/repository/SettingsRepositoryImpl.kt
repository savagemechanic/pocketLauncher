package com.github.codeworkscreativehub.mlauncher.data.repository

import android.content.Context
import android.os.UserHandle
import androidx.core.content.ContextCompat.getColor
import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.Constants
import com.github.codeworkscreativehub.mlauncher.data.Message
import com.github.codeworkscreativehub.mlauncher.data.MessageWrong
import com.github.codeworkscreativehub.mlauncher.data.datasource.MoshiSerializer
import com.github.codeworkscreativehub.mlauncher.data.datasource.PreferencesDataSource
import com.github.codeworkscreativehub.mlauncher.helper.emptyString
import com.github.codeworkscreativehub.mlauncher.helper.getUserHandleFromString
import com.github.codeworkscreativehub.mlauncher.helper.receivers.LocationResult
import com.github.codeworkscreativehub.mlauncher.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsRepositoryImpl(
    private val dataSource: PreferencesDataSource,
    private val serializer: MoshiSerializer,
    private val context: Context,
    scope: CoroutineScope
) : SettingsRepository {

    // --- StateFlow-backed property map for generic update ---
    private val flowToKey = mutableMapOf<StateFlow<*>, String>()

    private fun <T> stateFlow(key: String, initial: T): StateFlow<T> {
        val flow = MutableStateFlow(initial)
        flowToKey[flow] = key
        return flow.asStateFlow()
    }

    private inline fun <reified T : Enum<T>> enumState(key: String, default: T): StateFlow<T> {
        val name = dataSource.getString(key, default.name)
        val value = runCatching { enumValueOf<T>(name) }.getOrDefault(default)
        val flow = MutableStateFlow(value)
        flowToKey[flow] = key
        return flow.asStateFlow()
    }

    // --- General ---
    override val appVersion = stateFlow(APP_VERSION, dataSource.getInt(APP_VERSION, -1))
    override val firstOpen = stateFlow(FIRST_OPEN, dataSource.getBoolean(FIRST_OPEN, true))
    override val firstSettingsOpen = stateFlow(FIRST_SETTINGS_OPEN, dataSource.getBoolean(FIRST_SETTINGS_OPEN, true))
    override val autoOpenApp = stateFlow(AUTO_OPEN_APP, dataSource.getBoolean(AUTO_OPEN_APP, false))
    override val forceWallpaper = stateFlow(FORCE_COLORED_WALLPAPER, dataSource.getBoolean(FORCE_COLORED_WALLPAPER, false))
    override val openAppOnEnter = stateFlow(OPEN_APP_ON_ENTER, dataSource.getBoolean(OPEN_APP_ON_ENTER, false))
    override val homePager = stateFlow(HOME_PAGES_PAGER, dataSource.getBoolean(HOME_PAGES_PAGER, false))
    override val recentAppsDisplayed = stateFlow(RECENT_APPS_DISPLAYED, dataSource.getBoolean(RECENT_APPS_DISPLAYED, false))
    override val iconRainbowColors = stateFlow(ICON_RAINBOW_COLORS, dataSource.getBoolean(ICON_RAINBOW_COLORS, false))
    override val recentCounter = stateFlow(RECENT_COUNTER, dataSource.getInt(RECENT_COUNTER, 10))
    override val enableFilterStrength = stateFlow(ENABLE_FILTER_STRENGTH, dataSource.getBoolean(ENABLE_FILTER_STRENGTH, true))
    override val filterStrength = stateFlow(FILTER_STRENGTH, dataSource.getInt(FILTER_STRENGTH, 25))
    override val shortSwipeThreshold = stateFlow(SHORT_SWIPE_THRESHOLD, dataSource.getFloat(SHORT_SWIPE_THRESHOLD, 0.15f))
    override val longSwipeThreshold = stateFlow(LONG_SWIPE_THRESHOLD, dataSource.getFloat(LONG_SWIPE_THRESHOLD, 0.4f))
    override val searchFromStart = stateFlow(SEARCH_START, dataSource.getBoolean(SEARCH_START, false))
    override val autoShowKeyboard = stateFlow(AUTO_SHOW_KEYBOARD, dataSource.getBoolean(AUTO_SHOW_KEYBOARD, true))
    override val homeAppsNum = stateFlow(HOME_APPS_NUM, dataSource.getInt(HOME_APPS_NUM, 4))
    override val homePagesNum = stateFlow(HOME_PAGES_NUM, dataSource.getInt(HOME_PAGES_NUM, 1))
    override val opacityNum = stateFlow(APP_OPACITY, dataSource.getInt(APP_OPACITY, 15))
    override val appUsageStats = stateFlow(APP_USAGE_STATS, dataSource.getBoolean(APP_USAGE_STATS, false))
    override val lockOrientation = stateFlow(LOCK_ORIENTATION, dataSource.getBoolean(LOCK_ORIENTATION, false))
    override val lockOrientationPortrait = stateFlow(LOCK_ORIENTATION_PORTRAIT, dataSource.getBoolean(LOCK_ORIENTATION_PORTRAIT, true))
    override val hapticFeedback = stateFlow(HAPTIC_FEEDBACK, dataSource.getBoolean(HAPTIC_FEEDBACK, true))
    override val showAZSidebar = stateFlow(SHOW_AZSIDEBAR, dataSource.getBoolean(SHOW_AZSIDEBAR, false))
    override val homeLocked = stateFlow(HOME_LOCKED, dataSource.getBoolean(HOME_LOCKED, false))
    override val settingsLocked = stateFlow(SETTINGS_LOCKED, dataSource.getBoolean(SETTINGS_LOCKED, false))
    override val hideSearchView = stateFlow(HIDE_SEARCH_VIEW, dataSource.getBoolean(HIDE_SEARCH_VIEW, false))
    override val autoExpandNotes = stateFlow(AUTO_EXPAND_NOTES, dataSource.getBoolean(AUTO_EXPAND_NOTES, false))
    override val clickToEditDelete = stateFlow(CLICK_EDIT_DELETE, dataSource.getBoolean(CLICK_EDIT_DELETE, true))
    override val enableExpertOptions = stateFlow(EXPERT_OPTIONS, dataSource.getBoolean(EXPERT_OPTIONS, false))
    override val wordList = stateFlow(WORD_LIST, dataSource.getString(WORD_LIST, emptyString()))

    // --- Display ---
    override val showStatusBar = stateFlow(STATUS_BAR, dataSource.getBoolean(STATUS_BAR, true))
    override val showNavigationBar = stateFlow(NAVIGATION_BAR, dataSource.getBoolean(NAVIGATION_BAR, true))
    override val showDate = stateFlow(SHOW_DATE, dataSource.getBoolean(SHOW_DATE, true))
    override val showClock = stateFlow(SHOW_CLOCK, dataSource.getBoolean(SHOW_CLOCK, true))
    override val showClockFormat = stateFlow(SHOW_CLOCK_FORMAT, dataSource.getBoolean(SHOW_CLOCK_FORMAT, true))
    override val showAlarm = stateFlow(SHOW_ALARM, dataSource.getBoolean(SHOW_ALARM, false))
    override val showDailyWord = stateFlow(SHOW_DAILY_WORD, dataSource.getBoolean(SHOW_DAILY_WORD, false))
    override val showFloating = stateFlow(SHOW_FLOATING, dataSource.getBoolean(SHOW_FLOATING, true))
    override val showBattery = stateFlow(SHOW_BATTERY, dataSource.getBoolean(SHOW_BATTERY, true))
    override val showBatteryIcon = stateFlow(SHOW_BATTERY_ICON, dataSource.getBoolean(SHOW_BATTERY_ICON, true))
    override val showWeather = stateFlow(SHOW_WEATHER, dataSource.getBoolean(SHOW_WEATHER, true))
    override val showBackground = stateFlow(SHOW_BACKGROUND, dataSource.getBoolean(SHOW_BACKGROUND, false))
    override val gpsLocation = stateFlow(GPS_LOCATION, dataSource.getBoolean(GPS_LOCATION, true))
    override val extendHomeAppsArea = stateFlow(HOME_CLICK_AREA, dataSource.getBoolean(HOME_CLICK_AREA, false))

    // --- Alignment ---
    override val homeAlignment = enumState(HOME_ALIGNMENT, Constants.Gravity.Left)
    override val homeAlignmentBottom = stateFlow(HOME_ALIGNMENT_BOTTOM, dataSource.getBoolean(HOME_ALIGNMENT_BOTTOM, true))
    override val clockAlignment = enumState(CLOCK_ALIGNMENT, Constants.Gravity.Left)
    override val dateAlignment = enumState(DATE_ALIGNMENT, Constants.Gravity.Left)
    override val alarmAlignment = enumState(ALARM_ALIGNMENT, Constants.Gravity.Left)
    override val dailyWordAlignment = enumState(DAILY_WORD_ALIGNMENT, Constants.Gravity.Left)
    override val drawerAlignment = enumState(DRAWER_ALIGNMENT, Constants.Gravity.Right)

    // --- Gestures ---
    override val shortSwipeUpAction = enumState(SWIPE_UP_ACTION, Constants.Action.ShowAppList)
    override val shortSwipeDownAction = enumState(SWIPE_DOWN_ACTION, Constants.Action.ShowNotification)
    override val shortSwipeLeftAction = enumState(SWIPE_LEFT_ACTION, Constants.Action.OpenApp)
    override val shortSwipeRightAction = enumState(SWIPE_RIGHT_ACTION, Constants.Action.OpenApp)
    override val longSwipeUpAction = enumState(LONG_SWIPE_UP_ACTION, Constants.Action.ShowAppList)
    override val longSwipeDownAction = enumState(LONG_SWIPE_DOWN_ACTION, Constants.Action.ShowNotification)
    override val longSwipeLeftAction = enumState(LONG_SWIPE_LEFT_ACTION, Constants.Action.PreviousPage)
    override val longSwipeRightAction = enumState(LONG_SWIPE_RIGHT_ACTION, Constants.Action.NextPage)
    override val clickClockAction = enumState(CLICK_CLOCK_ACTION, Constants.Action.OpenApp)
    override val clickAppUsageAction = enumState(CLICK_APP_USAGE_ACTION, Constants.Action.ShowDigitalWellbeing)
    override val clickFloatingAction = enumState(CLICK_FLOATING_ACTION, Constants.Action.ShowNotesManager)
    override val clickDateAction = enumState(CLICK_DATE_ACTION, Constants.Action.OpenApp)
    override val doubleTapAction = enumState(DOUBLE_TAP_ACTION, Constants.Action.LockScreen)

    // --- Enum settings ---
    override val appTheme = enumState(APP_THEME, Constants.Theme.System)
    override val tempUnit = enumState(TEMP_UNIT, Constants.TempUnits.Celsius)
    override val appLanguage = enumState(APP_LANGUAGE, Constants.Language.System)
    override val searchEngines = enumState(SEARCH_ENGINE, Constants.SearchEngines.Google)
    override val fontFamily = enumState(LAUNCHER_FONT, Constants.FontFamily.System)
    override val iconPackHome = enumState(ICON_PACK_HOME, Constants.IconPacks.Disabled)
    override val iconPackAppList = enumState(ICON_PACK_APP_LIST, Constants.IconPacks.Disabled)
    override val customIconPackHome = stateFlow(CUSTOM_ICON_PACK_HOME, dataSource.getString(CUSTOM_ICON_PACK_HOME, emptyString()))
    override val customIconPackAppList = stateFlow(CUSTOM_ICON_PACK_APP_LIST, dataSource.getString(CUSTOM_ICON_PACK_APP_LIST, emptyString()))

    // --- Voice ---
    override val voiceEnabled = stateFlow(VOICE_ENABLED, dataSource.getBoolean(VOICE_ENABLED, true))
    override val voiceCloudEnabled = stateFlow(VOICE_CLOUD_ENABLED, dataSource.getBoolean(VOICE_CLOUD_ENABLED, false))
    override val voiceTtsEnabled = stateFlow(VOICE_TTS_ENABLED, dataSource.getBoolean(VOICE_TTS_ENABLED, true))
    override val voiceHapticEnabled = stateFlow(VOICE_HAPTIC_ENABLED, dataSource.getBoolean(VOICE_HAPTIC_ENABLED, true))

    init {
        // Observe changes from SharedPreferences and update StateFlows
        scope.launch {
            dataSource.observeChanges().collect { key ->
                updateFlowForKey(key)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateFlowForKey(key: String) {
        flowToKey.entries.find { it.value == key }?.let { (flow, _) ->
            val mutable = flow as? MutableStateFlow<Any?> ?: return
            val current = mutable.value
            mutable.value = when (current) {
                is Boolean -> dataSource.getBoolean(key, current)
                is Int -> dataSource.getInt(key, current)
                is Float -> dataSource.getFloat(key, current)
                is String -> dataSource.getString(key, current)
                is Enum<*> -> reloadEnum(key, current)
                else -> return
            }
        }
    }

    private fun reloadEnum(key: String, current: Enum<*>): Enum<*> {
        val name = dataSource.getString(key, current.name)
        return current.javaClass.enumConstants?.find { it.name == name } ?: current
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> update(property: StateFlow<T>, value: T) {
        val key = flowToKey[property] ?: return
        val mutableFlow = property as? MutableStateFlow<T> ?: return
        when (value) {
            is Boolean -> dataSource.putBoolean(key, value)
            is Int -> dataSource.putInt(key, value)
            is Float -> dataSource.putFloat(key, value)
            is String -> dataSource.putString(key, value)
            is Enum<*> -> dataSource.putString(key, value.name)
        }
        mutableFlow.value = value
    }

    // --- Home app model ---
    override fun getHomeAppModel(index: Int): AppListItem = loadApp("$index")

    override fun setHomeAppModel(index: Int, app: AppListItem) = storeApp("$index", app)

    override fun getSwipeApp(key: String): AppListItem = loadApp(key)

    override fun setSwipeApp(key: String, app: AppListItem) = storeApp(key, app)

    private fun loadApp(id: String): AppListItem {
        val appName = dataSource.getString("${APP_NAME}_$id", emptyString())
        val appPackage = dataSource.getString("${APP_PACKAGE}_$id", emptyString())
        val appActivity = dataSource.getString("${APP_ACTIVITY}_$id", emptyString())
        val userHandleString = runCatching {
            dataSource.getString("${APP_USER}_$id", emptyString())
        }.getOrDefault(emptyString())
        val userHandle: UserHandle = getUserHandleFromString(context, userHandleString)
        return AppListItem(
            activityLabel = appName,
            activityPackage = appPackage,
            customTag = emptyString(),
            activityClass = appActivity,
            user = userHandle,
        )
    }

    private fun storeApp(id: String, app: AppListItem) {
        if (app.activityPackage.isNotEmpty() && app.activityClass.isNotEmpty()) {
            dataSource.putString("${APP_NAME}_$id", app.activityLabel)
            dataSource.putString("${APP_PACKAGE}_$id", app.activityPackage)
            dataSource.putString("${APP_ACTIVITY}_$id", app.activityClass)
            dataSource.putString("${APP_USER}_$id", app.user.toString())
        } else {
            dataSource.remove("${APP_NAME}_$id")
            dataSource.remove("${APP_PACKAGE}_$id")
            dataSource.remove("${APP_ACTIVITY}_$id")
            dataSource.remove("${APP_USER}_$id")
        }
    }

    // --- Notes / Messages ---
    override fun saveMessages(messages: List<Message>) {
        val json = serializer.serializeMessages(messages)
        dataSource.putString(NOTES_MESSAGES, json)
    }

    override fun loadMessages(): List<Message> {
        val json = dataSource.getString(NOTES_MESSAGES, "[]")
        return serializer.deserializeMessages(json)
    }

    override fun loadMessagesWrong(): List<MessageWrong> {
        val json = dataSource.getString(NOTES_MESSAGES, "[]")
        return serializer.deserializeMessagesWrong(json)
    }

    override fun saveNotesSettings(category: String, priority: String) {
        dataSource.putString(NOTES_CATEGORY, category)
        dataSource.putString(NOTES_PRIORITY, priority)
    }

    override fun loadNotesSettings(): Pair<String, String> {
        val category = dataSource.getString(NOTES_CATEGORY, "None")
        val priority = dataSource.getString(NOTES_PRIORITY, "None")
        return Pair(category, priority)
    }

    // --- Location ---
    override fun saveLocation(result: LocationResult) {
        dataSource.putString(WEATHER_LOCATION, result.region)
        dataSource.putFloat(WEATHER_LATITUDE, result.latitude.toFloat())
        dataSource.putFloat(WEATHER_LONGITUDE, result.longitude.toFloat())
    }

    override fun loadLocation(): Pair<Double, Double>? {
        val lat = dataSource.getFloat(WEATHER_LATITUDE, Float.NaN)
        val lon = dataSource.getFloat(WEATHER_LONGITUDE, Float.NaN)
        return if (!lat.isNaN() && !lon.isNaN()) Pair(lat.toDouble(), lon.toDouble()) else null
    }

    override fun loadLocationName(): String =
        dataSource.getString(WEATHER_LOCATION, "Select Location")

    // --- Menu flags ---
    override fun saveMenuFlags(key: String, flags: List<Boolean>) {
        val flagString = flags.joinToString("") { if (it) "1" else "0" }
        dataSource.putString(key, flagString)
    }

    override fun getMenuFlags(key: String, default: String): List<Boolean> {
        val flagString = dataSource.getString(key, default)
        return flagString.map { it == '1' }
    }

    // --- Import/Export ---
    override fun saveToString(): String =
        serializer.serializePrefsToJson(dataSource.all())

    override fun loadFromString(json: String) {
        val all = serializer.deserializePrefsFromJson(json)
        for ((key, value) in all) {
            when (value) {
                is String -> dataSource.putString(key, value)
                is Boolean -> dataSource.putBoolean(key, value)
                is Double -> {
                    if (value % 1 == 0.0) dataSource.putInt(key, value.toInt())
                    else dataSource.putFloat(key, value.toFloat())
                }
                is List<*> -> {
                    val stringSet = value.filterIsInstance<String>().toSet()
                    dataSource.putStringSet(key, stringSet)
                }
                else -> {}
            }
        }
    }

    // --- Onboarding ---
    override fun isOnboardingCompleted(): Boolean =
        dataSource.getBoolean(ONBOARDING_COMPLETED, false)

    override fun setOnboardingCompleted(completed: Boolean) {
        dataSource.putBoolean(ONBOARDING_COMPLETED, completed)
    }

    override fun observeKey(key: String): Flow<Unit> =
        dataSource.observeChanges().filter { it == key }.map {}
}
