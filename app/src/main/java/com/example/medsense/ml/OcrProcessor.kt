package com.example.medsense.ml

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

interface OcrProcessor {
    fun processImage(
        context: Context,
        imageUri: Uri,
        onResult: (String) -> Unit
    )
}

class MlKitOcrProcessor : OcrProcessor {

    override fun processImage(
        context: Context,
        imageUri: Uri,
        onResult: (String) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, imageUri)
        } catch (e: Exception) {
            Log.e("OcrProcessor", "Error loading image", e)
            onResult("")
            return
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Identify the "Title" by finding text with the largest height (likely bold/main title)
                val blocks = visionText.textBlocks
                if (blocks.isEmpty()) {
                    onResult("")
                    return@addOnSuccessListener
                }

                // Sort by height descending - the tallest text is usually the brand name
                val sortedBlocks = blocks.sortedByDescending { it.boundingBox?.height() ?: 0 }
                
                // Construct a string where lines are prioritized by visual importance
                val finalOcrText = sortedBlocks.joinToString("\n") { it.text }
                
                Log.d("OcrProcessor", "Top block: ${sortedBlocks.firstOrNull()?.text}")
                onResult(finalOcrText)
            }
            .addOnFailureListener { e ->
                Log.e("OcrProcessor", "OCR failed", e)
                onResult("")
            }
    }
}
