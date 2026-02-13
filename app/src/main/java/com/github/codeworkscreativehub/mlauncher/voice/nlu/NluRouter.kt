package com.github.codeworkscreativehub.mlauncher.voice.nlu

import android.content.Context
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Strategy-based NLU router.
 *
 * Routes intent resolution based on user preference:
 * - If cloud AI enabled: tries CloudLLMNlu with 5s timeout, falls back to local
 * - Otherwise: uses FallbackLocalNlu directly
 *
 * This implements the Strategy pattern where the concrete strategy is selected
 * at runtime based on user configuration.
 */
class NluRouter(private val context: Context, private val prefs: Prefs) : VoiceNLU {

    private val localNlu: FallbackLocalNlu by lazy { FallbackLocalNlu(context) }
    private val cloudNlu: CloudLLMNlu by lazy { CloudLLMNlu(context) }

    override suspend fun resolveIntent(transcript: String, phoneContext: PhoneContext): VoiceAction {
        if (!prefs.voiceCloudEnabled) {
            return localNlu.resolveIntent(transcript, phoneContext)
        }

        return try {
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                cloudNlu.resolveIntent(transcript, phoneContext)
            }

            if (result != null && result !is VoiceAction.Unsupported) {
                result
            } else {
                AppLogger.d(TAG, "Cloud NLU returned null/unsupported, falling back to local")
                localNlu.resolveIntent(transcript, phoneContext)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cloud NLU failed, falling back to local", e)
            localNlu.resolveIntent(transcript, phoneContext)
        }
    }

    companion object {
        private const val TAG = "NluRouter"
        private const val CLOUD_TIMEOUT_MS = 5000L
    }
}
