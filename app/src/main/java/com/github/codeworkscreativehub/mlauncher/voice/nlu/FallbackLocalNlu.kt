package com.github.codeworkscreativehub.mlauncher.voice.nlu

import android.content.Context
import com.github.codeworkscreativehub.fuzzywuzzy.FuzzyFinder
import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.Constants.Action
import com.github.codeworkscreativehub.mlauncher.data.ContactListItem

/**
 * Offline NLU using keyword/regex matching with fuzzy fallback.
 *
 * Pattern matching priority (greedy):
 *   1. Exact keyword commands ("lock", "screenshot", etc.)
 *   2. "open X" / "launch X" → fuzzy app match
 *   3. "call X" → fuzzy contact match
 *   4. "message X ..." / "text X ..." → SendMessage
 *   5. "set alarm HH:MM" → SetAlarm
 *   6. Raw transcript → fuzzy app search fallback
 */
class FallbackLocalNlu(private val context: Context) : VoiceNLU {

    override suspend fun resolveIntent(transcript: String, context: PhoneContext): VoiceAction {
        val input = transcript.trim().lowercase()

        // 1. System action keywords
        resolveSystemAction(input)?.let { return it }

        // 2. "open X" / "launch X" / "start X"
        resolveOpenCommand(input, context)?.let { return it }

        // 3. "call X"
        resolveCallCommand(input, context)?.let { return it }

        // 4. "message X ..." / "text X ..." / "send message to X ..."
        resolveMessageCommand(input, context)?.let { return it }

        // 5. "set alarm HH:MM" / "set alarm for 7 30"
        resolveAlarmCommand(input)?.let { return it }

        // 6. Device settings
        resolveDeviceSetting(input)?.let { return it }

        // 7. Fuzzy app fallback — the 80% case
        return fuzzyMatchApp(input, context)
    }

    private fun resolveSystemAction(input: String): VoiceAction? {
        return when {
            input == "lock" || input == "lock screen" || input == "lock phone" ->
                VoiceAction.SystemAction(Action.LockScreen)
            input == "screenshot" || input == "take screenshot" || input == "take a screenshot" ->
                VoiceAction.SystemAction(Action.TakeScreenShot)
            input == "recents" || input == "recent apps" || input == "show recents" ->
                VoiceAction.SystemAction(Action.ShowRecents)
            input == "notifications" || input == "show notifications" ->
                VoiceAction.SystemAction(Action.ShowNotification)
            input == "quick settings" || input == "show quick settings" ->
                VoiceAction.SystemAction(Action.OpenQuickSettings)
            input == "power" || input == "power menu" || input == "power dialog" ->
                VoiceAction.SystemAction(Action.OpenPowerDialog)
            else -> null
        }
    }

    private fun resolveOpenCommand(input: String, phoneContext: PhoneContext): VoiceAction? {
        val prefixes = listOf("open ", "launch ", "start ")
        val appName = prefixes.firstNotNullOfOrNull { prefix ->
            if (input.startsWith(prefix)) input.removePrefix(prefix).trim() else null
        } ?: return null

        if (appName.isBlank()) return null
        return fuzzyMatchApp(appName, phoneContext)
    }

    private fun resolveCallCommand(input: String, phoneContext: PhoneContext): VoiceAction? {
        val prefixes = listOf("call ", "dial ", "phone ")
        val contactName = prefixes.firstNotNullOfOrNull { prefix ->
            if (input.startsWith(prefix)) input.removePrefix(prefix).trim() else null
        } ?: return null

        if (contactName.isBlank()) return null
        return fuzzyMatchContact(contactName, phoneContext)
    }

    private fun resolveMessageCommand(input: String, phoneContext: PhoneContext): VoiceAction? {
        val patterns = listOf(
            Regex("^(?:message|text|send (?:a )?message to) (.+?) (?:saying|that|with message) (.+)$"),
            Regex("^(?:message|text) (.+)$")
        )

        for (pattern in patterns) {
            val match = pattern.find(input) ?: continue
            val recipientName = match.groupValues[1].trim()
            val body = if (match.groupValues.size > 2) match.groupValues[2].trim() else ""

            val contact = findBestContact(recipientName, phoneContext)
            return if (contact != null) {
                VoiceAction.SendMessage(
                    recipient = contact.phoneNumber.ifBlank { contact.displayName },
                    body = body
                )
            } else {
                VoiceAction.SendMessage(recipient = recipientName, body = body)
            }
        }
        return null
    }

    private fun resolveAlarmCommand(input: String): VoiceAction? {
        val patterns = listOf(
            Regex("^set (?:an )?alarm (?:for )?(?:at )?(\\d{1,2})[: ](\\d{2})\\s*(.*)$"),
            Regex("^(?:wake me|alarm) (?:at|for) (\\d{1,2})[: ](\\d{2})\\s*(.*)$")
        )

        for (pattern in patterns) {
            val match = pattern.find(input) ?: continue
            val hour = match.groupValues[1].toIntOrNull() ?: continue
            val minute = match.groupValues[2].toIntOrNull() ?: continue
            val label = match.groupValues[3].trim().takeIf { it.isNotEmpty() }
            if (hour in 0..23 && minute in 0..59) {
                return VoiceAction.SetAlarm(hour, minute, label)
            }
        }
        return null
    }

    private fun resolveDeviceSetting(input: String): VoiceAction? {
        val togglePatterns = listOf(
            Regex("^(?:turn |switch |toggle )(on|off) (.+)$"),
            Regex("^(?:enable|disable) (.+)$"),
            Regex("^(.+?) (on|off)$")
        )

        for (pattern in togglePatterns) {
            val match = pattern.find(input) ?: continue
            val groups = match.groupValues.drop(1)

            val (setting, enable) = when {
                groups.size == 2 && groups[0] in listOf("on", "off") ->
                    groups[1] to (groups[0] == "on")
                groups.size == 2 && groups[1] in listOf("on", "off") ->
                    groups[0] to (groups[1] == "on")
                groups.size == 1 -> {
                    val isEnable = input.startsWith("enable")
                    groups[0] to isEnable
                }
                else -> continue
            }

            val normalizedSetting = setting.trim().lowercase()
            if (normalizedSetting in listOf("wifi", "bluetooth", "airplane mode", "flashlight", "location")) {
                return VoiceAction.DeviceSetting(normalizedSetting, enable)
            }
        }
        return null
    }

    private fun fuzzyMatchApp(query: String, phoneContext: PhoneContext): VoiceAction {
        val bestApp = findBestApp(query, phoneContext)
        return if (bestApp != null) {
            VoiceAction.LaunchApp(bestApp.activityPackage, bestApp.profileType)
        } else {
            VoiceAction.Unsupported("No matching app found for: $query")
        }
    }

    private fun findBestApp(query: String, phoneContext: PhoneContext): AppListItem? {
        if (phoneContext.installedApps.isEmpty()) return null

        val topScore = 100
        val threshold = 25

        return phoneContext.installedApps
            .map { app -> app to FuzzyFinder.scoreApp(context, app, query, topScore) }
            .filter { it.second > threshold }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun fuzzyMatchContact(query: String, phoneContext: PhoneContext): VoiceAction {
        val contact = findBestContact(query, phoneContext)
        return if (contact != null) {
            VoiceAction.CallContact(contact.displayName, contact.phoneNumber)
        } else {
            VoiceAction.Clarification("No contact found matching: $query")
        }
    }

    private fun findBestContact(query: String, phoneContext: PhoneContext): ContactListItem? {
        if (phoneContext.contacts.isEmpty()) return null

        val topScore = 100
        val threshold = 25

        return phoneContext.contacts
            .map { contact -> contact to FuzzyFinder.scoreContact(contact, query, topScore) }
            .filter { it.second > threshold }
            .maxByOrNull { it.second }
            ?.first
    }
}
