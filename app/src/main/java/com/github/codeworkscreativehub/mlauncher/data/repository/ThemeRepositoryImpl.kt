package com.github.codeworkscreativehub.mlauncher.data.repository

import android.content.Context
import androidx.core.content.ContextCompat.getColor
import androidx.core.graphics.toColorInt
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.common.showLongToast
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.*
import com.github.codeworkscreativehub.mlauncher.data.datasource.MoshiSerializer
import com.github.codeworkscreativehub.mlauncher.data.datasource.PreferencesDataSource
import com.github.codeworkscreativehub.mlauncher.helper.isSystemInDarkMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeRepositoryImpl(
    private val dataSource: PreferencesDataSource,
    private val serializer: MoshiSerializer,
    private val context: Context,
    scope: CoroutineScope
) : ThemeRepository {

    private val flowToKey = mutableMapOf<StateFlow<Int>, String>()

    private fun colorFlow(key: String, default: Int): StateFlow<Int> {
        val flow = MutableStateFlow(dataSource.getInt(key, default))
        flowToKey[flow] = key
        return flow.asStateFlow()
    }

    private fun sizeFlow(key: String, default: Int): StateFlow<Int> {
        val flow = MutableStateFlow(dataSource.getInt(key, default))
        flowToKey[flow] = key
        return flow.asStateFlow()
    }

    private fun defaultColor(type: String): Int {
        val theme = runCatching {
            enumValueOf<Constants.Theme>(dataSource.getString(APP_THEME, Constants.Theme.System.name))
        }.getOrDefault(Constants.Theme.System)
        val isDark = when (theme) {
            Constants.Theme.System -> isSystemInDarkMode(context)
            Constants.Theme.Dark -> true
            Constants.Theme.Light -> false
        }
        val lightColors = mapOf(
            "bg" to R.color.white, "txt" to R.color.black,
            "bg_notes" to R.color.light_gray_light, "bg_bubble" to R.color.light_gray_medium,
            "bg_bubble_message" to R.color.black, "bg_bubble_time_date" to R.color.dark_gray_very_dark,
            "bg_bubble_category" to R.color.dark_gray_dark, "input_text" to R.color.dark_gray_very_dark,
            "input_text_hint" to R.color.dark_gray_dark,
        )
        val darkColors = mapOf(
            "bg" to R.color.black, "txt" to R.color.white,
            "bg_notes" to R.color.dark_gray_very_dark, "bg_bubble" to R.color.dark_gray_dark,
            "bg_bubble_message" to R.color.white, "bg_bubble_time_date" to R.color.light_gray_very_light,
            "bg_bubble_category" to R.color.light_gray_light, "input_text" to R.color.light_gray_very_light,
            "input_text_hint" to R.color.light_gray_light,
        )
        val defaultLight = R.color.black
        val defaultDark = R.color.white
        val colorRes = if (isDark) darkColors[type] ?: defaultDark else lightColors[type] ?: defaultLight
        return getColor(context, colorRes)
    }

    // --- Colors ---
    override val backgroundColor = colorFlow(BACKGROUND_COLOR, defaultColor("bg"))
    override val appColor = colorFlow(APP_COLOR, defaultColor("txt"))
    override val dateColor = colorFlow(DATE_COLOR, defaultColor("txt"))
    override val clockColor = colorFlow(CLOCK_COLOR, defaultColor("txt"))
    override val batteryColor = colorFlow(BATTERY_COLOR, defaultColor("txt"))
    override val dailyWordColor = colorFlow(DAILY_WORD_COLOR, defaultColor("txt"))
    override val shortcutIconsColor = colorFlow(SHORTCUT_ICONS_COLOR, defaultColor("txt"))
    override val alarmClockColor = colorFlow(ALARM_CLOCK_COLOR, defaultColor("txt"))
    override val notesBackgroundColor = colorFlow(NOTES_BACKGROUND_COLOR, defaultColor("bg_notes"))
    override val bubbleBackgroundColor = colorFlow(BUBBLE_BACKGROUND_COLOR, defaultColor("bg_bubble"))
    override val bubbleMessageTextColor = colorFlow(BUBBLE_MESSAGE_COLOR, defaultColor("bg_bubble_message"))
    override val bubbleTimeDateColor = colorFlow(BUBBLE_TIMEDATE_COLOR, defaultColor("bg_bubble_time_date"))
    override val bubbleCategoryColor = colorFlow(BUBBLE_CATEGORY_COLOR, defaultColor("bg_bubble_category"))
    override val inputMessageColor = colorFlow(INPUT_MESSAGE_COLOR, defaultColor("input_text"))
    override val inputMessageHintColor = colorFlow(INPUT_MESSAGEHINT_COLOR, defaultColor("input_text_hint"))

    // --- Sizes ---
    override val appSize = sizeFlow(APP_SIZE_TEXT, 18)
    override val dateSize = sizeFlow(DATE_SIZE_TEXT, 22)
    override val clockSize = sizeFlow(CLOCK_SIZE_TEXT, 42)
    override val alarmSize = sizeFlow(ALARM_SIZE_TEXT, 20)
    override val dailyWordSize = sizeFlow(DAILY_WORD_SIZE_TEXT, 20)
    override val batterySize = sizeFlow(BATTERY_SIZE_TEXT, 14)
    override val settingsSize = sizeFlow(TEXT_SIZE_SETTINGS, 12)
    override val textPaddingSize = sizeFlow(TEXT_PADDING_SIZE, 10)

    init {
        scope.launch {
            dataSource.observeChanges().collect { key ->
                flowToKey.entries.find { it.value == key }?.let { (flow, _) ->
                    (flow as MutableStateFlow<Int>).value = dataSource.getInt(key, flow.value)
                }
            }
        }
    }

    override fun setColor(property: StateFlow<Int>, value: Int) {
        val key = flowToKey[property] ?: return
        dataSource.putInt(key, value)
        (property as MutableStateFlow<Int>).value = value
    }

    override fun setSize(property: StateFlow<Int>, value: Int) = setColor(property, value)

    override fun saveToTheme(colorNames: List<String>): String {
        val allPrefs = dataSource.all()
        val filtered = mutableMapOf<String, String>()
        for (name in colorNames) {
            val colorInt = allPrefs[name] as? Int ?: continue
            filtered[name] = String.format("#%08X", colorInt)
        }
        return serializer.serializeThemeToJson(filtered)
    }

    override fun loadFromTheme(json: String) {
        val all = serializer.deserializeThemeFromJson(json)
        if (all == null) {
            context.showLongToast("Failed to parse theme JSON.")
            return
        }
        for ((key, value) in all) {
            when (value) {
                is String -> {
                    if (value.matches(Regex("^#([A-Fa-f0-9]{8})$"))) {
                        runCatching { dataSource.putInt(key, value.toColorInt()) }
                            .onFailure {
                                context.showLongToast("Invalid color format for key: $key")
                                AppLogger.e("ThemeRepo", "Invalid color: $key=$value", it)
                            }
                    } else {
                        context.showLongToast("Unsupported HEX format for key: $key")
                    }
                }
                null -> AppLogger.e("ThemeRepo", "Null value for key: $key")
                else -> AppLogger.e("ThemeRepo", "Unsupported type for key: $key")
            }
        }
    }
}
