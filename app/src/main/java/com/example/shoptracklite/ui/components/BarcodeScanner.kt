package com.example.shoptracklite.ui.components

import android.Manifest
import android.media.ToneGenerator
import android.media.AudioManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(0.95f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan Barcode",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Camera preview or permission request
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        cameraPermissionState.status.isGranted -> {
                            CameraPreview(
                                onBarcodeScanned = { barcode ->
                                    onBarcodeScanned(barcode)
                                    onDismiss()
                                }
                            )
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Camera permission is required to scan barcodes",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Button(
                                    onClick = { cameraPermissionState.launchPermissionRequest() }
                                ) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    }
                }

                // Instructions
                Text(
                    text = "Position the barcode within the frame",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasScanned by remember { mutableStateOf(false) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val barcodeScanner = BarcodeScanning.getClient()
                val analysisExecutor = Executors.newSingleThreadExecutor()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy) { barcode ->
                        if (!hasScanned) {
                            hasScanned = true
                            // Play beep sound
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                            onBarcodeScanned(barcode)
                        }
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(
            image,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        onBarcodeDetected(value)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } ?: imageProxy.close()
}

/**
 * Bluetooth Barcode Detector
 * 
 * A composable that wraps content and captures keyboard input from Bluetooth barcode readers.
 * Bluetooth scanners send barcodes as rapid HID keyboard input, typically ending with Enter.
 * 
 * @param enabled Whether the detector is active
 * @param onBarcodeScanned Callback when a barcode is detected
 * @param content The content to wrap
 */
@Composable
fun BluetoothBarcodeDetector(
    enabled: Boolean = true,
    onBarcodeScanned: (String) -> Unit,
    content: @Composable () -> Unit
) {
    // Buffer to accumulate barcode characters
    var barcodeBuffer by remember { mutableStateOf("") }
    var lastInputTime by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()
    var timeoutJob by remember { mutableStateOf<Job?>(null) }
    
    // Tone generator for beep feedback
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    
    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }
    
    // Focus requester to capture keyboard events
    val focusRequester = remember { FocusRequester() }
    
    // Request focus when enabled
    LaunchedEffect(enabled) {
        if (enabled) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus request may fail if not yet attached
            }
        }
    }
    
    // Constants for detection
    val bufferTimeoutMs = 500L  // Clear buffer if no Enter received
    val minBarcodeLength = 3   // Minimum valid barcode length
    
    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable(enabled)
            .onKeyEvent { event ->
                if (!enabled) return@onKeyEvent false
                
                // Only handle key down events
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                
                val currentTime = System.currentTimeMillis()
                
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        // Enter key - process the barcode
                        if (barcodeBuffer.length >= minBarcodeLength) {
                            val barcode = barcodeBuffer.trim()
                            barcodeBuffer = ""
                            timeoutJob?.cancel()
                            
                            // Play beep sound
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                            
                            // Trigger callback
                            onBarcodeScanned(barcode)
                            return@onKeyEvent true
                        } else {
                            // Too short, clear buffer
                            barcodeBuffer = ""
                            timeoutJob?.cancel()
                        }
                        false
                    }
                    Key.Backspace -> {
                        // Allow backspace to correct mistakes
                        if (barcodeBuffer.isNotEmpty()) {
                            barcodeBuffer = barcodeBuffer.dropLast(1)
                        }
                        false
                    }
                    Key.Escape -> {
                        // Clear buffer on escape
                        barcodeBuffer = ""
                        timeoutJob?.cancel()
                        false
                    }
                    else -> {
                        // Get the character from the key event
                        val char = event.utf16CodePoint.toChar()
                        
                        // Only accept printable characters (letters, digits, common barcode chars)
                        if (char.isLetterOrDigit() || char in "-_.") {
                            barcodeBuffer += char
                            lastInputTime = currentTime
                            
                            // Cancel previous timeout and start new one
                            timeoutJob?.cancel()
                            timeoutJob = scope.launch {
                                delay(bufferTimeoutMs)
                                // Timeout - clear buffer if Enter wasn't pressed
                                barcodeBuffer = ""
                            }
                            return@onKeyEvent true
                        }
                        false
                    }
                }
            }
    ) {
        content()
    }
}

