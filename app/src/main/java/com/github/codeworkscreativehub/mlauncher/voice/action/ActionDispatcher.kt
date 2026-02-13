package com.github.codeworkscreativehub.mlauncher.voice.action

import com.github.codeworkscreativehub.mlauncher.voice.nlu.VoiceAction

sealed class ActionResult {
    object Success : ActionResult()
    data class NeedsConfirmation(val message: String) : ActionResult()
    data class NeedsPermission(val permission: String) : ActionResult()
    data class Failed(val reason: String) : ActionResult()
}

interface ActionDispatcher {
    suspend fun execute(action: VoiceAction): ActionResult
}
