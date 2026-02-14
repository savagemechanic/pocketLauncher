package com.github.codeworkscreativehub.mlauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.common.isGestureNavigationEnabled
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.Constants
import com.github.codeworkscreativehub.mlauncher.data.Constants.Action
import com.github.codeworkscreativehub.mlauncher.data.Constants.AppDrawerFlag
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.helper.ismlauncherDefault
import com.github.codeworkscreativehub.mlauncher.helper.utils.PrivateSpaceManager
import com.github.codeworkscreativehub.mlauncher.ui.BaseFragment
import com.github.codeworkscreativehub.mlauncher.ui.components.DialogManager
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsTitle

@Composable
fun GesturesSettingsScreen(
    prefs: Prefs,
    titleFontSize: TextUnit,
    descriptionFontSize: TextUnit,
    iconSize: Dp,
    dialogBuilder: DialogManager,
    fragment: BaseFragment,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // State variables for gesture actions
    var selectedDoubleTapAction by remember { mutableStateOf(prefs.doubleTapAction) }
    var selectedClickClockAction by remember { mutableStateOf(prefs.clickClockAction) }
    var selectedClickDateAction by remember { mutableStateOf(prefs.clickDateAction) }
    var selectedClickAppUsageAction by remember { mutableStateOf(prefs.clickAppUsageAction) }
    var selectedClickFloatingAction by remember { mutableStateOf(prefs.clickFloatingAction) }

    var selectedShortSwipeUpAction by remember { mutableStateOf(prefs.shortSwipeUpAction) }
    var selectedShortSwipeDownAction by remember { mutableStateOf(prefs.shortSwipeDownAction) }
    var selectedShortSwipeLeftAction by remember { mutableStateOf(prefs.shortSwipeLeftAction) }
    var selectedShortSwipeRightAction by remember { mutableStateOf(prefs.shortSwipeRightAction) }

    var selectedLongSwipeUpAction by remember { mutableStateOf(prefs.longSwipeUpAction) }
    var selectedLongSwipeDownAction by remember { mutableStateOf(prefs.longSwipeDownAction) }
    var selectedLongSwipeLeftAction by remember { mutableStateOf(prefs.longSwipeLeftAction) }
    var selectedLongSwipeRightAction by remember { mutableStateOf(prefs.longSwipeRightAction) }

    var selectedShortSwipeThreshold by remember { mutableFloatStateOf(prefs.shortSwipeThreshold) }
    var selectedLongSwipeThreshold by remember { mutableFloatStateOf(prefs.longSwipeThreshold) }

    val actions = Action.entries

    // Filter out 'TogglePrivateSpace' if private space is not supported
    val filteredActions =
        if (!PrivateSpaceManager(context).isPrivateSpaceSetUp() || !ismlauncherDefault(context)) {
            actions.filter { it != Action.TogglePrivateSpace }
        } else {
            actions
        }

    val actionStrings = filteredActions.map { it.getString() }.toTypedArray()

    // Helper function to set gesture
    fun setGesture(flag: AppDrawerFlag, action: Action) {
        when (flag) {
            AppDrawerFlag.SetShortSwipeUp -> prefs.shortSwipeUpAction = action
            AppDrawerFlag.SetShortSwipeDown -> prefs.shortSwipeDownAction = action
            AppDrawerFlag.SetShortSwipeLeft -> prefs.shortSwipeLeftAction = action
            AppDrawerFlag.SetShortSwipeRight -> prefs.shortSwipeRightAction = action
            AppDrawerFlag.SetClickClock -> prefs.clickClockAction = action
            AppDrawerFlag.SetAppUsage -> prefs.clickAppUsageAction = action
            AppDrawerFlag.SetClickDate -> prefs.clickDateAction = action
            AppDrawerFlag.SetDoubleTap -> prefs.doubleTapAction = action
            AppDrawerFlag.SetLongSwipeUp -> prefs.longSwipeUpAction = action
            AppDrawerFlag.SetLongSwipeDown -> prefs.longSwipeDownAction = action
            AppDrawerFlag.SetLongSwipeLeft -> prefs.longSwipeLeftAction = action
            AppDrawerFlag.SetLongSwipeRight -> prefs.longSwipeRightAction = action
            AppDrawerFlag.SetFloating -> prefs.clickFloatingAction = action
            AppDrawerFlag.None,
            AppDrawerFlag.SetHomeApp,
            AppDrawerFlag.HiddenApps,
            AppDrawerFlag.PrivateApps,
            AppDrawerFlag.LaunchApp -> {
            }
        }

        when (action) {
            Action.OpenApp -> {
                fragment.findNavController().navigate(
                    R.id.action_settingsFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString())
                )
            }
            else -> {
                // No additional action needed for other actions
            }
        }
    }

    BackHandler { onBack() }
    PageHeader(
        iconRes = R.drawable.ic_back,
        title = getLocalizedString(R.string.gestures_settings_title),
        onClick = { onBack() }
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Tap & Click Actions
    SettingsTitle(
        text = getLocalizedString(R.string.tap_click_actions),
        fontSize = titleFontSize
    )

    val appLabelDoubleTapAction = prefs.appDoubleTap.activityLabel
    SettingsSelect(
        title = getLocalizedString(R.string.double_tap),
        option = if (selectedDoubleTapAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelDoubleTapAction"
        } else {
            selectedDoubleTapAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.double_tap),
                onItemSelected = { newDoubleTapAction ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newDoubleTapAction }
                    if (selectedAction != null) {
                        selectedDoubleTapAction = selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetDoubleTap,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelClickClockAction = prefs.appClickClock.activityLabel.ifEmpty { "Clock" }
    SettingsSelect(
        title = getLocalizedString(R.string.clock_click_app),
        option = if (selectedClickClockAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelClickClockAction"
        } else {
            selectedClickClockAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.clock_click_app),
                onItemSelected = { newClickClock ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newClickClock }
                    if (selectedAction != null) {
                        selectedClickClockAction = selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetClickClock,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelClickDateAction = prefs.appClickDate.activityLabel.ifEmpty { "Calendar" }
    SettingsSelect(
        title = getLocalizedString(R.string.date_click_app),
        option = if (selectedClickDateAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelClickDateAction"
        } else {
            selectedClickDateAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.date_click_app),
                onItemSelected = { newClickDate ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newClickDate }
                    if (selectedAction != null) {
                        selectedClickDateAction = selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetClickDate,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelClickAppUsageAction =
        prefs.appClickUsage.activityLabel.ifEmpty { "Digital Wellbeing" }
    SettingsSelect(
        title = getLocalizedString(R.string.usage_click_app),
        option = if (selectedClickAppUsageAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelClickAppUsageAction"
        } else {
            selectedClickAppUsageAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.usage_click_app),
                onItemSelected = { newClickAppUsage ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newClickAppUsage }
                    if (selectedAction != null) {
                        selectedClickAppUsageAction =
                            selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetAppUsage,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelClickFloatingAction = prefs.appFloating.activityLabel.ifEmpty { "Notes" }
    SettingsSelect(
        title = getLocalizedString(R.string.floating_click_app),
        option = if (selectedClickFloatingAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelClickFloatingAction"
        } else {
            selectedClickFloatingAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.floating_click_app),
                onItemSelected = { newClickFloating ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newClickFloating }
                    if (selectedAction != null) {
                        selectedClickFloatingAction =
                            selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetFloating,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Swipe Actions
    SettingsTitle(
        text = getLocalizedString(R.string.swipe_movement),
        fontSize = titleFontSize,
    )

    val appLabelShortSwipeUpAction =
        prefs.appShortSwipeUp.activityLabel.ifEmpty { "Settings" }
    SettingsSelect(
        title = getLocalizedString(R.string.short_swipe_up_app),
        option = if (selectedShortSwipeUpAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelShortSwipeUpAction"
        } else {
            selectedShortSwipeUpAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.short_swipe_up_app),
                onItemSelected = { newShortSwipeUpAction ->

                    val selectedAction =
                        actions.firstOrNull { it.getString() == newShortSwipeUpAction }
                    if (selectedAction != null) {
                        selectedShortSwipeUpAction = selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetShortSwipeUp,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelShortSwipeDownAction =
        prefs.appShortSwipeDown.activityLabel.ifEmpty { "Phone" }
    SettingsSelect(
        title = getLocalizedString(R.string.short_swipe_down_app),
        option = if (selectedShortSwipeDownAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelShortSwipeDownAction"
        } else {
            selectedShortSwipeDownAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.short_swipe_down_app),
                onItemSelected = { newShortSwipeDownAction ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newShortSwipeDownAction }
                    if (selectedAction != null) {
                        selectedShortSwipeDownAction =
                            selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetShortSwipeDown,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelShortSwipeLeftAction =
        prefs.appShortSwipeLeft.activityLabel.ifEmpty { "Settings" }
    SettingsSelect(
        title = getLocalizedString(R.string.short_swipe_left_app),
        option = if (selectedShortSwipeLeftAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelShortSwipeLeftAction"
        } else {
            selectedShortSwipeLeftAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.short_swipe_left_app),
                onItemSelected = { newShortSwipeLeftAction ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newShortSwipeLeftAction }
                    if (selectedAction != null) {
                        selectedShortSwipeLeftAction =
                            selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetShortSwipeLeft,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelShortSwipeRightAction =
        prefs.appShortSwipeRight.activityLabel.ifEmpty { "Phone" }
    SettingsSelect(
        title = getLocalizedString(R.string.short_swipe_right_app),
        option = if (selectedShortSwipeRightAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelShortSwipeRightAction"
        } else {
            selectedShortSwipeRightAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.short_swipe_right_app),
                onItemSelected = { newShortSwipeRightAction ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newShortSwipeRightAction }
                    if (selectedAction != null) {
                        selectedShortSwipeRightAction =
                            selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetShortSwipeRight,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelLongSwipeUpAction =
        prefs.appLongSwipeUp.activityLabel.ifEmpty { "Settings" }
    SettingsSelect(
        title = getLocalizedString(R.string.long_swipe_up_app),
        option = if (selectedLongSwipeUpAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelLongSwipeUpAction"
        } else {
            selectedLongSwipeUpAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.long_swipe_up_app),
                onItemSelected = { newLongSwipeUpAction ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newLongSwipeUpAction }
                    if (selectedAction != null) {
                        selectedLongSwipeUpAction = selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetLongSwipeUp,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelLongSwipeDownAction =
        prefs.appLongSwipeDown.activityLabel.ifEmpty { "Phone" }
    SettingsSelect(
        title = getLocalizedString(R.string.long_swipe_down_app),
        option = if (selectedLongSwipeDownAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelLongSwipeDownAction"
        } else {
            selectedLongSwipeDownAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.long_swipe_down_app),
                onItemSelected = { newLongSwipeDownAction ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newLongSwipeDownAction }
                    if (selectedAction != null) {
                        selectedLongSwipeDownAction =
                            selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetLongSwipeDown,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelLongSwipeLeftAction =
        prefs.appLongSwipeLeft.activityLabel.ifEmpty { "Settings" }
    SettingsSelect(
        title = getLocalizedString(R.string.long_swipe_left_app),
        option = if (selectedLongSwipeLeftAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelLongSwipeLeftAction"
        } else {
            selectedLongSwipeLeftAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings, // Pass the list of localized strings
                title = getLocalizedString(R.string.long_swipe_left_app),
                onItemSelected = { newLongSwipeLeftAction ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newLongSwipeLeftAction }
                    if (selectedAction != null) {
                        selectedLongSwipeLeftAction =
                            selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetLongSwipeLeft,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    val appLabelLongSwipeRightAction =
        prefs.appLongSwipeRight.activityLabel.ifEmpty { "Phone" }
    SettingsSelect(
        title = getLocalizedString(R.string.long_swipe_right_app),
        option = if (selectedLongSwipeRightAction == Action.OpenApp) {
            "${getLocalizedString(R.string.open)} $appLabelLongSwipeRightAction"
        } else {
            selectedLongSwipeRightAction.string()
        },
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = actionStrings,
                title = getLocalizedString(R.string.long_swipe_right_app),
                onItemSelected = { newLongSwipeRightAction ->
                    val selectedAction =
                        actions.firstOrNull { it.getString() == newLongSwipeRightAction }
                    if (selectedAction != null) {
                        selectedLongSwipeRightAction =
                            selectedAction // Store the enum itself
                        setGesture(
                            AppDrawerFlag.SetLongSwipeRight,
                            selectedAction
                        ) // Persist selection in preferences
                    }
                }
            )
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Thresholds
    SettingsTitle(
        text = getLocalizedString(R.string.threshold),
        fontSize = titleFontSize
    )

    SettingsSelect(
        title = getLocalizedString(R.string.settings_short_threshold),
        option = selectedShortSwipeThreshold.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.settings_short_threshold),
                minValue = Constants.MIN_THRESHOLD,
                maxValue = selectedLongSwipeThreshold,
                currentValue = prefs.shortSwipeThreshold,
                onValueSelected = { newSettingsSize ->
                    selectedShortSwipeThreshold = newSettingsSize.toFloat()
                    prefs.shortSwipeThreshold = newSettingsSize.toFloat()
                }
            )
        }
    )

    SettingsSelect(
        title = getLocalizedString(R.string.settings_long_threshold),
        option = selectedLongSwipeThreshold.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.settings_long_threshold),
                minValue = selectedShortSwipeThreshold,
                maxValue = Constants.MAX_THRESHOLD,
                currentValue = prefs.longSwipeThreshold,
                onValueSelected = { newSettingsSize ->
                    selectedLongSwipeThreshold = newSettingsSize.toFloat()
                    prefs.longSwipeThreshold = newSettingsSize.toFloat()
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
