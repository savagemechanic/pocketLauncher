package com.github.codeworkscreativehub.mlauncher.voice

enum class VoiceSessionState {
    IDLE,
    LISTENING,
    PROCESSING,
    EXECUTING,
    FEEDBACK,
    ERROR
}

interface VoiceSessionManager {
    val state: VoiceSessionState
    fun startSession()
    fun cancelSession()
    fun destroy()
}
