package com.github.codeworkscreativehub.mlauncher.voice.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import androidx.fragment.app.Fragment
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.MainViewModel
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.helper.initActionService
import com.github.codeworkscreativehub.mlauncher.services.ActionService
import com.github.codeworkscreativehub.mlauncher.voice.nlu.VoiceAction
import kotlinx.coroutines.delay

/**
 * Exhaustive action dispatcher mapping VoiceAction sealed subtypes to Android intents
 * and system APIs.
 *
 * Dispatch table (sealed class → handler):
 * - LaunchApp → ViewModel.launchApp() with profile-aware UserHandle
 * - CallContact → ACTION_DIAL intent
 * - SendMessage → ACTION_SENDTO with smsto: URI
 * - SystemAction → ActionService global actions
 * - OpenUrl → ACTION_VIEW intent
 * - SetAlarm → AlarmClock.ACTION_SET_ALARM
 * - DeviceSetting → Settings intents (wifi, bluetooth, etc.)
 * - AccessibilityAction → AccessibilityActionHandler
 * - CompoundAction → sequential execution with 500ms delays
 * - Clarification → NeedsConfirmation result
 * - Unsupported → Failed result
 */
class ActionDispatcherImpl(
    private val context: Context,
    private val viewModel: MainViewModel,
    private val fragment: Fragment,
    private val prefs: Prefs
) : ActionDispatcher {

    override suspend fun execute(action: VoiceAction): ActionResult {
        return try {
            when (action) {
                is VoiceAction.LaunchApp -> launchApp(action)
                is VoiceAction.CallContact -> callContact(action)
                is VoiceAction.SendMessage -> sendMessage(action)
                is VoiceAction.SystemAction -> executeSystemAction(action)
                is VoiceAction.OpenUrl -> openUrl(action)
                is VoiceAction.SetAlarm -> setAlarm(action)
                is VoiceAction.DeviceSetting -> openDeviceSetting(action)
                is VoiceAction.AccessibilityAction -> executeAccessibilityAction(action)
                is VoiceAction.CompoundAction -> executeCompound(action)
                is VoiceAction.Clarification -> ActionResult.NeedsConfirmation(action.message)
                is VoiceAction.Unsupported -> ActionResult.Failed(action.message)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Action dispatch failed", e)
            ActionResult.Failed("Execution error: ${e.message}")
        }
    }

    private fun launchApp(action: VoiceAction.LaunchApp): ActionResult {
        val apps = viewModel.appList.value ?: return ActionResult.Failed("App list not loaded")

        val app = apps.find { it.activityPackage == action.packageName }
            ?: return ActionResult.Failed("App not found: ${action.packageName}")

        // Check if locked — biometric will be handled by ViewModel
        if (prefs.lockedApps.contains(action.packageName)) {
            viewModel.launchApp(app, fragment)
            return ActionResult.Success
        }

        viewModel.launchApp(app, fragment)
        return ActionResult.Success
    }

    private fun callContact(action: VoiceAction.CallContact): ActionResult {
        val number = action.number
        if (number.isNullOrBlank()) {
            // Try to find from contacts
            val contacts = viewModel.contactList.value ?: emptyList()
            val match = contacts.find {
                it.displayName.equals(action.name, ignoreCase = true)
            }
            if (match != null && match.phoneNumber.isNotBlank()) {
                return dialNumber(match.phoneNumber)
            }
            return ActionResult.NeedsConfirmation("No number found for ${action.name}")
        }
        return dialNumber(number)
    }

    private fun dialNumber(number: String): ActionResult {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult.Success
    }

    private fun sendMessage(action: VoiceAction.SendMessage): ActionResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(action.recipient)}")).apply {
            putExtra("sms_body", action.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult.Success
    }

    private fun executeSystemAction(action: VoiceAction.SystemAction): ActionResult {
        val service = initActionService(context)
            ?: return ActionResult.Failed("Accessibility service not available")

        val success = when (action.action) {
            com.github.codeworkscreativehub.mlauncher.data.Constants.Action.LockScreen -> service.lockScreen()
            com.github.codeworkscreativehub.mlauncher.data.Constants.Action.ShowRecents -> service.showRecents()
            com.github.codeworkscreativehub.mlauncher.data.Constants.Action.ShowNotification -> service.openNotifications()
            com.github.codeworkscreativehub.mlauncher.data.Constants.Action.OpenQuickSettings -> service.openQuickSettings()
            com.github.codeworkscreativehub.mlauncher.data.Constants.Action.OpenPowerDialog -> service.openPowerDialog()
            com.github.codeworkscreativehub.mlauncher.data.Constants.Action.TakeScreenShot -> service.takeScreenShot()
            else -> return ActionResult.Failed("Unsupported system action: ${action.action}")
        }

        return if (success) ActionResult.Success else ActionResult.Failed("System action failed")
    }

    private fun openUrl(action: VoiceAction.OpenUrl): ActionResult {
        val uri = Uri.parse(action.url.let {
            if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
        })
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult.Success
    }

    private fun setAlarm(action: VoiceAction.SetAlarm): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, action.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            action.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult.Success
    }

    private fun openDeviceSetting(action: VoiceAction.DeviceSetting): ActionResult {
        val settingsAction = when (action.setting.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "airplane mode", "airplane" -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(settingsAction).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult.Success
    }

    private fun executeAccessibilityAction(action: VoiceAction.AccessibilityAction): ActionResult {
        val service = ActionService.instance()
            ?: return ActionResult.Failed("Accessibility service not available")

        return AccessibilityActionHandler.findAndClickNode(service, action.description)
    }

    private suspend fun executeCompound(action: VoiceAction.CompoundAction): ActionResult {
        for (subAction in action.actions) {
            val result = execute(subAction)
            if (result is ActionResult.Failed) {
                return ActionResult.Failed("Compound action failed at: ${result.reason}")
            }
            delay(COMPOUND_ACTION_DELAY_MS)
        }
        return ActionResult.Success
    }

    companion object {
        private const val TAG = "ActionDispatcher"
        private const val COMPOUND_ACTION_DELAY_MS = 500L
    }
}
