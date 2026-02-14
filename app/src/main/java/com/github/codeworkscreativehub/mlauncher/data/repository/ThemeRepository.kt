package com.github.codeworkscreativehub.mlauncher.data.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages all color/font/size theme settings.
 * Separated from SettingsRepository to keep theme concerns
 * isolated and support import/export of theme files.
 */
interface ThemeRepository {

    // --- Colors ---
    val backgroundColor: StateFlow<Int>
    val appColor: StateFlow<Int>
    val dateColor: StateFlow<Int>
    val clockColor: StateFlow<Int>
    val batteryColor: StateFlow<Int>
    val dailyWordColor: StateFlow<Int>
    val shortcutIconsColor: StateFlow<Int>
    val alarmClockColor: StateFlow<Int>
    val notesBackgroundColor: StateFlow<Int>
    val bubbleBackgroundColor: StateFlow<Int>
    val bubbleMessageTextColor: StateFlow<Int>
    val bubbleTimeDateColor: StateFlow<Int>
    val bubbleCategoryColor: StateFlow<Int>
    val inputMessageColor: StateFlow<Int>
    val inputMessageHintColor: StateFlow<Int>

    // --- Sizes ---
    val appSize: StateFlow<Int>
    val dateSize: StateFlow<Int>
    val clockSize: StateFlow<Int>
    val alarmSize: StateFlow<Int>
    val dailyWordSize: StateFlow<Int>
    val batterySize: StateFlow<Int>
    val settingsSize: StateFlow<Int>
    val textPaddingSize: StateFlow<Int>

    // --- Setters ---
    fun setColor(property: StateFlow<Int>, value: Int)
    fun setSize(property: StateFlow<Int>, value: Int)

    // --- Theme import/export ---
    fun saveToTheme(colorNames: List<String>): String
    fun loadFromTheme(json: String)
}
