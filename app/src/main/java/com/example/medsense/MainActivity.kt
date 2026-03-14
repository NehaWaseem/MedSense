package com.example.medsense

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for record audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        setContent {
            var isTtsReady by remember { mutableStateOf(false) }
            var triggerCapture by remember { mutableStateOf(false) }

            // Initialize Speech Recognizer
            DisposableEffect(Unit) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@MainActivity)
                recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { Log.d("Speech", "Ready") }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Log.e("Speech", "Error: $error")
                        // Restart listening if error occurs (optional)
                        startListening()
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (matches != null) {
                            for (match in matches) {
                                if (match.lowercase().contains("identify medicine") || 
                                    match.lowercase().contains("scan")) {
                                    triggerCapture = true
                                    break
                                }
                            }
                        }
                        startListening() // Keep listening
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (matches != null) {
                            for (match in matches) {
                                if (match.lowercase().contains("identify medicine") || 
                                    match.lowercase().contains("scan")) {
                                    triggerCapture = true
                                    break
                                }
                            }
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                startListening()

                onDispose {
                    speechRecognizer?.destroy()
                }
            }

            // Initialize TTS
            DisposableEffect(Unit) {
                textToSpeech = TextToSpeech(this@MainActivity) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech?.language = Locale.US
                        isTtsReady = true
                    }
                }
                onDispose {
                    textToSpeech?.stop()
                    textToSpeech?.shutdown()
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
                    onCaptureHandled = { triggerCapture = false }
                )
            }
        }
    }

    private fun startListening() {
        runOnUiThread {
            speechRecognizer?.startListening(recognitionIntent)
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
