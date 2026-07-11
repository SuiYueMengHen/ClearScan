package com.clearscan

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

data class CodeScanResult(val rawValue: String, val format: Int, val valueType: Int) {
    val isWebUrl: Boolean get() = rawValue.startsWith("https://", true) || rawValue.startsWith("http://", true)
}

class BarcodeAnalyzer(
    mode: ScanMode,
    private val onResult: (CodeScanResult) -> Unit,
) : ImageAnalysis.Analyzer {
    private val processing = AtomicBoolean(false)
    private var lastValue = ""
    private var lastResultAt = 0L
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().apply {
            if (mode == ScanMode.QrCode) {
                setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            } else {
                setBarcodeFormats(
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                )
            }
        }.build(),
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !processing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { results ->
                val barcode = results.firstOrNull { !it.rawValue.isNullOrBlank() } ?: return@addOnSuccessListener
                val value = barcode.rawValue.orEmpty()
                val now = System.currentTimeMillis()
                if (value != lastValue || now - lastResultAt > 2_000L) {
                    lastValue = value
                    lastResultAt = now
                    onResult(CodeScanResult(value, barcode.format, barcode.valueType))
                }
            }
            .addOnFailureListener { AppLogger.w("CodeScan", "Frame analysis failed: ${it.message}") }
            .addOnCompleteListener {
                processing.set(false)
                imageProxy.close()
            }
    }

    fun close() = scanner.close()
}
