package com.github.codeworkscreativehub.mlauncher.voice.stt

import java.util.Locale

interface SpeechToText {
    fun startListening(locale: Locale, callback: STTCallback)
    fun stopListening()
    fun isAvailable(): Boolean
    fun destroy()
}

interface STTCallback {
    fun onPartialResult(text: String)
    fun onFinalResult(text: String, confidence: Float)
    fun onError(error: STTError)
}

enum class STTError {
    NO_MATCH,
    NETWORK,
    AUDIO,
    PERMISSION_DENIED,
    NOT_AVAILABLE,
    TIMEOUT,
    UNKNOWN
}
