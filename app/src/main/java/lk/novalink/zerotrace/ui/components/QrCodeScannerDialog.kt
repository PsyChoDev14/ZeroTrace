package lk.novalink.zerotrace.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import lk.novalink.zerotrace.ui.theme.ZtAccent
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZtBorder
import lk.novalink.zerotrace.ui.theme.ZtSurface2
import lk.novalink.zerotrace.ui.theme.ZtText
import lk.novalink.zerotrace.ui.theme.ZtTextMuted
import java.util.concurrent.Executors

@Composable
fun QrCodeScannerDialog(
    onDismiss: () -> Unit,
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            if (!rawValue.isNullOrBlank()) {
                                onQrCodeScanned(rawValue)
                                return@addOnSuccessListener
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ZtBg
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    CameraScannerPreview(
                        onQrCodeScanned = onQrCodeScanned,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Permission Request Fallback View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = ZtAccent,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Permission Required",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZtText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please allow camera access to scan Xray config QR codes directly.",
                            fontSize = 13.sp,
                            color = ZtTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = ZtAccent)
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Top Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ZtBg.copy(alpha = 0.85f))
                            .border(1.dp, ZtBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ZtText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Scan Config QR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZtText
                    )

                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ZtBg.copy(alpha = 0.85f))
                            .border(1.dp, ZtBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Pick Image",
                            tint = ZtAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom Help Hint
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ZtBg.copy(alpha = 0.85f))
                        .border(1.dp, ZtBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Point camera at any VLESS / VMess / Trojan QR code",
                        fontSize = 12.5.sp,
                        color = ZtTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraScannerPreview(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isScanned by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy, barcodeScanner) { raw ->
                            if (!isScanned) {
                                isScanned = true
                                onQrCodeScanned(raw)
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
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Reticle Frame with laser scan line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val boxSize = (w * 0.72f).coerceAtMost(300.dp.toPx())
            val left = (w - boxSize) / 2f
            val top = (h - boxSize) / 2f
            val right = left + boxSize
            val bottom = top + boxSize

            val cornerLen = 28.dp.toPx()
            val strokeW = 3.dp.toPx()

            // Top-Left Corner
            drawLine(ZtAccent, Offset(left, top), Offset(left + cornerLen, top), strokeW, StrokeCap.Round)
            drawLine(ZtAccent, Offset(left, top), Offset(left, top + cornerLen), strokeW, StrokeCap.Round)

            // Top-Right Corner
            drawLine(ZtAccent, Offset(right, top), Offset(right - cornerLen, top), strokeW, StrokeCap.Round)
            drawLine(ZtAccent, Offset(right, top), Offset(right, top + cornerLen), strokeW, StrokeCap.Round)

            // Bottom-Left Corner
            drawLine(ZtAccent, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW, StrokeCap.Round)
            drawLine(ZtAccent, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW, StrokeCap.Round)

            // Bottom-Right Corner
            drawLine(ZtAccent, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW, StrokeCap.Round)
            drawLine(ZtAccent, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW, StrokeCap.Round)

            // Laser scan line
            val laserY = top + (boxSize * laserProgress)
            drawLine(
                color = ZtAccent.copy(alpha = 0.8f),
                start = Offset(left + 10.dp.toPx(), laserY),
                end = Offset(right - 10.dp.toPx(), laserY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onFound: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onFound(rawValue)
                        break
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
