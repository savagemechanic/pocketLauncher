package com.github.codeworkscreativehub.mlauncher.voice

import android.content.Context
import androidx.fragment.app.Fragment
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.MainViewModel
import com.github.codeworkscreativehub.mlauncher.data.Constants.Action
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.helper.analytics.AppUsageMonitor
import com.github.codeworkscreativehub.mlauncher.voice.action.ActionDispatcher
import com.github.codeworkscreativehub.mlauncher.voice.action.ActionDispatcherImpl
import com.github.codeworkscreativehub.mlauncher.voice.feedback.FeedbackManager
import com.github.codeworkscreativehub.mlauncher.voice.feedback.FeedbackManagerImpl
import com.github.codeworkscreativehub.mlauncher.voice.feedback.VoiceOverlayState
import com.github.codeworkscreativehub.mlauncher.voice.nlu.NluRouter
import com.github.codeworkscreativehub.mlauncher.voice.nlu.PhoneContext
import com.github.codeworkscreativehub.mlauncher.voice.nlu.VoiceNLU
import com.github.codeworkscreativehub.mlauncher.voice.stt.AndroidSpeechToText
import com.github.codeworkscreativehub.mlauncher.voice.stt.STTCallback
import com.github.codeworkscreativehub.mlauncher.voice.stt.STTError
import com.github.codeworkscreativehub.mlauncher.voice.stt.SpeechToText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Orchestrator wiring STT → NLU → ActionDispatcher → Feedback.
 *
 * Lifecycle:
 * 1. startVoiceCommand() — checks IDLE state, begins STT
 * 2. STT onFinalResult → coroutine: NLU resolve → dispatch → feedback
 * 3. Auto-dismiss after 2s feedback delay
 * 4. destroy() — cleanup all components and coroutine scope
 */
class VoiceServiceImpl(
    private val context: Context,
    private val viewModel: MainViewModel,
    private val fragment: Fragment,
    private val prefs: Prefs
) : VoiceService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val stt: SpeechToText = AndroidSpeechToText(context)

    override val nlu: VoiceNLU = NluRouter(context, prefs)

    override val dispatcher: ActionDispatcher = ActionDispatcherImpl(context, viewModel, fragment, prefs)

    private val _feedback = FeedbackManagerImpl(context, prefs)
    override val feedback: FeedbackManager = _feedback

    private val _sessionManager = VoiceSessionManagerImpl()
    override val sessionManager: VoiceSessionManager = _sessionManager

    val overlayState: StateFlow<VoiceOverlayState> = _feedback.overlayState

    override fun startVoiceCommand() {
        if (_sessionManager.state != VoiceSessionState.IDLE) {
            AppLogger.d(TAG, "Not idle, current state: ${_sessionManager.state}")
            cancel()
            return
        }

        if (stt.isAvailable() != true) {
            _feedback.onError("Speech recognition not available")
            return
        }

        _sessionManager.startSession()
        _feedback.onListeningStarted()

        val locale = prefs.appLanguage.locale()
        stt.startListening(locale, object : STTCallback {
            override fun onPartialResult(text: String) {
                _feedback.onPartialTranscript(text)
            }

            override fun onFinalResult(text: String, confidence: Float) {
                processTranscript(text)
            }

            override fun onError(error: STTError) {
                val message = when (error) {
                    STTError.NO_MATCH -> "Didn't catch that"
                    STTError.PERMISSION_DENIED -> "Microphone permission required"
                    STTError.TIMEOUT -> "Listening timed out"
                    else -> "Speech recognition error"
                }
                _sessionManager.transitionToError()
                _feedback.onError(message)
                autoDismiss()
            }
        })
    }

    private fun processTranscript(transcript: String) {
        _sessionManager.transitionToProcessing()
        _feedback.onProcessing(transcript)

        scope.launch {
            try {
                val phoneContext = buildPhoneContext()

                _sessionManager.transitionToExecuting()
                val action = nlu.resolveIntent(transcript, phoneContext)

                val result = dispatcher.execute(action)

                _sessionManager.transitionToFeedback()
                _feedback.onActionResult(result)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Voice pipeline failed", e)
                _sessionManager.transitionToError()
                _feedback.onError("Something went wrong")
            }

            autoDismiss()
        }
    }

    private fun buildPhoneContext(): PhoneContext {
        val apps = viewModel.appList.value ?: emptyList()
        val contacts = viewModel.contactList.value ?: emptyList()

        val recentApps = try {
            AppUsageMonitor.createInstance(context)
                .getLastTenAppsUsed(context)
                .map { it.first }
        } catch (_: Exception) {
            emptyList()
        }

        val availableActions = Action.entries.map { it.name }

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        return PhoneContext(
            installedApps = apps,
            recentApps = recentApps,
            contacts = contacts,
            availableActions = availableActions,
            currentTime = currentTime
        )
    }

    private fun autoDismiss() {
        scope.launch {
            delay(AUTO_DISMISS_DELAY_MS)
            _sessionManager.transitionToIdle()
            _feedback.dismiss()
        }
    }

    override fun cancel() {
        stt.stopListening()
        _sessionManager.cancelSession()
        _feedback.dismiss()
    }

    override fun destroy() {
        cancel()
        stt.destroy()
        _feedback.destroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "VoiceService"
        private const val AUTO_DISMISS_DELAY_MS = 2000L
    }
}
