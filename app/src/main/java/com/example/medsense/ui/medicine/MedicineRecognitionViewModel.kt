package com.example.medsense.ui.medicine

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medsense.data.remote.NetworkModule
import com.example.medsense.data.remote.RemoteMedicineRepository
import com.example.medsense.domain.MedicineRepository
import com.example.medsense.domain.MedicineInfo
import com.example.medsense.ml.MlKitOcrProcessor
import com.example.medsense.ml.OcrProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MedicineRecognitionUiState(
    val isCameraPermissionGranted: Boolean = false,
    val isProcessing: Boolean = false,
    val recognizedText: String = "",
    val matchedMedicine: MedicineInfo? = null,
    val errorMessage: String? = null
)

class MedicineRecognitionViewModel(
    private val repository: MedicineRepository = RemoteMedicineRepository(
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
            errorMessage = null
        )

        ocrProcessor.processImage(
            context = context,
            imageUri = uri
        ) { text ->
            if (text.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    recognizedText = "",
                    matchedMedicine = null,
                    errorMessage = "Could not read text from the image. Please try again with better lighting and focus."
                )
                return@processImage
            }
            onImageTextRecognized(text)
        }
    }

    fun onImageTextRecognized(text: String) {
        viewModelScope.launch {
            val matched = repository.findByText(text)

            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                recognizedText = text,
                matchedMedicine = matched,
                errorMessage = if (matched == null) {
                    "No matching medicine found. Please try again or ensure the label is clearly visible."
                } else null
            )
        }
    }
}
