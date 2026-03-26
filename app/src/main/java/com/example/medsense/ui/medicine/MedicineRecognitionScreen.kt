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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medsense.util.MedicineTextFormatter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun MedicineRecognitionRoute(
    speakText: (String) -> Unit,
    triggerCapture: Boolean = false,
    onCaptureHandled: () -> Unit = {},
    isListening: Boolean = false,
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
        isListening = isListening,
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
    isListening: Boolean,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MedSense Scanner",
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
            )
            
            if (isListening) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color.Green.copy(alpha = 0.2f),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        "Listening...", 
                        color = Color.Green, 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                        
                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Dosage Instructions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = MedicineTextFormatter.formatForDisplay(med.dosage),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Safety Warnings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                        Text(
                            text = MedicineTextFormatter.formatForDisplay(med.warnings),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } ?: Text(uiState.errorMessage ?: "Ready to scan. Say 'Identify Medicine' or 'Scan' to start.")
                }

                Spacer(modifier = Modifier.height(16.dp))

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
