package com.example.ukonnect2.ui.screen

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Opsi untuk memberi tahu scanner bahwa kita HANYA mencari QR Code
    private val options = com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Jika ada QR code yang terdeteksi
                    if (barcodes.isNotEmpty()) {
                        barcodes.firstOrNull()?.rawValue?.let { qrValue ->
                            // Kirim hasil pindaian (teks dari QR code)
                            onQrCodeScanned(qrValue)
                        }
                    }
                }
                .addOnFailureListener {
                    // Gagal memproses frame
                    it.printStackTrace()
                }
                .addOnCompleteListener {
                    // Selalu tutup imageProxy agar kamera bisa mengirim frame berikutnya
                    imageProxy.close()
                }
        }
    }
}