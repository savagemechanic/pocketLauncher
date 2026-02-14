package com.github.codeworkscreativehub.mlauncher.ui.settings

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
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.ui.components.DialogManager
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsTitle

@Composable
fun NotesSettingsScreen(
    prefs: Prefs,
    titleFontSize: TextUnit,
    dialogBuilder: DialogManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler { onBack() }
    PageHeader(
        iconRes = R.drawable.ic_back,
        title = getLocalizedString(R.string.notes_settings_title),
        onClick = { onBack() }
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Display
    SettingsTitle(text = getLocalizedString(R.string.display_options), fontSize = titleFontSize)

    var toggledAutoExpandNotes by remember { mutableStateOf(prefs.autoExpandNotes) }
    var toggledClickToEditDelete by remember { mutableStateOf(prefs.clickToEditDelete) }

    SettingsSwitch(
        text = getLocalizedString(R.string.auto_expand_notes),
        fontSize = titleFontSize,
        defaultState = toggledAutoExpandNotes,
        onCheckedChange = {
            toggledAutoExpandNotes = !prefs.autoExpandNotes
            prefs.autoExpandNotes = toggledAutoExpandNotes
        }
    )
    SettingsSwitch(
        text = getLocalizedString(R.string.click_to_edit_delete),
        fontSize = titleFontSize,
        defaultState = toggledClickToEditDelete,
        onCheckedChange = {
            toggledClickToEditDelete = !prefs.clickToEditDelete
            prefs.clickToEditDelete = toggledClickToEditDelete
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Notes Colors
    SettingsTitle(text = getLocalizedString(R.string.notes_colors), fontSize = titleFontSize)

    var selectedNotesBackgroundColor by remember { mutableIntStateOf(prefs.notesBackgroundColor) }
    var selectedBubbleBackgroundColor by remember { mutableIntStateOf(prefs.bubbleBackgroundColor) }
    var selectedBubbleMessageTextColor by remember { mutableIntStateOf(prefs.bubbleMessageTextColor) }
    var selectedBubbleTimeDateColor by remember { mutableIntStateOf(prefs.bubbleTimeDateColor) }
    var selectedBubbleCategoryColor by remember { mutableIntStateOf(prefs.bubbleCategoryColor) }

    ColorSetting(getLocalizedString(R.string.notes_background_color), selectedNotesBackgroundColor, titleFontSize, dialogBuilder, context) {
        selectedNotesBackgroundColor = it; prefs.notesBackgroundColor = it
    }
    ColorSetting(getLocalizedString(R.string.bubble_background_color), selectedBubbleBackgroundColor, titleFontSize, dialogBuilder, context) {
        selectedBubbleBackgroundColor = it; prefs.bubbleBackgroundColor = it
    }
    ColorSetting(getLocalizedString(R.string.bubble_message_color), selectedBubbleMessageTextColor, titleFontSize, dialogBuilder, context) {
        selectedBubbleMessageTextColor = it; prefs.bubbleMessageTextColor = it
    }
    ColorSetting(getLocalizedString(R.string.bubble_date_time_color), selectedBubbleTimeDateColor, titleFontSize, dialogBuilder, context) {
        selectedBubbleTimeDateColor = it; prefs.bubbleTimeDateColor = it
    }
    ColorSetting(getLocalizedString(R.string.bubble_category_color), selectedBubbleCategoryColor, titleFontSize, dialogBuilder, context) {
        selectedBubbleCategoryColor = it; prefs.bubbleCategoryColor = it
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Input Colors
    SettingsTitle(text = getLocalizedString(R.string.input_colors), fontSize = titleFontSize)

    var selectedInputMessageColor by remember { mutableIntStateOf(prefs.inputMessageColor) }
    var selectedInputMessageHintColor by remember { mutableIntStateOf(prefs.inputMessageHintColor) }

    ColorSetting(getLocalizedString(R.string.message_input_color), selectedInputMessageColor, titleFontSize, dialogBuilder, context) {
        selectedInputMessageColor = it; prefs.inputMessageColor = it
    }
    ColorSetting(getLocalizedString(R.string.message_input_hint_color), selectedInputMessageHintColor, titleFontSize, dialogBuilder, context) {
        selectedInputMessageHintColor = it; prefs.inputMessageHintColor = it
    }

    if (isGestureNavigationEnabled(context)) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_gesture_nav)))
    } else {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_margin_3_button_nav)))
    }
}

@Composable
private fun ColorSetting(
    title: String,
    colorValue: Int,
    fontSize: TextUnit,
    dialogBuilder: DialogManager,
    context: android.content.Context,
    onColorSelected: (Int) -> Unit
) {
    val hex = String.format("#%06X", (0xFFFFFF and colorValue))
    SettingsSelect(
        title = title,
        option = hex,
        fontSize = fontSize,
        optionColor = Color(hex.toColorInt()),
        onClick = {
            dialogBuilder.showColorPickerBottomSheet(
                context = context,
                color = colorValue,
                title = title,
                onItemSelected = { onColorSelected(it) }
            )
        }
    )
}
