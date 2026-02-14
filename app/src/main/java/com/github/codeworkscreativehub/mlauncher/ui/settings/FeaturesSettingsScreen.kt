package com.github.codeworkscreativehub.mlauncher.ui.settings

import android.Manifest
import android.content.Context
import android.os.UserManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.github.codeworkscreativehub.common.LauncherLocaleManager
import com.github.codeworkscreativehub.common.LocalizedResources
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.common.isGestureNavigationEnabled
import com.github.codeworkscreativehub.common.requestRuntimePermission
import com.github.codeworkscreativehub.mlauncher.MainActivity
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.AppCategory
import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.Constants
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.helper.emptyString
import com.github.codeworkscreativehub.mlauncher.helper.getTrueSystemFont
import com.github.codeworkscreativehub.mlauncher.helper.hasLocationPermission
import com.github.codeworkscreativehub.mlauncher.helper.reloadLauncher
import com.github.codeworkscreativehub.mlauncher.helper.updateHomeWidget
import com.github.codeworkscreativehub.mlauncher.helper.utils.AppReloader
import com.github.codeworkscreativehub.mlauncher.ui.BaseFragment
import com.github.codeworkscreativehub.mlauncher.ui.components.DialogManager
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsTitle

@Composable
fun FeaturesSettingsScreen(
    prefs: Prefs,
    titleFontSize: TextUnit,
    descriptionFontSize: TextUnit,
    iconSize: Dp,
    dialogBuilder: DialogManager,
    fragment: BaseFragment,
    onBack: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLaunchFontPicker: () -> Unit
) {
    val context = LocalContext.current

    // Remember state for all settings
    var selectedTheme by remember { mutableStateOf(prefs.appTheme) }
    var selectedLanguage by remember { mutableStateOf(prefs.appLanguage) }
    var selectedFontFamily by remember { mutableStateOf(prefs.fontFamily) }
    var toggledHideSearchView by remember { mutableStateOf(prefs.hideSearchView) }
    var toggledFloating by remember { mutableStateOf(prefs.showFloating) }

    var selectedSearchEngine by remember { mutableStateOf(prefs.searchEngines) }
    var toggledShowAZSidebar by remember { mutableStateOf(prefs.showAZSidebar) }
    var toggledAutoShowKeyboard by remember { mutableStateOf(prefs.autoShowKeyboard) }
    var toggledSearchFromStart by remember { mutableStateOf(prefs.searchFromStart) }
    var toggledEnableFilterStrength by remember { mutableStateOf(prefs.enableFilterStrength) }
    var selectedFilterStrength by remember { mutableIntStateOf(prefs.filterStrength) }

    var toggledAutoOpenApp by remember { mutableStateOf(prefs.autoOpenApp) }
    var toggledOpenAppOnEnter by remember { mutableStateOf(prefs.openAppOnEnter) }
    var toggledAppsLocked by remember { mutableStateOf(prefs.homeLocked) }
    var selectedHomeAppsNum by remember { mutableIntStateOf(prefs.homeAppsNum) }
    var selectedHomePagesNum by remember { mutableIntStateOf(prefs.homePagesNum) }
    var toggledHomePager by remember { mutableStateOf(prefs.homePager) }

    var toggledShowDate by remember { mutableStateOf(prefs.showDate) }
    var toggledShowClock by remember { mutableStateOf(prefs.showClock) }
    var toggledShowClockFormat by remember { mutableStateOf(prefs.showClockFormat) }
    var toggledShowAlarm by remember { mutableStateOf(prefs.showAlarm) }
    var toggledShowDailyWord by remember { mutableStateOf(prefs.showDailyWord) }
    var toggledShowBattery by remember { mutableStateOf(prefs.showBattery) }
    var toggledShowBatteryIcon by remember { mutableStateOf(prefs.showBatteryIcon) }
    var toggledShowWeather by remember { mutableStateOf(prefs.showWeather) }
    var toggledGPSLocation by remember { mutableStateOf(prefs.gpsLocation) }
    var selectedTempUnits by remember { mutableStateOf(prefs.tempUnit) }
    var selectedWeatherLocation by remember { mutableStateOf(prefs.loadLocationName()) }

    val contextMenuOptionLabels = listOf(
        getLocalizedString(R.string.pin),
        getLocalizedString(R.string.lock),
        getLocalizedString(R.string.hide),
        getLocalizedString(R.string.rename),
        getLocalizedString(R.string.tag),
        getLocalizedString(R.string.info),
        getLocalizedString(R.string.delete)
    )

    val homeButtonOptionLabels = listOf(
        getLocalizedString(R.string.home_button_phone),
        getLocalizedString(R.string.home_button_messages),
        getLocalizedString(R.string.home_button_camera),
        getLocalizedString(R.string.home_button_photos),
        getLocalizedString(R.string.home_button_web),
        getLocalizedString(R.string.home_button_settings),
        getLocalizedString(R.string.home_button_logo)
    )

    val appListButtonOptionLabels = listOf(
        getLocalizedString(R.string.applist_button_web),
        getLocalizedString(R.string.applist_button_contacts)
    )

    BackHandler { onBack() }

    PageHeader(
        iconRes = R.drawable.ic_back,
        title = getLocalizedString(R.string.features_settings_title),
        onClick = { onBack() }
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Personalization
    SettingsTitle(
        text = getLocalizedString(R.string.personalization),
        fontSize = titleFontSize
    )

    SettingsSelect(
        title = getLocalizedString(R.string.theme_mode),
        option = selectedTheme.getString(),
        fontSize = titleFontSize,
        onClick = {
            val themeEntries = Constants.Theme.entries

            val themeOptions = themeEntries.map { it.getString() }

            val selectedIndex = themeEntries.indexOf(selectedTheme).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheetPill(
                context = context,
                options = themeOptions.toTypedArray(),
                title = getLocalizedString(R.string.theme_mode),
                selectedIndex = selectedIndex,
                onItemSelected = { newThemeName ->
                    val newThemeIndex = themeOptions.indexOfFirst { it == newThemeName }
                    if (newThemeIndex != -1) {
                        val newTheme = themeEntries[newThemeIndex]
                        selectedTheme = newTheme
                        prefs.appTheme = newTheme
                        // resetThemeColors() would be called from parent fragment
                    }
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.app_language),
        option = selectedLanguage.getString(), // assuming selectedLanguage: Constants.Language
        fontSize = titleFontSize,
        onClick = {
            // Generate options
            val languageEntries = Constants.Language.entries

            val languageOptions = languageEntries.map { it.getString() } // get localized names to display

            // Determine selected index based on current prefs value
            val selectedIndex = languageEntries.indexOf(selectedLanguage).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = languageOptions.toTypedArray(),
                title = getLocalizedString(R.string.app_language),
                selectedIndex = selectedIndex,
                onItemSelected = { newLanguageName ->
                    // Find the actual enum by matching its localized name
                    val newLanguageIndex = languageOptions.indexOfFirst { it == newLanguageName }
                    if (newLanguageIndex != -1) {
                        val newLanguage = languageEntries[newLanguageIndex]
                        selectedLanguage = newLanguage // Update state
                        prefs.appLanguage = newLanguage // Persist in preferences
                        LauncherLocaleManager.updateLanguage(context, newLanguage)
                        LocalizedResources.invalidate()
                        reloadLauncher() // force reload with new language
                    }
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.font_family),
        option = selectedFontFamily.string(),
        fontSize = titleFontSize,
        onClick = {
            // Generate options and fonts
            val fontFamilyEntries = Constants.FontFamily.entries

            val fontFamilyOptions = fontFamilyEntries.map { it.getString() }
            val fontFamilyFonts = fontFamilyEntries.map {
                it.getFont(context) ?: getTrueSystemFont()
            }

            // Determine selected index based on current prefs value
            val selectedIndex = fontFamilyEntries.indexOf(selectedFontFamily).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = fontFamilyOptions.toTypedArray(),
                fonts = fontFamilyFonts,
                title = getLocalizedString(R.string.font_family),
                selectedIndex = selectedIndex,
                onItemSelected = { newFontFamilyName ->
                    val newFontFamilyIndex =
                        fontFamilyOptions.indexOfFirst { it == newFontFamilyName }
                    if (newFontFamilyIndex != -1) {
                        val newFontFamily = fontFamilyEntries[newFontFamilyIndex]
                        if (newFontFamily == Constants.FontFamily.Custom) {
                            // Show file picker and handle upload
                            onLaunchFontPicker()
                        } else {
                            selectedFontFamily = newFontFamily
                            prefs.fontFamily = newFontFamily
                            AppReloader.restartApp(context)
                        }
                    }
                }
            )
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // App List & Search
    SettingsTitle(
        text = getLocalizedString(R.string.search_and_app_list),
        fontSize = titleFontSize
    )
    SettingsSwitch(
        text = getLocalizedString(R.string.hide_search_view),
        fontSize = titleFontSize,
        defaultState = toggledHideSearchView,
        onCheckedChange = {
            toggledHideSearchView = !prefs.hideSearchView
            prefs.hideSearchView = toggledHideSearchView

            if (toggledHideSearchView) {
                toggledAutoShowKeyboard = false
                prefs.autoShowKeyboard = toggledAutoShowKeyboard
            }
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_floating_button),
        fontSize = titleFontSize,
        defaultState = toggledFloating,
        onCheckedChange = {
            toggledFloating = !prefs.showFloating
            prefs.showFloating = toggledFloating
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.search_engine),
        option = selectedSearchEngine.string(),
        fontSize = titleFontSize,
        onClick = {
            val searchEnginesEntries = Constants.SearchEngines.entries

            val searchEnginesOptions = searchEnginesEntries.map { it.getString() }

            // Determine selected index based on current prefs value
            val selectedIndex = searchEnginesEntries.indexOf(selectedSearchEngine).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = searchEnginesOptions.map { it }.toTypedArray(),
                title = getLocalizedString(R.string.search_engine),
                selectedIndex = selectedIndex,
                onItemSelected = { newSearchEngineName ->
                    val newFontFamilyIndex =
                        searchEnginesOptions.indexOfFirst { it == newSearchEngineName }
                    if (newFontFamilyIndex != -1) {
                        val newSearchEngine =
                            searchEnginesEntries[newFontFamilyIndex] // Get the selected FontFamily enum
                        selectedSearchEngine = newSearchEngine // Update state
                        prefs.searchEngines = newSearchEngine // Persist selection in preferences
                    }
                }
            )
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_az_sidebar),
        fontSize = titleFontSize,
        defaultState = toggledShowAZSidebar,
        onCheckedChange = {
            toggledShowAZSidebar = !prefs.showAZSidebar
            prefs.showAZSidebar = toggledShowAZSidebar
        }
    )

    if (!toggledHideSearchView) {
        SettingsSwitch(
            text = getLocalizedString(R.string.auto_show_keyboard),
            fontSize = titleFontSize,
            defaultState = toggledAutoShowKeyboard,
            onCheckedChange = {
                toggledAutoShowKeyboard = !prefs.autoShowKeyboard
                prefs.autoShowKeyboard = toggledAutoShowKeyboard
            }
        )
    }

    SettingsSwitch(
        text = getLocalizedString(R.string.enable_filter_strength),
        fontSize = titleFontSize,
        defaultState = toggledEnableFilterStrength,
        onCheckedChange = {
            toggledEnableFilterStrength = !prefs.enableFilterStrength
            prefs.enableFilterStrength = toggledEnableFilterStrength
        }
    )

    if (toggledEnableFilterStrength) {
        SettingsSwitch(
            text = getLocalizedString(R.string.search_from_start),
            fontSize = titleFontSize,
            defaultState = toggledSearchFromStart,
            onCheckedChange = {
                toggledSearchFromStart = !prefs.searchFromStart
                prefs.searchFromStart = toggledSearchFromStart
            }
        )
    }

    if (toggledEnableFilterStrength) {
        SettingsSelect(
            title = getLocalizedString(R.string.filter_strength),
            option = selectedFilterStrength.toString(),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showSliderBottomSheet(
                    context = context,
                    title = getLocalizedString(R.string.filter_strength),
                    minValue = Constants.MIN_FILTER_STRENGTH,
                    maxValue = Constants.MAX_FILTER_STRENGTH,
                    currentValue = prefs.filterStrength,
                    onValueSelected = { newFilterStrength ->
                        selectedFilterStrength = newFilterStrength.toInt() // Update state
                        prefs.filterStrength = newFilterStrength.toInt() // Persist selection in preferences
                        // viewModel.filterStrength.value = newFilterStrength.toInt()
                    }
                )
            }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Home Management
    SettingsTitle(
        text = getLocalizedString(R.string.home_management),
        fontSize = titleFontSize,
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.auto_open_apps),
        fontSize = titleFontSize,
        defaultState = toggledAutoOpenApp,
        onCheckedChange = {
            toggledAutoOpenApp = !prefs.autoOpenApp
            prefs.autoOpenApp = toggledAutoOpenApp
        }
    )


    SettingsSwitch(
        text = getLocalizedString(R.string.open_apps_on_enter),
        fontSize = titleFontSize,
        defaultState = toggledOpenAppOnEnter,
        onCheckedChange = {
            toggledOpenAppOnEnter = !prefs.openAppOnEnter
            prefs.openAppOnEnter = toggledOpenAppOnEnter
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.lock_home_apps),
        fontSize = titleFontSize,
        defaultState = toggledAppsLocked,
        onCheckedChange = {
            toggledAppsLocked = !prefs.homeLocked
            prefs.homeLocked = toggledAppsLocked
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.apps_on_home_screen),
        option = selectedHomeAppsNum.toString(),
        fontSize = titleFontSize,
        onClick = {
            Constants.updateMaxAppsBasedOnPages(context)
            val oldHomeAppsNum = selectedHomeAppsNum + 1
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.apps_on_home_screen),
                minValue = Constants.MIN_HOME_APPS,
                maxValue = Constants.MAX_HOME_APPS,
                currentValue = prefs.homeAppsNum,
                onValueSelected = { newHomeAppsNum ->
                    selectedHomeAppsNum = newHomeAppsNum.toInt() // Update state
                    prefs.homeAppsNum = newHomeAppsNum.toInt() // Persist selection in preferences
                    // viewModel.homeAppsNum.value = newHomeAppsNum.toInt()

                    // Check if homeAppsNum is less than homePagesNum and update homePagesNum accordingly
                    if (newHomeAppsNum in 1..<selectedHomePagesNum) {
                        selectedHomePagesNum = newHomeAppsNum.toInt()
                        prefs.homePagesNum = newHomeAppsNum.toInt() // Persist the new homePagesNum
                        // viewModel.homePagesNum.value = newHomeAppsNum.toInt()
                    }

                    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

                    val clearApp = AppListItem(
                        activityLabel = "Clear",
                        activityPackage = emptyString(),
                        activityClass = emptyString(),
                        user = userManager.userProfiles[0], // or use Process.myUserHandle() if it makes more sense
                        profileType = "SYSTEM",
                        customTag = emptyString(),
                        category = AppCategory.REGULAR
                    )

                    for (n in newHomeAppsNum.toInt()..oldHomeAppsNum) {
                        // i is outside the range between oldHomeAppsNum and newHomeAppsNum
                        // Do something with i
                        prefs.setHomeAppModel(n, clearApp)
                    }

                    // --- Trigger widget update ---
                    updateHomeWidget(context)
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.pages_on_home_screen),
        option = selectedHomePagesNum.toString(),
        fontSize = titleFontSize,
        onClick = {
            Constants.updateMaxHomePages(context)
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.pages_on_home_screen),
                minValue = Constants.MIN_HOME_PAGES,
                maxValue = Constants.MAX_HOME_PAGES,
                currentValue = prefs.homePagesNum,
                onValueSelected = { newHomePagesNum ->
                    selectedHomePagesNum = newHomePagesNum.toInt() // Update state
                    prefs.homePagesNum = newHomePagesNum.toInt() // Persist selection in preferences
                    // viewModel.homePagesNum.value = newHomePagesNum.toInt()
                }
            )
        }
    )

    if (selectedHomePagesNum > 1) {
        SettingsSwitch(
            text = getLocalizedString(R.string.enable_home_pager),
            fontSize = titleFontSize,
            defaultState = toggledHomePager,
            onCheckedChange = {
                toggledHomePager = !prefs.homePager
                prefs.homePager = toggledHomePager
            }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Toggles
    SettingsTitle(
        text = getLocalizedString(R.string.toggleable_items),
        fontSize = titleFontSize
    )

    val currentContextMenuFlags = remember {
        mutableStateListOf<Boolean>().apply {
            addAll(prefs.getMenuFlags("CONTEXT_MENU_FLAGS", "0011111"))
        }
    }

    val enabledContextMenuOptions by remember {
        derivedStateOf {
            contextMenuOptionLabels
                .take(currentContextMenuFlags.size)
                .zip(currentContextMenuFlags)
                .filter { it.second }
                .joinToString(", ") { it.first }
                .ifEmpty { getLocalizedString(R.string.none) }
        }
    }

    SettingsSelect(
        title = getLocalizedString(R.string.settings_context_menu_title),
        option = enabledContextMenuOptions,
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showFlagSettingsBottomSheet(
                context,
                contextMenuOptionLabels,
                "CONTEXT_MENU_FLAGS",
                "0011111"
            ) { updatedFlags: List<Boolean> ->
                currentContextMenuFlags.clear()
                currentContextMenuFlags.addAll(updatedFlags)
            }
        }
    )

    val currentAppListFlags = remember {
        mutableStateListOf<Boolean>().apply {
            addAll(prefs.getMenuFlags("APPLIST_BUTTON_FLAGS", "00"))
        }
    }

    val enabledAppListOptions by remember {
        derivedStateOf {
            appListButtonOptionLabels
                .take(currentAppListFlags.size)
                .zip(currentAppListFlags)
                .filter { it.second }
                .joinToString(", ") { it.first }
                .ifEmpty { getLocalizedString(R.string.none) }
        }
    }

    SettingsSelect(
        title = getLocalizedString(R.string.settings_applist_buttons_title),
        option = enabledAppListOptions,
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showFlagSettingsBottomSheet(
                context,
                appListButtonOptionLabels,
                "APPLIST_BUTTON_FLAGS",
                "00"
            ) { updatedFlags: List<Boolean> ->
                currentAppListFlags.clear()
                currentAppListFlags.addAll(updatedFlags)
            }
        }
    )

    val currentHomeButtonFlags = remember {
        mutableStateListOf<Boolean>().apply {
            addAll(prefs.getMenuFlags("HOME_BUTTON_FLAGS", "0000011"))
        }
    }

    val enabledHomeButtonOptions by remember {
        derivedStateOf {
            homeButtonOptionLabels
                .take(currentHomeButtonFlags.size)
                .zip(currentHomeButtonFlags)
                .filter { it.second }
                .joinToString(", ") { it.first }
                .ifEmpty { getLocalizedString(R.string.none) }
        }
    }
    SettingsSelect(
        title = getLocalizedString(R.string.settings_home_buttons_title),
        option = enabledHomeButtonOptions,
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showFlagSettingsBottomSheet(
                context,
                homeButtonOptionLabels,
                "HOME_BUTTON_FLAGS",
                "0000011"
            ) { updatedFlags: List<Boolean> ->
                currentHomeButtonFlags.clear()
                currentHomeButtonFlags.addAll(updatedFlags)
            }
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Info Tiles
    SettingsTitle(
        text = getLocalizedString(R.string.info_tiles),
        fontSize = titleFontSize,
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_date),
        fontSize = titleFontSize,
        defaultState = toggledShowDate,
        onCheckedChange = {
            toggledShowDate = !prefs.showDate
            prefs.showDate = toggledShowDate
            // viewModel.setShowDate(prefs.showDate)
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_clock),
        fontSize = titleFontSize,
        defaultState = toggledShowClock,
        onCheckedChange = {
            toggledShowClock = !prefs.showClock
            prefs.showClock = toggledShowClock
            // viewModel.setShowClock(prefs.showClock)
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_clock_format),
        fontSize = titleFontSize,
        defaultState = toggledShowClockFormat,
        onCheckedChange = {
            toggledShowClockFormat = !prefs.showClockFormat
            prefs.showClockFormat = toggledShowClockFormat
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_alarm),
        fontSize = titleFontSize,
        defaultState = toggledShowAlarm,
        onCheckedChange = {
            toggledShowAlarm = !prefs.showAlarm
            prefs.showAlarm = toggledShowAlarm
            // viewModel.setShowAlarm(prefs.showAlarm)
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_daily_word),
        fontSize = titleFontSize,
        defaultState = toggledShowDailyWord,
        onCheckedChange = {
            toggledShowDailyWord = !prefs.showDailyWord
            prefs.showDailyWord = toggledShowDailyWord
            // viewModel.setShowDailyWord(prefs.showDailyWord)
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_battery),
        fontSize = titleFontSize,
        defaultState = toggledShowBattery,
        onCheckedChange = {
            toggledShowBattery = !prefs.showBattery
            prefs.showBattery = toggledShowBattery
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_battery_icon),
        fontSize = titleFontSize,
        defaultState = toggledShowBatteryIcon,
        onCheckedChange = {
            toggledShowBatteryIcon = !prefs.showBatteryIcon
            prefs.showBatteryIcon = toggledShowBatteryIcon
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Weather
    SettingsTitle(
        text = getLocalizedString(R.string.weather),
        fontSize = titleFontSize,
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_weather),
        fontSize = titleFontSize,
        defaultState = toggledShowWeather,
        onCheckedChange = {
            toggledShowWeather = !prefs.showWeather
            prefs.showWeather = toggledShowWeather
        }
    )

    if (toggledShowWeather) {
        SettingsSwitch(
            text = getLocalizedString(R.string.gps_location),
            fontSize = titleFontSize,
            defaultState = toggledGPSLocation,
            onCheckedChange = {
                toggledGPSLocation = !prefs.gpsLocation
                prefs.gpsLocation = toggledGPSLocation

                if (toggledGPSLocation && !hasLocationPermission(context)) {
                    context.requestRuntimePermission(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        Constants.ACCESS_FINE_LOCATION,
                        "Location"
                    )
                }
            }
        )
    }

    if (toggledShowWeather) {
        SettingsSelect(
            title = getLocalizedString(R.string.temp_unit),
            option = selectedTempUnits.string(),
            fontSize = titleFontSize,
            onClick = {
                val tempUnitsOptions = Constants.TempUnits.entries.toTypedArray()
                val selectedIndex = tempUnitsOptions.indexOf(selectedTempUnits).takeIf { it >= 0 } ?: 1

                dialogBuilder.showSingleChoiceBottomSheetPill(
                    context = context,
                    options = tempUnitsOptions,
                    title = getLocalizedString(R.string.temp_unit),
                    selectedIndex = selectedIndex,
                    onItemSelected = { newTempUnit ->
                        selectedTempUnits = newTempUnit // Update state
                        prefs.tempUnit = selectedTempUnits // Persist selection in preferences
                    }
                )
            }
        )
    }

    if (toggledShowWeather && !toggledGPSLocation) {
        SettingsSelect(
            title = getLocalizedString(R.string.manual_location),
            option = selectedWeatherLocation,
            fontSize = titleFontSize,
            onClick = {
                onNavigateToLocationSearch()
            }
        )
    }

    if (isGestureNavigationEnabled(context)) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_gesture_nav)))
    } else {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_3_button_nav)))
    }
}
