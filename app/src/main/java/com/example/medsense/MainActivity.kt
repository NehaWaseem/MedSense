package com.example.medsense

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.medsense.ui.medicine.MedicineRecognitionRoute
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null
    private var isSpeaking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        setContent {
            var isTtsReady by remember { mutableStateOf(false) }
            var triggerCapture by remember { mutableStateOf(false) }
            var isListening by remember { mutableStateOf(false) }

            // Initialize TTS with Progress Listener
            DisposableEffect(Unit) {
                textToSpeech = TextToSpeech(this@MainActivity) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech?.language = Locale.US
                        isTtsReady = true
                        
                        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                isSpeaking = true
                                stopListening()
                            }
                            override fun onDone(utteranceId: String?) {
                                isSpeaking = false
                                startListening()
                            }
                            override fun onError(utteranceId: String?) {
                                isSpeaking = false
                                startListening()
                            }
                        })
                    }
                }
                onDispose {
                    textToSpeech?.stop()
                    textToSpeech?.shutdown()
                }
            }

            // Initialize Speech Recognizer
            DisposableEffect(Unit) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@MainActivity)
                recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { 
                        isListening = true
                        Log.d("Speech", "Ready") 
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isListening = false }
                    override fun onError(error: Int) {
                        isListening = false
                        // Error 7 is no speech detected; we restart if not speaking
                        if (!isSpeaking) startListening()
                    }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (matches != null) {
                            processMatches(matches) { triggerCapture = true }
                        }
                        if (!isSpeaking) startListening()
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (matches != null) {
                            processMatches(matches) { triggerCapture = true }
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                startListening()

                onDispose {
                    speechRecognizer?.destroy()
                }
            }

            val speak: (String) -> Unit = { text ->
                if (isTtsReady) {
                    textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "medicine_tts")
                }
            }

            Surface(color = MaterialTheme.colorScheme.background) {
                MedicineRecognitionRoute(
                    speakText = speak,
                    triggerCapture = triggerCapture,
                    onCaptureHandled = { triggerCapture = false },
                    isListening = isListening
                )
            }
        }
    }

    private fun processMatches(matches: ArrayList<String>, onTrigger: () -> Unit) {
        for (match in matches) {
            val lower = match.lowercase()
            if (lower.contains("identify medicine") || lower.contains("scan")) {
                onTrigger()
                break
            }
        }
    }

    private fun startListening() {
        if (isSpeaking) return
        runOnUiThread {
            try {
                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                Log.e("Speech", "Start error", e)
            }
        }
    }

    private fun stopListening() {
        runOnUiThread {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
