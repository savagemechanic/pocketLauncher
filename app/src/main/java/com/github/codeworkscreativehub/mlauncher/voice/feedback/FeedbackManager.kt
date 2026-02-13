package com.github.codeworkscreativehub.mlauncher.voice.feedback

import com.github.codeworkscreativehub.mlauncher.voice.action.ActionResult

interface FeedbackManager {
    fun onListeningStarted()
    fun onPartialTranscript(text: String)
    fun onProcessing(transcript: String)
    fun onActionResult(result: ActionResult)
    fun onError(message: String)
    fun destroy()
}
