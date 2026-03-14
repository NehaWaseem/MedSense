package com.example.medsense.ml

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class OcrResult(
    val fullText: String,
    val candidates: List<OcrCandidate>
)

data class OcrCandidate(
    val text: String,
    val confidence: Float,
    val boxArea: Int,
    val isCenter: Boolean
)

interface OcrProcessor {
    fun processImage(
        context: Context,
        imageUri: Uri,
        onResult: (OcrResult) -> Unit
    )
}

class MlKitOcrProcessor : OcrProcessor {

    override fun processImage(
        context: Context,
        imageUri: Uri,
        onResult: (OcrResult) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, imageUri)
        } catch (e: Exception) {
            Log.e("OcrProcessor", "Error loading image", e)
            onResult(OcrResult("", emptyList()))
            return
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val candidates = mutableListOf<OcrCandidate>()
                val imgWidth = image.width
                val imgHeight = image.height

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val box = line.boundingBox ?: continue
                        val area = box.width() * box.height()
                        
                        // Check if center of line is near center of image
                        val centerX = box.centerX()
                        val centerY = box.centerY()
                        val isCenter = centerX in (imgWidth/4..3*imgWidth/4) && 
                                     centerY in (imgHeight/4..3*imgHeight/4)

                        // ML Kit provides confidence at the Element level, 
                        // so we average it for the line.
                        val avgConfidence = line.elements.map { it.confidence }.average().toFloat()

                        candidates.add(
                            OcrCandidate(
                                text = line.text,
                                confidence = avgConfidence,
                                boxArea = area,
                                isCenter = isCenter
                            )
                        )
                    }
                }

                onResult(OcrResult(visionText.text, candidates))
            }
            .addOnFailureListener { e ->
                Log.e("OcrProcessor", "OCR failed", e)
                onResult(OcrResult("", emptyList()))
            }
    }
}
