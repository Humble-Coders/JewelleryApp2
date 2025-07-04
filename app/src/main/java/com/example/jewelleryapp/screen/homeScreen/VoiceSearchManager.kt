package com.example.jewelleryapp.screen.homeScreen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class VoiceSearchManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onReadyForSpeech: () -> Unit = {},
    private val onBeginningOfSpeech: () -> Unit = {},
    private val onEndOfSpeech: () -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = AtomicBoolean(false)
    private var hasBegunSpeech = AtomicBoolean(false) // Prevent multiple calls

    companion object {
        private const val TAG = "VoiceSearchManager"
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            if (isListening.get()) {
                try {
                    onReadyForSpeech()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onReadyForSpeech callback", e)
                }
            }
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech")
            // Use atomic boolean to prevent infinite loop
            if (isListening.get() && hasBegunSpeech.compareAndSet(false, true)) {
                try {
                    onBeginningOfSpeech()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onBeginningOfSpeech callback", e)
                }
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Optional: Use this for volume level indicators
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
            if (isListening.get()) {
                isListening.set(false)
                hasBegunSpeech.set(false)
                try {
                    onEndOfSpeech()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onEndOfSpeech callback", e)
                }
            }
        }

        override fun onError(error: Int) {
            Log.e(TAG, "Speech recognition error: $error")
            isListening.set(false)
            hasBegunSpeech.set(false)

            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please try again."
                SpeechRecognizer.ERROR_CLIENT -> "Recognition client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error. Check your connection."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy. Try again."
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Please speak sooner."
                else -> "Voice recognition error ($error)"
            }

            try {
                onError(errorMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onError callback", e)
            }
        }

        override fun onResults(results: Bundle?) {
            Log.d(TAG, "Speech recognition results received")
            isListening.set(false)
            hasBegunSpeech.set(false)

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "Recognition matches: $matches")

            try {
                if (!matches.isNullOrEmpty()) {
                    val result = matches[0].trim()
                    if (result.isNotEmpty()) {
                        onResult(result)
                    } else {
                        onError("Empty speech result")
                    }
                } else {
                    onError("No speech detected")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onResults callback", e)
                onError("Error processing speech result")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "Partial results: $matches")
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening() {
        if (isListening.get()) {
            Log.w(TAG, "Already listening")
            return
        }

        if (!hasPermission()) {
            onError("Microphone permission required")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
            return
        }

        try {
            Log.d(TAG, "Starting speech recognition")

            // Clean up any existing recognizer
            stopListening()
            destroy()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            if (speechRecognizer == null) {
                onError("Failed to create speech recognizer")
                return
            }

            speechRecognizer?.setRecognitionListener(recognitionListener)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                // Timeout settings
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            }

            isListening.set(true)
            hasBegunSpeech.set(false)
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Speech recognition started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognition", e)
            isListening.set(false)
            hasBegunSpeech.set(false)
            onError("Failed to start voice recognition: ${e.message}")
        }
    }

    fun stopListening() {
        Log.d(TAG, "Stopping speech recognition")
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
        isListening.set(false)
        hasBegunSpeech.set(false)
    }

    fun cancel() {
        Log.d(TAG, "Cancelling speech recognition")
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
        isListening.set(false)
        hasBegunSpeech.set(false)
    }

    fun destroy() {
        Log.d(TAG, "Destroying speech recognizer")
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
        isListening.set(false)
        hasBegunSpeech.set(false)
    }

    fun isCurrentlyListening(): Boolean = isListening.get()
}