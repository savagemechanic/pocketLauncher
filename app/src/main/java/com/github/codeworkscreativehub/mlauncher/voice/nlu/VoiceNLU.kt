package com.github.codeworkscreativehub.mlauncher.voice.nlu

import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.ContactListItem
import com.github.codeworkscreativehub.mlauncher.data.Constants.Action

sealed class VoiceAction {
    data class LaunchApp(val packageName: String, val profileType: String = "SYSTEM") : VoiceAction()
    data class CallContact(val name: String, val number: String? = null) : VoiceAction()
    data class SendMessage(val recipient: String, val body: String) : VoiceAction()
    data class SystemAction(val action: Action) : VoiceAction()
    data class OpenUrl(val url: String) : VoiceAction()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String? = null) : VoiceAction()
    data class DeviceSetting(val setting: String, val enable: Boolean) : VoiceAction()
    data class AccessibilityAction(val description: String) : VoiceAction()
    data class Clarification(val message: String) : VoiceAction()
    data class CompoundAction(val actions: List<VoiceAction>) : VoiceAction()
    data class Unsupported(val message: String) : VoiceAction()
}

data class PhoneContext(
    val installedApps: List<AppListItem>,
    val recentApps: List<String>,
    val contacts: List<ContactListItem>,
    val availableActions: List<String>,
    val currentTime: String
)

interface VoiceNLU {
    suspend fun resolveIntent(transcript: String, context: PhoneContext): VoiceAction
}
