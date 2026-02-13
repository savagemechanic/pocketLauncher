package com.github.codeworkscreativehub.mlauncher.voice

import com.github.codeworkscreativehub.mlauncher.voice.action.ActionDispatcher
import com.github.codeworkscreativehub.mlauncher.voice.feedback.FeedbackManager
import com.github.codeworkscreativehub.mlauncher.voice.nlu.VoiceNLU
import com.github.codeworkscreativehub.mlauncher.voice.stt.SpeechToText

/**
 * Orchestrator that wires STT → NLU → ActionDispatcher → Feedback.
 * Implementations will be built in Phase 5.
 */
interface VoiceService {
    val stt: SpeechToText
    val nlu: VoiceNLU
    val dispatcher: ActionDispatcher
    val feedback: FeedbackManager
    val sessionManager: VoiceSessionManager

    fun startVoiceCommand()
    fun cancel()
    fun destroy()
}
