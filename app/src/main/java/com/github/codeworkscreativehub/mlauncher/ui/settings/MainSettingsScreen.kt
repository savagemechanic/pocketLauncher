package com.github.codeworkscreativehub.mlauncher.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.common.isGestureNavigationEnabled
// Toast used in place of Fragment.showInstantToast for Compose context
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.helper.utils.PrivateSpaceManager
import com.github.codeworkscreativehub.mlauncher.ui.components.DialogManager
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.TopMainHeader

@Composable
fun ColumnScope.MainSettingsScreen(
    prefs: Prefs,
    titleFontSize: TextUnit,
    descriptionFontSize: TextUnit,
    iconSize: Dp,
    dialogBuilder: DialogManager,
    toggledPrivateSpaces: Boolean,
    toggledExpertOptions: Boolean,
    onPrivateSpaceToggle: () -> Unit,
    onNavigate: (String) -> Unit,
    onShowFavoriteApps: () -> Unit,
    onShowHiddenApps: () -> Unit,
    onExpertUnlocked: () -> Unit
) {
    val context = LocalContext.current

    Spacer(modifier = Modifier.height(16.dp))

    TopMainHeader(
        iconRes = R.drawable.app_launcher,
        title = getLocalizedString(R.string.settings_name),
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        onIconClick = { dialogBuilder.showDeviceStatsBottomSheet(context = context) }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.settings_features_title),
        description = getLocalizedString(R.string.settings_features_description),
        iconRes = R.drawable.ic_feature,
        titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
        onClick = { onNavigate("features") }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.settings_look_feel_title),
        description = getLocalizedString(R.string.settings_look_feel_description),
        iconRes = R.drawable.ic_look_feel,
        titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
        onClick = { onNavigate("look_feel") }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.settings_gestures_title),
        description = getLocalizedString(R.string.settings_gestures_description),
        iconRes = R.drawable.ic_gestures,
        titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
        onClick = { onNavigate("gestures") }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.settings_notes_title),
        description = getLocalizedString(R.string.settings_notes_description),
        iconRes = R.drawable.ic_notes,
        titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
        onClick = { onNavigate("notes") }
    )

    if (PrivateSpaceManager(context).isPrivateSpaceSetUp()) {
        val (icon, status) = if (toggledPrivateSpaces) {
            R.drawable.private_profile_on to R.string.locked
        } else {
            R.drawable.private_profile_off to R.string.unlocked
        }
        SettingsHomeItem(
            title = getLocalizedString(R.string.private_space, getLocalizedString(status)),
            iconRes = icon,
            titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
            onClick = { onPrivateSpaceToggle() }
        )
    }

    SettingsHomeItem(
        title = getLocalizedString(R.string.settings_favorite_apps_title),
        description = getLocalizedString(R.string.settings_favorite_apps_description),
        iconRes = R.drawable.ic_favorite,
        titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
        onClick = { onShowFavoriteApps() }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.settings_hidden_apps_title),
        description = getLocalizedString(R.string.settings_hidden_apps_description),
        iconRes = R.drawable.ic_hidden,
        titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
        onClick = { onShowHiddenApps() }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.settings_advanced_title),
        description = getLocalizedString(R.string.settings_advanced_description),
        iconRes = R.drawable.ic_advanced,
        titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
        onClick = { onNavigate("advanced") }
    )

    if (toggledExpertOptions) {
        SettingsHomeItem(
            title = getLocalizedString(R.string.settings_expert_title),
            description = getLocalizedString(R.string.settings_expert_description),
            iconRes = R.drawable.ic_experimental,
            titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
            onClick = { onNavigate("expert") }
        )
    }

    Spacer(modifier = Modifier.weight(1f))

    SettingsHomeItem(
        title = getLocalizedString(R.string.about_settings_title, getLocalizedString(R.string.app_name)),
        iconRes = R.drawable.ic_toast,
        titleFontSize = titleFontSize, descriptionFontSize = descriptionFontSize, iconSize = iconSize,
        onClick = { onNavigate("about") },
        enableMultiClick = !toggledExpertOptions,
        onMultiClick = { count ->
            if (!prefs.enableExpertOptions) {
                if (count in 2..4) {
                    Toast.makeText(context, getLocalizedString(R.string.expert_options_tap_hint, count), Toast.LENGTH_SHORT).show()
                } else if (count == 5) {
                    Toast.makeText(context, getLocalizedString(R.string.expert_options_unlocked), Toast.LENGTH_SHORT).show()
                    onExpertUnlocked()
                }
            }
        }
    )

    if (isGestureNavigationEnabled(context)) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_gesture_nav)))
    } else {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_3_button_nav)))
    }
}
