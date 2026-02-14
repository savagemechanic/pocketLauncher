package com.github.codeworkscreativehub.mlauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.common.isGestureNavigationEnabled
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.TitleWithHtmlLinks
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.TopMainHeader

@Composable
fun ColumnScope.AboutSettingsScreen(
    titleFontSize: TextUnit,
    descriptionFontSize: TextUnit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler { onBack() }

    PageHeader(
        iconRes = R.drawable.ic_back,
        title = getLocalizedString(R.string.about_settings_title, getLocalizedString(R.string.app_name)),
        onClick = { onBack() }
    )

    Spacer(modifier = Modifier.height(26.dp))

    TopMainHeader(
        iconRes = R.drawable.app_launcher,
        title = getLocalizedString(R.string.app_name),
        description = getLocalizedString(R.string.created_by),
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize
    )

    TitleWithHtmlLinks(
        title = getLocalizedString(R.string.app_version),
        titleFontSize = descriptionFontSize,
    )

    Spacer(modifier = Modifier.height(16.dp))

    TitleWithHtmlLinks(
        title = getLocalizedString(R.string.settings_source_code),
        descriptions = listOf(getLocalizedString(R.string.github_link)),
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        columns = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    TitleWithHtmlLinks(
        title = getLocalizedString(R.string.settings_donations),
        descriptions = listOf(
            getLocalizedString(R.string.sponsor_link),
            getLocalizedString(R.string.coffee_link),
            getLocalizedString(R.string.libera_link)
        ),
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize
    )

    Spacer(modifier = Modifier.weight(1f))

    TitleWithHtmlLinks(
        title = getLocalizedString(R.string.settings_credits),
        descriptions = listOf(
            getLocalizedString(R.string.weather_link),
            getLocalizedString(R.string.forked_link),
            getLocalizedString(R.string.privacy_policy_link)
        ),
        titleFontSize = titleFontSize,
        descriptionFontSize = descriptionFontSize,
        columns = true
    )

    if (isGestureNavigationEnabled(context)) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_gesture_nav)))
    } else {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_3_button_nav)))
    }
}
