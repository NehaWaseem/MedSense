package com.example.medsense.ui.medicine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun MedicineRecognitionRoute(
    speakText: (String) -> Unit,
    triggerCapture: Boolean = false,
    onCaptureHandled: () -> Unit = {},
    viewModel: MedicineRecognitionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var lastSpokenOutput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.ttsOutput) {
        val output = uiState.ttsOutput
        if (output != null && output != lastSpokenOutput) {
            speakText(output)
            lastSpokenOutput = output
        }
    }

    MedicineRecognitionScreen(
        uiState = uiState,
        triggerCapture = triggerCapture,
        onCaptureHandled = onCaptureHandled,
        onCameraPermissionResult = viewModel::onCameraPermissionResult,
        onImageCaptured = viewModel::onImageCaptured
    )
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun MedicineRecognitionScreen(
    uiState: MedicineRecognitionUiState,
    triggerCapture: Boolean,
    onCaptureHandled: () -> Unit,
    onCameraPermissionResult: (Boolean) -> Unit,
    onImageCaptured: (Uri, Context) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> onCameraPermissionResult(granted) }

    DisposableEffect(Unit) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            onCameraPermissionResult(true)
        }
        onDispose { }
    }

    // Logic to auto-capture when triggered by voice
    LaunchedEffect(triggerCapture) {
        if (triggerCapture && !uiState.isProcessing && uiState.isCameraPermissionGranted) {
            val capture = imageCapture
            if (capture != null) {
                val file = createImageFile(context)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                            onImageCaptured(results.savedUri ?: Uri.fromFile(file), context)
                            onCaptureHandled()
                        }
                        override fun onError(e: ImageCaptureException) {
                            e.printStackTrace()
                            onCaptureHandled()
                        }
                    }
                )
            } else {
                onCaptureHandled()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B1A))
            .padding(16.dp)
    ) {
        Text(
            text = "MedSense Scanner",
            style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        Card(modifier = Modifier.fillMaxWidth().weight(0.4f)) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!uiState.isCameraPermissionGranted) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Enable Camera")
                        }
                    }
                } else {
                    AndroidCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onUseCases = { preview, capture ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        capture
                                    )
                                    imageCapture = capture
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth().weight(0.6f)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isProcessing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Analyzing medicine...")
                    }
                } else {
                    uiState.matchedMedicine?.let { med ->
                        Text(med.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (med.strength.isNotBlank()) Text("Strength: ${med.strength}", style = MaterialTheme.typography.bodyMedium)
                        
                        Text("Dosage:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(med.dosage, style = MaterialTheme.typography.bodyMedium)
                        
                        Text("Warnings:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(med.warnings, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    } ?: Text(uiState.errorMessage ?: "Ready to scan.")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        val file = createImageFile(context)
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                        
                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                    onImageCaptured(results.savedUri ?: Uri.fromFile(file), context)
                                }
                                override fun onError(e: ImageCaptureException) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isProcessing && uiState.isCameraPermissionGranted
                ) {
                    Text("Capture & Scan")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun AndroidCameraPreview(modifier: Modifier, onUseCases: (Preview, ImageCapture) -> Unit) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val capture = ImageCapture.Builder().setTargetRotation(ctx.display?.rotation ?: 0).build()
            onUseCases(preview, capture)
            previewView
        },
        modifier = modifier
    )
}

private fun createImageFile(context: Context): File = File.createTempFile(
    "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}",
    ".jpg",
    context.cacheDir
)
