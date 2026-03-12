package com.example.medsense

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.medsense.ui.medicine.MedicineRecognitionRoute
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isTtsReady by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

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
                    coroutineScope.run {
                        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "medicine_tts")
                    }
                }
            }

            Surface(color = MaterialTheme.colorScheme.background) {
                MedicineRecognitionRoute(
                    speakText = speak
                )
            }
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}
