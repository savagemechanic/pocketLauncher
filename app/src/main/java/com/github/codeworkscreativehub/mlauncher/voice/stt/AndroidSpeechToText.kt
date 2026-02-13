package com.github.codeworkscreativehub.mlauncher.voice.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class AndroidSpeechToText(private val context: Context) : SpeechToText {

    private var recognizer: SpeechRecognizer? = null
    private var callback: STTCallback? = null

    override fun startListening(locale: Locale, callback: STTCallback) {
        this.callback = callback

        if (!isAvailable()) {
            callback.onError(STTError.NOT_AVAILABLE)
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        recognizer?.startListening(intent)
    }

    override fun stopListening() {
        recognizer?.stopListening()
    }

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun destroy() {
        recognizer?.destroy()
        recognizer = null
        callback = null
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            callback?.onPartialResult(text)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            val text = matches?.firstOrNull() ?: ""
            val score = confidence?.firstOrNull() ?: 0f
            if (text.isNotEmpty()) {
                callback?.onFinalResult(text, score)
            } else {
                callback?.onError(STTError.NO_MATCH)
            }
        }

        override fun onError(error: Int) {
            callback?.onError(mapError(error))
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun mapError(error: Int): STTError = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> STTError.NO_MATCH
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> STTError.NETWORK
        SpeechRecognizer.ERROR_AUDIO -> STTError.AUDIO
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> STTError.PERMISSION_DENIED
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> STTError.TIMEOUT
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> STTError.UNKNOWN
        else -> STTError.UNKNOWN
    }
}
