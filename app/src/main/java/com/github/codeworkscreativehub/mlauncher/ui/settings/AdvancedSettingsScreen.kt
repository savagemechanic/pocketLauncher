package com.github.codeworkscreativehub.mlauncher.ui.settings

import android.content.Context
import android.os.Process
import androidx.activity.compose.BackHandler
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
import com.github.codeworkscreativehub.common.share.ShareUtils
import com.github.codeworkscreativehub.mlauncher.BuildConfig
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.helper.checkWhoInstalled
import com.github.codeworkscreativehub.mlauncher.helper.communitySupportButton
import com.github.codeworkscreativehub.mlauncher.helper.helpFeedbackButton
import com.github.codeworkscreativehub.mlauncher.helper.ismlauncherDefault
import com.github.codeworkscreativehub.mlauncher.helper.openAppInfo
import com.github.codeworkscreativehub.mlauncher.helper.utils.AppReloader
import com.github.codeworkscreativehub.mlauncher.ui.components.DialogManager
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsHomeItem

@Composable
fun ColumnScope.AdvancedSettingsScreen(
    titleFontSize: TextUnit,
    descriptionFontSize: TextUnit,
    iconSize: Dp,
    dialogBuilder: DialogManager,
    shareUtils: ShareUtils,
    onResetDefaultLauncher: () -> Unit,
    onExitLauncher: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler { onBack() }
    PageHeader(
        iconRes = R.drawable.ic_back,
        title = getLocalizedString(R.string.advanced_settings_title),
        onClick = { onBack() }
    )

    Spacer(modifier = Modifier.height(16.dp))

    val versionName = getLocalizedString(R.string.app_version)

    val (changeLauncherText, changeLauncherDesc) = if (ismlauncherDefault(context)) {
        R.string.advanced_settings_change_default_launcher to
                R.string.advanced_settings_change_default_launcher_description
    } else {
        R.string.advanced_settings_set_as_default_launcher to
                R.string.advanced_settings_set_as_default_launcher_description
    }

    SettingsHomeItem(
        title = getLocalizedString(R.string.advanced_settings_app_info_title),
        description = getLocalizedString(R.string.advanced_settings_app_info_description).format(versionName),
        iconRes = R.drawable.ic_app_info,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = { openAppInfo(context, Process.myUserHandle(), BuildConfig.APPLICATION_ID) }
    )

    SettingsHomeItem(
        title = getLocalizedString(changeLauncherText),
        description = getLocalizedString(changeLauncherDesc),
        iconRes = R.drawable.ic_change_default_launcher,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = { onResetDefaultLauncher() }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.advanced_settings_restart_title),
        description = getLocalizedString(R.string.advanced_settings_restart_description).format(versionName),
        iconRes = R.drawable.ic_restart,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = { AppReloader.restartApp(context) }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.settings_exit_mlauncher_title),
        description = getLocalizedString(R.string.settings_exit_mlauncher_description),
        iconRes = R.drawable.ic_exit,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = { onExitLauncher() }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.advanced_settings_backup_restore_title),
        description = getLocalizedString(R.string.advanced_settings_backup_restore_description),
        iconRes = R.drawable.ic_backup_restore,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = { dialogBuilder.showBackupRestoreBottomSheet() }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.advanced_settings_theme_title),
        description = getLocalizedString(R.string.advanced_settings_theme_description),
        iconRes = R.drawable.ic_theme,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = { dialogBuilder.showSaveLoadThemeBottomSheet() }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.advanced_settings_wotd_title),
        description = getLocalizedString(R.string.advanced_settings_wotd_description),
        iconRes = R.drawable.ic_word_of_the_day,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = { dialogBuilder.showSaveDownloadWOTDBottomSheet() }
    )

    Spacer(modifier = Modifier.weight(1f))

    SettingsHomeItem(
        title = getLocalizedString(R.string.advanced_settings_help_feedback_title),
        iconRes = R.drawable.ic_help_feedback,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = { helpFeedbackButton(context) }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.advanced_settings_community_support_title),
        iconRes = R.drawable.ic_community,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = { communitySupportButton(context) }
    )

    SettingsHomeItem(
        title = getLocalizedString(R.string.advanced_settings_share_application_title),
        iconRes = R.drawable.ic_share_app,
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        iconSize = iconSize,
        onClick = {
            shareUtils.showMaterialShareDialog(
                context,
                getLocalizedString(R.string.share_application),
                checkWhoInstalled(context)
            )
        }
    )

    if (isGestureNavigationEnabled(context)) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_gesture_nav)))
    } else {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_3_button_nav)))
    }
}
