package com.github.codeworkscreativehub.mlauncher.ui.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.common.isBiometricEnabled
import com.github.codeworkscreativehub.common.isGestureNavigationEnabled
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.Constants
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.ui.components.DialogManager
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsTitle
import com.github.codeworkscreativehub.mlauncher.helper.utils.AppReloader
import com.github.codeworkscreativehub.mlauncher.voice.nlu.CloudLLMNlu
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Composable
fun ExpertSettingsScreen(
    prefs: Prefs,
    titleFontSize: TextUnit,
    dialogBuilder: DialogManager,
    resources: android.content.res.Resources,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler { onBack() }

    PageHeader(
        iconRes = R.drawable.ic_back,
        title = getLocalizedString(R.string.expert_settings_title),
        onClick = { onBack() }
    )

    Spacer(modifier = Modifier.height(16.dp))

    var toggledExpertOptions by remember { mutableStateOf(prefs.enableExpertOptions) }
    SettingsSwitch(
        text = getLocalizedString(R.string.expert_options_display),
        fontSize = titleFontSize,
        defaultState = toggledExpertOptions,
        onCheckedChange = {
            toggledExpertOptions = !prefs.enableExpertOptions
            prefs.enableExpertOptions = toggledExpertOptions
            onBack()
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Personalization
    SettingsTitle(text = getLocalizedString(R.string.personalization), fontSize = titleFontSize)

    var selectedSettingsSize by remember { mutableStateOf(prefs.settingsSize) }
    SettingsSelect(
        title = getLocalizedString(R.string.settings_text_size),
        option = selectedSettingsSize.toString(),
        fontSize = titleFontSize,
        onClick = {
            dialogBuilder.showSliderBottomSheet(
                context = context,
                title = getLocalizedString(R.string.settings_text_size),
                minValue = Constants.MIN_TEXT_SIZE,
                maxValue = Constants.MAX_TEXT_SIZE,
                currentValue = prefs.settingsSize,
                onValueSelected = { newSize ->
                    selectedSettingsSize = newSize.toInt()
                    prefs.settingsSize = newSize.toInt()
                }
            )
        }
    )

    var toggledLockOrientation by remember { mutableStateOf(prefs.lockOrientation) }
    SettingsSwitch(
        text = getLocalizedString(R.string.lock_orientation),
        fontSize = titleFontSize,
        defaultState = toggledLockOrientation,
        onCheckedChange = {
            toggledLockOrientation = !prefs.lockOrientation
            prefs.lockOrientation = toggledLockOrientation
            val currentOrientation = resources.configuration.orientation
            prefs.lockOrientationPortrait = currentOrientation == Configuration.ORIENTATION_PORTRAIT
            AppReloader.restartApp(context)
        }
    )

    var toggledForceWallpaper by remember { mutableStateOf(prefs.forceWallpaper) }
    SettingsSwitch(
        text = getLocalizedString(R.string.force_colored_wallpaper),
        fontSize = titleFontSize,
        defaultState = toggledForceWallpaper,
        onCheckedChange = {
            toggledForceWallpaper = !prefs.forceWallpaper
            prefs.forceWallpaper = toggledForceWallpaper
        }
    )

    if (context.isBiometricEnabled()) {
        var toggledSettingsLocked by remember { mutableStateOf(prefs.settingsLocked) }
        SettingsSwitch(
            text = getLocalizedString(R.string.lock_settings),
            fontSize = titleFontSize,
            defaultState = toggledSettingsLocked,
            onCheckedChange = {
                toggledSettingsLocked = !prefs.settingsLocked
                prefs.settingsLocked = toggledSettingsLocked
            }
        )
    }

    var toggledHapticFeedback by remember { mutableStateOf(prefs.hapticFeedback) }
    SettingsSwitch(
        text = getLocalizedString(R.string.haptic_feedback),
        fontSize = titleFontSize,
        defaultState = toggledHapticFeedback,
        onCheckedChange = {
            toggledHapticFeedback = !prefs.hapticFeedback
            prefs.hapticFeedback = toggledHapticFeedback
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Voice Control
    SettingsTitle(text = getLocalizedString(R.string.voice_control), fontSize = titleFontSize)

    var toggledVoiceEnabled by remember { mutableStateOf(prefs.voiceEnabled) }
    var toggledVoiceCloud by remember { mutableStateOf(prefs.voiceCloudEnabled) }
    var toggledVoiceTts by remember { mutableStateOf(prefs.voiceTtsEnabled) }
    var toggledVoiceHaptic by remember { mutableStateOf(prefs.voiceHapticEnabled) }

    SettingsSwitch(
        text = getLocalizedString(R.string.voice_enable),
        fontSize = titleFontSize,
        defaultState = toggledVoiceEnabled,
        onCheckedChange = {
            toggledVoiceEnabled = !prefs.voiceEnabled
            prefs.voiceEnabled = toggledVoiceEnabled
        }
    )

    if (toggledVoiceEnabled) {
        SettingsSwitch(
            text = getLocalizedString(R.string.voice_cloud_ai),
            fontSize = titleFontSize,
            defaultState = toggledVoiceCloud,
            onCheckedChange = {
                toggledVoiceCloud = !prefs.voiceCloudEnabled
                prefs.voiceCloudEnabled = toggledVoiceCloud
            }
        )

        if (toggledVoiceCloud) {
            var apiKeyText by remember {
                mutableStateOf(
                    CloudLLMNlu.getApiKey(context)?.let { "••••••${it.takeLast(4)}" } ?: ""
                )
            }
            SettingsSelect(
                title = getLocalizedString(R.string.voice_api_key),
                option = apiKeyText.ifEmpty { getLocalizedString(R.string.voice_api_key_hint) },
                fontSize = titleFontSize,
                onClick = {
                    val editText = android.widget.EditText(context).apply {
                        hint = getLocalizedString(R.string.voice_api_key_hint)
                        inputType = android.text.InputType.TYPE_CLASS_TEXT or
                                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                        setPadding(48, 32, 48, 32)
                    }
                    MaterialAlertDialogBuilder(context)
                        .setTitle(getLocalizedString(R.string.voice_api_key))
                        .setView(editText)
                        .setPositiveButton(getLocalizedString(R.string.okay)) { _, _ ->
                            val key = editText.text.toString().trim()
                            if (key.isNotBlank()) {
                                CloudLLMNlu.saveApiKey(context, key)
                                apiKeyText = "••••••${key.takeLast(4)}"
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            )
        }

        SettingsSwitch(
            text = getLocalizedString(R.string.voice_tts),
            fontSize = titleFontSize,
            defaultState = toggledVoiceTts,
            onCheckedChange = {
                toggledVoiceTts = !prefs.voiceTtsEnabled
                prefs.voiceTtsEnabled = toggledVoiceTts
            }
        )

        SettingsSwitch(
            text = getLocalizedString(R.string.voice_haptic),
            fontSize = titleFontSize,
            defaultState = toggledVoiceHaptic,
            onCheckedChange = {
                toggledVoiceHaptic = !prefs.voiceHapticEnabled
                prefs.voiceHapticEnabled = toggledVoiceHaptic
            }
        )
    }

    if (isGestureNavigationEnabled(context)) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_gesture_nav)))
    } else {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_3_button_nav)))
    }
}
