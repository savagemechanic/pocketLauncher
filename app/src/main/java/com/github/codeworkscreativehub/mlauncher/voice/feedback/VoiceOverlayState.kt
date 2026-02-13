package com.github.codeworkscreativehub.mlauncher.voice.feedback

/**
 * Represents the visual state of the voice command overlay.
 * Drives the Compose UI via StateFlow.
 */
sealed class VoiceOverlayState {
    data object Hidden : VoiceOverlayState()
    data class Listening(val transcript: String = "") : VoiceOverlayState()
    data class Processing(val transcript: String) : VoiceOverlayState()
    data object Success : VoiceOverlayState()
    data class Confirmation(val message: String) : VoiceOverlayState()
    data class Error(val message: String) : VoiceOverlayState()
}
