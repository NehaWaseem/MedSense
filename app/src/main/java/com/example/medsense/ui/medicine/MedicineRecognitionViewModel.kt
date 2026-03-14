package com.example.medsense.ui.medicine

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medsense.data.remote.NetworkModule
import com.example.medsense.data.remote.RemoteMedicineRepository
import com.example.medsense.domain.MedicineInfo
import com.example.medsense.ml.MlKitOcrProcessor
import com.example.medsense.ml.OcrProcessor
import com.example.medsense.ml.OcrResult
import com.example.medsense.util.SpeechFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MedicineRecognitionUiState(
    val isCameraPermissionGranted: Boolean = false,
    val isProcessing: Boolean = false,
    val recognizedText: String = "",
    val matchedMedicine: MedicineInfo? = null,
    val errorMessage: String? = null,
    val ttsOutput: String? = null
)

class MedicineRecognitionViewModel(
    private val repository: RemoteMedicineRepository = RemoteMedicineRepository(
        openFdaApi = NetworkModule.createOpenFdaApi()
    ),
    private val ocrProcessor: OcrProcessor = MlKitOcrProcessor()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicineRecognitionUiState())
    val uiState: StateFlow<MedicineRecognitionUiState> = _uiState

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            isCameraPermissionGranted = granted,
            errorMessage = if (!granted) "Camera permission is required to scan medicines." else null
        )
    }

    fun onImageCaptured(uri: Uri, context: Context) {
        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            errorMessage = null,
            ttsOutput = "Analyzing"
        )

        ocrProcessor.processImage(
            context = context,
            imageUri = uri
        ) { result ->
            if (result.candidates.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    recognizedText = "",
                    matchedMedicine = null,
                    errorMessage = "Could not read text from the image.",
                    ttsOutput = "I'm not confident this is a medicine package."
                )
                return@processImage
            }
            onOcrResultRecognized(result)
        }
    }

    private fun onOcrResultRecognized(result: OcrResult) {
        viewModelScope.launch {
            val matched = repository.findByOcrResult(result)

            if (matched == null) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    recognizedText = result.fullText,
                    matchedMedicine = null,
                    errorMessage = "No matching medicine found.",
                    ttsOutput = "I'm not confident this is a medicine package."
                )
            } else {
                val speechText = SpeechFormatter.formatForSpeech(matched)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    recognizedText = result.fullText,
                    matchedMedicine = matched,
                    errorMessage = if (!matched.isConfident) "Possible match found." else null,
                    ttsOutput = speechText
                )
            }
        }
    }
}
