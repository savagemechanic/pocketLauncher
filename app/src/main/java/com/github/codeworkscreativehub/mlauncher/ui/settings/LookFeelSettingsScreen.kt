package com.github.codeworkscreativehub.mlauncher.ui.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.common.isGestureNavigationEnabled
import com.github.codeworkscreativehub.mlauncher.MainViewModel
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.Constants
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.helper.IconCacheTarget
import com.github.codeworkscreativehub.mlauncher.helper.emptyString
import com.github.codeworkscreativehub.mlauncher.helper.hideNavigationBar
import com.github.codeworkscreativehub.mlauncher.helper.hideStatusBar
import com.github.codeworkscreativehub.mlauncher.helper.showNavigationBar
import com.github.codeworkscreativehub.mlauncher.helper.showStatusBar
import com.github.codeworkscreativehub.mlauncher.helper.updateHomeWidget
import com.github.codeworkscreativehub.mlauncher.ui.BaseFragment
import com.github.codeworkscreativehub.mlauncher.ui.components.DialogManager
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsTitle
import com.github.codeworkscreativehub.mlauncher.ui.iconpack.CustomIconSelectionActivity

@Composable
fun LookFeelSettingsScreen(
    prefs: Prefs,
    titleFontSize: TextUnit,
    descriptionFontSize: TextUnit,
    iconSize: Dp,
    dialogBuilder: DialogManager,
    fragment: BaseFragment,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Look & Feel Settings State Variables
    var selectedAppSize by remember { mutableIntStateOf(prefs.appSize) }
    var selectedDateSize by remember { mutableIntStateOf(prefs.dateSize) }
    var selectedClockSize by remember { mutableIntStateOf(prefs.clockSize) }
    var selectedAlarmSize by remember { mutableIntStateOf(prefs.alarmSize) }
    var selectedDailyWordSize by remember { mutableIntStateOf(prefs.dailyWordSize) }
    var selectedBatterySize by remember { mutableIntStateOf(prefs.batterySize) }

    var selectedPaddingSize by remember { mutableIntStateOf(prefs.textPaddingSize) }
    var toggledExtendHomeAppsArea by remember { mutableStateOf(prefs.extendHomeAppsArea) }
    var toggledHomeAlignmentBottom by remember { mutableStateOf(prefs.homeAlignmentBottom) }

    var toggledShowStatusBar by remember { mutableStateOf(prefs.showStatusBar) }
    var toggledShowNavigationBar by remember { mutableStateOf(prefs.showNavigationBar) }
    var toggledRecentAppsDisplayed by remember { mutableStateOf(prefs.recentAppsDisplayed) }
    var selectedRecentCounter by remember { mutableIntStateOf(prefs.recentCounter) }
    var toggledRecentAppUsageStats by remember { mutableStateOf(prefs.appUsageStats) }
    var selectedIconPackHome by remember { mutableStateOf(prefs.iconPackHome) }
    var selectedIconPackAppList by remember { mutableStateOf(prefs.iconPackAppList) }
    var toggledShowBackground by remember { mutableStateOf(prefs.showBackground) }
    var selectedBackgroundOpacity by remember { mutableIntStateOf(prefs.opacityNum) }

    var selectedHomeAlignment by remember { mutableStateOf(prefs.homeAlignment) }
    var selectedClockAlignment by remember { mutableStateOf(prefs.clockAlignment) }
    var selectedDateAlignment by remember { mutableStateOf(prefs.dateAlignment) }
    var selectedAlarmAlignment by remember { mutableStateOf(prefs.alarmAlignment) }
    var selectedDailyWordAlignment by remember { mutableStateOf(prefs.dailyWordAlignment) }
    var selectedDrawAlignment by remember { mutableStateOf(prefs.drawerAlignment) }

    var selectedBackgroundColor by remember { mutableIntStateOf(prefs.backgroundColor) }
    var selectedAppColor by remember { mutableIntStateOf(prefs.appColor) }
    var selectedDateColor by remember { mutableIntStateOf(prefs.dateColor) }
    var selectedClockColor by remember { mutableIntStateOf(prefs.clockColor) }
    var selectedAlarmColor by remember { mutableIntStateOf(prefs.alarmClockColor) }
    var selectedDailyWordColor by remember { mutableIntStateOf(prefs.dailyWordColor) }
    var selectedBatteryColor by remember { mutableIntStateOf(prefs.batteryColor) }
    var toggledIconRainbowColors by remember { mutableStateOf(prefs.iconRainbowColors) }
    var selectedShortcutIconsColor by remember { mutableIntStateOf(prefs.shortcutIconsColor) }

    BackHandler {
        onBack()
    }

    PageHeader(
        iconRes = R.drawable.ic_back,
        title = getLocalizedString(R.string.look_feel_settings_title),
        onClick = {
            onBack()
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Layout & Density
    SettingsTitle(
        text = getLocalizedString(R.string.layout_positioning),
        fontSize = titleFontSize
    )
    SettingsSelect(
        title = getLocalizedString(R.string.app_padding_size),
        option = selectedPaddingSize.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.app_padding_size),
                minValue = Constants.MIN_TEXT_PADDING,
                maxValue = Constants.MAX_TEXT_PADDING,
                currentValue = prefs.textPaddingSize,
                onValueSelected = { newPaddingSize ->
                    selectedPaddingSize = newPaddingSize.toInt() // Update state
                    prefs.textPaddingSize = newPaddingSize.toInt() // Persist selection in preferences
                }
            )
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.extend_home_apps_area),
        fontSize = titleFontSize,
        defaultState = toggledExtendHomeAppsArea,
        onCheckedChange = {
            toggledExtendHomeAppsArea = !prefs.extendHomeAppsArea
            prefs.extendHomeAppsArea = toggledExtendHomeAppsArea
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.alignment_to_bottom),
        fontSize = titleFontSize,
        defaultState = toggledHomeAlignmentBottom,
        onCheckedChange = {
            toggledHomeAlignmentBottom = !prefs.homeAlignmentBottom
            prefs.homeAlignmentBottom = toggledHomeAlignmentBottom
            viewModel.updateHomeAppsAlignment(
                prefs.homeAlignment,
                prefs.homeAlignmentBottom
            )
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Visibility & Display
    SettingsTitle(
        text = getLocalizedString(R.string.visibility_display),
        fontSize = titleFontSize,
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_status_bar),
        fontSize = titleFontSize,
        defaultState = toggledShowStatusBar,
        onCheckedChange = {
            toggledShowStatusBar = !prefs.showStatusBar
            prefs.showStatusBar = toggledShowStatusBar
            if (toggledShowStatusBar) showStatusBar(fragment.requireActivity().window)
            else hideStatusBar(fragment.requireActivity().window)
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_navigation_bar),
        fontSize = titleFontSize,
        defaultState = toggledShowNavigationBar,
        onCheckedChange = {
            toggledShowNavigationBar = !prefs.showNavigationBar
            prefs.showNavigationBar = toggledShowNavigationBar
            if (toggledShowNavigationBar) showNavigationBar(fragment.requireActivity().window)
            else hideNavigationBar(fragment.requireActivity().window)
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_recent_apps),
        fontSize = titleFontSize,
        defaultState = toggledRecentAppsDisplayed,
        onCheckedChange = {
            toggledRecentAppsDisplayed = !prefs.recentAppsDisplayed
            prefs.recentAppsDisplayed = toggledRecentAppsDisplayed
        }
    )

    if (toggledRecentAppsDisplayed) {
        SettingsSelect(
            title = getLocalizedString(R.string.number_of_recents),
            option = selectedRecentCounter.toString(),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showSliderBottomSheet(
                    context = context,
                    title = getLocalizedString(R.string.number_of_recents),
                    minValue = Constants.MIN_RECENT_COUNTER,
                    maxValue = Constants.MAX_RECENT_COUNTER,
                    currentValue = prefs.recentCounter,
                    onValueSelected = { newRecentCounter ->
                        selectedRecentCounter = newRecentCounter.toInt() // Update state
                        prefs.recentCounter = newRecentCounter.toInt() // Persist selection in preferences
                        viewModel.recentCounter.value = newRecentCounter.toInt()
                    }
                )
            }
        )
    }

    SettingsSwitch(
        text = getLocalizedString(R.string.show_app_usage_stats),
        fontSize = titleFontSize,
        defaultState = toggledRecentAppUsageStats,
        onCheckedChange = {
            toggledRecentAppUsageStats = !prefs.appUsageStats
            prefs.appUsageStats = toggledRecentAppUsageStats
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.show_background),
        fontSize = titleFontSize,
        defaultState = toggledShowBackground,
        onCheckedChange = {
            toggledShowBackground = !prefs.showBackground
            prefs.showBackground = toggledShowBackground
        }
    )
    if (!toggledShowBackground) {
        SettingsSelect(
            title = getLocalizedString(R.string.background_opacity),
            option = selectedBackgroundOpacity.toString(),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showSliderBottomSheet(
                    context = context,
                    title = getLocalizedString(R.string.background_opacity),
                    minValue = Constants.MIN_OPACITY,
                    maxValue = Constants.MAX_OPACITY,
                    currentValue = prefs.opacityNum,
                    onValueSelected = { newBackgroundOpacity ->
                        selectedBackgroundOpacity = newBackgroundOpacity.toInt() // Update state
                        prefs.opacityNum = newBackgroundOpacity.toInt() // Persist selection in preferences
                        viewModel.opacityNum.value = newBackgroundOpacity.toInt()
                    }
                )
            }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Alignment
    SettingsTitle(
        text = getLocalizedString(R.string.element_alignment),
        fontSize = titleFontSize
    )

    SettingsSelect(
        title = getLocalizedString(R.string.clock_alignment),
        option = selectedClockAlignment.string(),
        fontSize = titleFontSize,
        onClick = {
            val gravityOptions = Constants.Gravity.entries.toTypedArray()
            val selectedIndex = gravityOptions.indexOf(selectedClockAlignment).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheetPill(
                context = context,
                options = gravityOptions,
                title = getLocalizedString(R.string.clock_alignment),
                selectedIndex = selectedIndex,
                onItemSelected = { newGravity ->
                    selectedClockAlignment = newGravity // Update state
                    prefs.clockAlignment = newGravity // Persist selection in preferences
                    viewModel.updateClockAlignment(newGravity)
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.date_alignment),
        option = selectedDateAlignment.string(),
        fontSize = titleFontSize,
        onClick = {
            val gravityOptions = Constants.Gravity.entries.toTypedArray()
            val selectedIndex = gravityOptions.indexOf(selectedDateAlignment).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheetPill(
                context = context,
                options = gravityOptions,
                title = getLocalizedString(R.string.date_alignment),
                selectedIndex = selectedIndex,
                onItemSelected = { newGravity ->
                    selectedDateAlignment = newGravity // Update state
                    prefs.dateAlignment = newGravity // Persist selection in preferences
                    viewModel.updateDateAlignment(newGravity)
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.alarm_alignment),
        option = selectedAlarmAlignment.string(),
        fontSize = titleFontSize,
        onClick = {
            val gravityOptions = Constants.Gravity.entries.toTypedArray()
            val selectedIndex = gravityOptions.indexOf(selectedAlarmAlignment).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheetPill(
                context = context,
                options = Constants.Gravity.entries.toTypedArray(),
                title = getLocalizedString(R.string.alarm_alignment),
                selectedIndex = selectedIndex,
                onItemSelected = { newGravity ->
                    selectedAlarmAlignment = newGravity // Update state
                    prefs.alarmAlignment = newGravity // Persist selection in preferences
                    viewModel.updateAlarmAlignment(newGravity)
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.daily_word_alignment),
        option = selectedDailyWordAlignment.string(),
        fontSize = titleFontSize,
        onClick = {
            val gravityOptions = Constants.Gravity.entries.toTypedArray()
            val selectedIndex = gravityOptions.indexOf(selectedDailyWordAlignment).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheetPill(
                context = context,
                options = Constants.Gravity.entries.toTypedArray(),
                title = getLocalizedString(R.string.daily_word_alignment),
                selectedIndex = selectedIndex,
                onItemSelected = { newGravity ->
                    selectedDailyWordAlignment = newGravity // Update state
                    prefs.dailyWordAlignment = newGravity // Persist selection in preferences
                    viewModel.updateDailyWordAlignment(newGravity)
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.home_alignment),
        option = selectedHomeAlignment.string(),
        fontSize = titleFontSize,
        onClick = {
            val gravityOptions = Constants.Gravity.entries.toTypedArray()
            val selectedIndex = gravityOptions.indexOf(selectedHomeAlignment).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheetPill(
                context = context,
                options = gravityOptions,
                title = getLocalizedString(R.string.home_alignment),
                selectedIndex = selectedIndex,
                onItemSelected = { newGravity ->
                    selectedHomeAlignment = newGravity // Update state
                    prefs.homeAlignment = newGravity // Persist selection in preferences
                    viewModel.updateHomeAppsAlignment(
                        prefs.homeAlignment,
                        prefs.homeAlignmentBottom
                    )
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.drawer_alignment),
        option = selectedDrawAlignment.string(),
        fontSize = titleFontSize,
        onClick = {
            val gravityOptions = Constants.Gravity.entries.toTypedArray()
            val selectedIndex = gravityOptions.indexOf(selectedDrawAlignment).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheetPill(
                context = context,
                options = Constants.Gravity.entries.toTypedArray(),
                title = getLocalizedString(R.string.drawer_alignment),
                selectedIndex = selectedIndex,
                onItemSelected = { newGravity ->
                    selectedDrawAlignment = newGravity // Update state
                    prefs.drawerAlignment = newGravity // Persist selection in preferences
                    viewModel.updateDrawerAlignment(newGravity)
                }
            )
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Colors
    SettingsTitle(
        text = getLocalizedString(R.string.element_colors),
        fontSize = titleFontSize
    )

    val hexBackgroundColor = String.format("#%06X", (0xFFFFFF and selectedBackgroundColor))
    SettingsSelect(
        title = getLocalizedString(R.string.background_color),
        option = hexBackgroundColor,
        fontSize = titleFontSize,
        optionColor = Color(selectedBackgroundColor),
        onClick = {
            dialogBuilder.showColorPickerBottomSheet(
                context = context,
                color = selectedBackgroundColor,
                title = getLocalizedString(R.string.background_color),
                onItemSelected = { selectedColor ->
                    selectedBackgroundColor = selectedColor
                    prefs.backgroundColor = selectedColor
                })
        }
    )

    val hexAppColor = String.format("#%06X", (0xFFFFFF and selectedAppColor))
    SettingsSelect(
        title = getLocalizedString(R.string.app_color),
        option = hexAppColor,
        fontSize = titleFontSize,
        optionColor = Color(hexAppColor.toColorInt()),
        onClick = {
            dialogBuilder.showColorPickerBottomSheet(
                context = context,
                color = selectedAppColor,
                title = getLocalizedString(R.string.app_color),
                onItemSelected = { selectedColor ->
                    selectedAppColor = selectedColor
                    prefs.appColor = selectedColor

                    // --- Trigger widget update ---
                    updateHomeWidget(context)
                })
        }
    )

    val hexDateColor = String.format("#%06X", (0xFFFFFF and selectedDateColor))
    SettingsSelect(
        title = getLocalizedString(R.string.date_color),
        option = hexDateColor,
        fontSize = titleFontSize,
        optionColor = Color(hexDateColor.toColorInt()),
        onClick = {
            dialogBuilder.showColorPickerBottomSheet(
                context = context,
                color = selectedDateColor,
                title = getLocalizedString(R.string.date_color),
                onItemSelected = { selectedColor ->
                    selectedDateColor = selectedColor
                    prefs.dateColor = selectedColor
                })
        }
    )

    val hexClockColor = String.format("#%06X", (0xFFFFFF and selectedClockColor))
    SettingsSelect(
        title = getLocalizedString(R.string.clock_color),
        option = hexClockColor,
        fontSize = titleFontSize,
        optionColor = Color(hexClockColor.toColorInt()),
        onClick = {
            dialogBuilder.showColorPickerBottomSheet(
                context = context,
                color = selectedClockColor,
                title = getLocalizedString(R.string.clock_color),
                onItemSelected = { selectedColor ->
                    selectedClockColor = selectedColor
                    prefs.clockColor = selectedColor
                })
        }
    )

    val hexAlarmColor = String.format("#%06X", (0xFFFFFF and selectedAlarmColor))
    SettingsSelect(
        title = getLocalizedString(R.string.alarm_color),
        option = hexAlarmColor,
        fontSize = titleFontSize,
        optionColor = Color(hexAlarmColor.toColorInt()),
        onClick = {
            dialogBuilder.showColorPickerBottomSheet(
                context = context,
                color = selectedAlarmColor,
                title = getLocalizedString(R.string.alarm_color),
                onItemSelected = { selectedColor ->
                    selectedAlarmColor = selectedColor
                    prefs.alarmClockColor = selectedColor
                })
        }
    )

    val hexDailyWordColor = String.format("#%06X", (0xFFFFFF and selectedDailyWordColor))
    SettingsSelect(
        title = getLocalizedString(R.string.daily_word_color),
        option = hexDailyWordColor,
        fontSize = titleFontSize,
        optionColor = Color(hexDailyWordColor.toColorInt()),
        onClick = {
            dialogBuilder.showColorPickerBottomSheet(
                context = context,
                color = selectedDailyWordColor,
                title = getLocalizedString(R.string.daily_word_color),
                onItemSelected = { selectedColor ->
                    selectedDailyWordColor = selectedColor
                    prefs.dailyWordColor = selectedColor
                })
        }
    )

    val hexBatteryColor = String.format("#%06X", (0xFFFFFF and selectedBatteryColor))
    SettingsSelect(
        title = getLocalizedString(R.string.battery_color),
        option = hexBatteryColor,
        fontSize = titleFontSize,
        optionColor = Color(hexBatteryColor.toColorInt()),
        onClick = {
            dialogBuilder.showColorPickerBottomSheet(
                context = context,
                color = selectedBatteryColor,
                title = getLocalizedString(R.string.battery_color),
                onItemSelected = { selectedColor ->
                    selectedBatteryColor = selectedColor
                    prefs.batteryColor = selectedColor
                })
        }
    )

    SettingsSwitch(
        text = getLocalizedString(R.string.rainbow_shortcuts),
        fontSize = titleFontSize,
        defaultState = toggledIconRainbowColors,
        onCheckedChange = {
            toggledIconRainbowColors = !prefs.iconRainbowColors
            prefs.iconRainbowColors = toggledIconRainbowColors
        }
    )
    SettingsSelect(
        title = getLocalizedString(R.string.shortcuts_color),
        option = String.format("#%06X", (0xFFFFFF and selectedShortcutIconsColor)),
        fontSize = titleFontSize,
        optionColor = Color(String.format("#%06X", (0xFFFFFF and selectedShortcutIconsColor)).toColorInt()),
        onClick = {
            dialogBuilder.showColorPickerBottomSheet(
                context = context,
                color = selectedShortcutIconsColor,
                title = getLocalizedString(R.string.shortcuts_color),
                onItemSelected = { selectedColor ->
                    selectedShortcutIconsColor = selectedColor
                    prefs.shortcutIconsColor = selectedColor
                })
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Icon Packs
    SettingsTitle(
        text = getLocalizedString(R.string.icon_packs),
        fontSize = titleFontSize
    )

    SettingsSelect(
        title = getLocalizedString(R.string.select_home_icons),
        option = selectedIconPackHome.getString(IconCacheTarget.HOME.name),
        fontSize = titleFontSize,
        onClick = {
            // Generate options and icons
            val iconPacksEntries = Constants.IconPacks.entries

            val iconPacksOptions = iconPacksEntries.map { it.getString(emptyString()) }

            // Determine selected index based on current prefs value
            val selectedIndex = iconPacksEntries.indexOf(selectedIconPackHome).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = iconPacksOptions.map { it }.toTypedArray(),
                title = getLocalizedString(R.string.select_home_icons),
                selectedIndex = selectedIndex,
                onItemSelected = { newAppIconsName ->
                    val newIconPacksIndex =
                        iconPacksOptions.indexOfFirst { it == newAppIconsName }
                    if (newIconPacksIndex != -1) {
                        val newAppIcons =
                            iconPacksEntries[newIconPacksIndex] // Get the selected FontFamily enum
                        if (newAppIcons == Constants.IconPacks.Custom) {
                            val intent = Intent(fragment.requireActivity(), CustomIconSelectionActivity::class.java).apply {
                                putExtra("IconCacheTarget", "${IconCacheTarget.HOME}")
                            }
                            fragment.startActivity(intent)
                        } else {
                            prefs.customIconPackHome = emptyString()
                            selectedIconPackHome = newAppIcons // Update state
                            prefs.iconPackHome =
                                newAppIcons // Persist selection in preferences
                            viewModel.iconPackHome.value = newAppIcons

                            // --- Trigger widget update ---
                            updateHomeWidget(context)
                        }
                    }
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.select_app_list_icons),
        option = selectedIconPackAppList.getString(IconCacheTarget.APP_LIST.name),
        fontSize = titleFontSize,
        onClick = {
            // Generate options and icons
            val iconPacksEntries = Constants.IconPacks.entries

            val iconPacksOptions = iconPacksEntries.map { it.getString(emptyString()) }

            // Determine selected index based on current prefs value
            val selectedIndex = iconPacksEntries.indexOf(selectedIconPackAppList).takeIf { it >= 0 } ?: 1

            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = iconPacksOptions.map { it }.toTypedArray(),
                title = getLocalizedString(R.string.select_app_list_icons),
                selectedIndex = selectedIndex,
                onItemSelected = { newAppIconsName ->
                    val newIconPacksIndex =
                        iconPacksOptions.indexOfFirst { it == newAppIconsName }
                    if (newIconPacksIndex != -1) {
                        val newAppIcons =
                            iconPacksEntries[newIconPacksIndex] // Get the selected FontFamily enum
                        if (newAppIcons == Constants.IconPacks.Custom) {
                            val intent = Intent(fragment.requireActivity(), CustomIconSelectionActivity::class.java).apply {
                                putExtra("IconCacheTarget", "${IconCacheTarget.APP_LIST}")
                            }
                            fragment.startActivity(intent)
                        } else {
                            prefs.customIconPackAppList = emptyString()
                            selectedIconPackAppList = newAppIcons // Update state
                            prefs.iconPackAppList =
                                newAppIcons // Persist selection in preferences
                            viewModel.iconPackAppList.value = newAppIcons
                        }
                    }
                }
            )
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Text Size (moved to bottom for advanced users)
    SettingsTitle(
        text = getLocalizedString(R.string.text_size_adjustments),
        fontSize = titleFontSize
    )

    SettingsSelect(
        title = getLocalizedString(R.string.app_text_size),
        option = selectedAppSize.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.app_text_size),
                minValue = Constants.MIN_TEXT_SIZE,
                maxValue = Constants.MAX_TEXT_SIZE,
                currentValue = prefs.appSize,
                onValueSelected = { newAppSize ->
                    selectedAppSize = newAppSize.toInt() // Update state
                    prefs.appSize = newAppSize.toInt() // Persist selection in preferences
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.date_text_size),
        option = selectedDateSize.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.date_text_size),
                minValue = Constants.MIN_CLOCK_DATE_SIZE,
                maxValue = Constants.MAX_CLOCK_DATE_SIZE,
                currentValue = prefs.dateSize,
                onValueSelected = { newDateSize ->
                    selectedDateSize = newDateSize.toInt() // Update state
                    prefs.dateSize = newDateSize.toInt() // Persist selection in preferences
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.clock_text_size),
        option = selectedClockSize.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.clock_text_size),
                minValue = Constants.MIN_CLOCK_DATE_SIZE,
                maxValue = Constants.MAX_CLOCK_DATE_SIZE,
                currentValue = prefs.clockSize,
                onValueSelected = { newClockSize ->
                    selectedClockSize = newClockSize.toInt() // Update state
                    prefs.clockSize = newClockSize.toInt() // Persist selection in preferences
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.alarm_text_size),
        option = selectedAlarmSize.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.alarm_text_size),
                minValue = Constants.MIN_ALARM_SIZE,
                maxValue = Constants.MAX_ALARM_SIZE,
                currentValue = prefs.alarmSize,
                onValueSelected = { newDateSize ->
                    selectedAlarmSize = newDateSize.toInt() // Update state
                    prefs.alarmSize = newDateSize.toInt() // Persist selection in preferences
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.daily_word_text_size),
        option = selectedDailyWordSize.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.daily_word_text_size),
                minValue = Constants.MIN_DAILY_WORD_SIZE,
                maxValue = Constants.MAX_DAILY_WORD_SIZE,
                currentValue = prefs.dailyWordSize,
                onValueSelected = { newDateSize ->
                    selectedDailyWordSize = newDateSize.toInt() // Update state
                    prefs.dailyWordSize = newDateSize.toInt() // Persist selection in preferences
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.battery_text_size),
        option = selectedBatterySize.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.battery_text_size),
                minValue = Constants.MIN_BATTERY_SIZE,
                maxValue = Constants.MAX_BATTERY_SIZE,
                currentValue = prefs.batterySize,
                onValueSelected = { newBatterySize ->
                    selectedBatterySize = newBatterySize.toInt() // Update state
                    prefs.batterySize = newBatterySize.toInt() // Persist selection in preferences
                }
            )
        }
    )

    if (isGestureNavigationEnabled(context)) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_gesture_nav)))
    } else {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_3_button_nav)))
    }
}
