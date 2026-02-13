package com.github.codeworkscreativehub.mlauncher.voice.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.voice.action.ActionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages voice command feedback through three channels:
 * 1. Visual: VoiceOverlayState exposed as StateFlow for Compose
 * 2. Audio: TextToSpeech for spoken feedback (respects prefs.voiceTtsEnabled)
 * 3. Haptic: Vibration patterns for state changes (respects prefs.voiceHapticEnabled)
 */
class FeedbackManagerImpl(
    private val context: Context,
    private val prefs: Prefs
) : FeedbackManager {

    private val _overlayState = MutableStateFlow<VoiceOverlayState>(VoiceOverlayState.Hidden)
    val overlayState: StateFlow<VoiceOverlayState> = _overlayState.asStateFlow()

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        if (prefs.voiceTtsEnabled) {
            tts = TextToSpeech(context) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.language = Locale.getDefault()
                }
            }
        }
    }

    override fun onListeningStarted() {
        _overlayState.value = VoiceOverlayState.Listening()
        hapticTick()
    }

    override fun onPartialTranscript(text: String) {
        _overlayState.value = VoiceOverlayState.Listening(text)
    }

    override fun onProcessing(transcript: String) {
        _overlayState.value = VoiceOverlayState.Processing(transcript)
    }

    override fun onActionResult(result: ActionResult) {
        when (result) {
            is ActionResult.Success -> {
                _overlayState.value = VoiceOverlayState.Success
                hapticSuccess()
            }
            is ActionResult.NeedsConfirmation -> {
                _overlayState.value = VoiceOverlayState.Confirmation(result.message)
                speak(result.message)
            }
            is ActionResult.NeedsPermission -> {
                _overlayState.value = VoiceOverlayState.Error("Permission needed: ${result.permission}")
                hapticError()
            }
            is ActionResult.Failed -> {
                _overlayState.value = VoiceOverlayState.Error(result.reason)
                hapticError()
            }
        }
    }

    override fun onError(message: String) {
        _overlayState.value = VoiceOverlayState.Error(message)
        hapticError()
    }

    fun dismiss() {
        _overlayState.value = VoiceOverlayState.Hidden
    }

    override fun destroy() {
        _overlayState.value = VoiceOverlayState.Hidden
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun speak(text: String) {
        if (!prefs.voiceTtsEnabled || !ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_feedback")
    }

    private fun hapticTick() {
        if (!prefs.voiceHapticEnabled) return
        vibrate(longArrayOf(0, 30), -1)
    }

    private fun hapticSuccess() {
        if (!prefs.voiceHapticEnabled) return
        vibrate(longArrayOf(0, 30, 80, 30), -1)
    }

    private fun hapticError() {
        if (!prefs.voiceHapticEnabled) return
        vibrate(longArrayOf(0, 50, 50, 100), -1)
    }

    @Suppress("DEPRECATION")
    private fun vibrate(pattern: LongArray, repeat: Int) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, repeat))
            } else {
                it.vibrate(pattern, repeat)
            }
        }
    }

    companion object {
        private const val TAG = "FeedbackManager"
    }
}
