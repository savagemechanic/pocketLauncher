package com.github.codeworkscreativehub.mlauncher.voice

import com.github.codeworkscreativehub.common.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FSM-based session manager with deterministic state transitions.
 *
 * State machine:
 *   IDLE → LISTENING → PROCESSING → EXECUTING → FEEDBACK → IDLE
 *   Any state → ERROR → IDLE (on timeout/failure)
 *
 * The FSM register is a MutableStateFlow, enabling reactive observation
 * from both the orchestrator and UI layer.
 */
class VoiceSessionManagerImpl : VoiceSessionManager {

    private val _stateFlow = MutableStateFlow(VoiceSessionState.IDLE)
    val stateFlow: StateFlow<VoiceSessionState> = _stateFlow.asStateFlow()

    override val state: VoiceSessionState
        get() = _stateFlow.value

    override fun startSession() {
        transition(VoiceSessionState.IDLE, VoiceSessionState.LISTENING)
    }

    override fun cancelSession() {
        _stateFlow.value = VoiceSessionState.IDLE
        AppLogger.d(TAG, "Session cancelled → IDLE")
    }

    override fun destroy() {
        _stateFlow.value = VoiceSessionState.IDLE
    }

    fun transitionToProcessing() {
        transition(VoiceSessionState.LISTENING, VoiceSessionState.PROCESSING)
    }

    fun transitionToExecuting() {
        transition(VoiceSessionState.PROCESSING, VoiceSessionState.EXECUTING)
    }

    fun transitionToFeedback() {
        transition(VoiceSessionState.EXECUTING, VoiceSessionState.FEEDBACK)
    }

    fun transitionToIdle() {
        _stateFlow.value = VoiceSessionState.IDLE
        AppLogger.d(TAG, "→ IDLE")
    }

    fun transitionToError() {
        _stateFlow.value = VoiceSessionState.ERROR
        AppLogger.d(TAG, "→ ERROR")
    }

    private fun transition(expected: VoiceSessionState, next: VoiceSessionState) {
        if (_stateFlow.value != expected) {
            AppLogger.w(TAG, "Invalid transition: ${_stateFlow.value} → $next (expected $expected)")
            return
        }
        _stateFlow.value = next
        AppLogger.d(TAG, "$expected → $next")
    }

    companion object {
        private const val TAG = "VoiceSessionManager"
    }
}
