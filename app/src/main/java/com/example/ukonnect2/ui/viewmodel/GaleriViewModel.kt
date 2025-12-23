package com.example.ukonnect2.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.ukonnect2.network.RetrofitClient
import com.example.ukonnect2.network.GaleriDto

data class FotoItem(
    val id: Int,
    val uri: Uri,
    val keterangan: String,
    val tanggal: String,
    val hari: String
)

class GaleriViewModel : ViewModel() {

    private val api = RetrofitClient.instance

    private fun dtoToItem(dto: GaleriDto): FotoItem {
        val absolute = RetrofitClient.BASE_URL.trimEnd('/') + "/" + dto.imageUrl.trimStart('/')
        return FotoItem(
            id = dto.id,
            uri = Uri.parse(absolute),
            keterangan = dto.keterangan,
            tanggal = dto.tanggal,
            hari = dto.hari
        )
    }

    // Menyimpan semua foto dalam galeri
    var galeriList = mutableStateListOf<FotoItem>()
        private set

    // Menyimpan foto yang baru dipilih dari galeri
    var selectedImageUri = mutableStateOf<Uri?>(null)

    fun refresh() {
        viewModelScope.launch {
            try {
                val items = api.getGaleri()
                galeriList.clear()
                galeriList.addAll(items.map { dtoToItem(it) })
            } catch (e: Exception) {
                // Log error untuk debugging
                e.printStackTrace()
                // TODO: Tampilkan error ke user
            }
        }
    }

    // Tambah foto baru dengan retry mechanism
    fun addFoto(context: Context, uri: Uri, keterangan: String, tanggal: String, hari: String) {
        viewModelScope.launch {
            var retryCount = 0
            val maxRetries = 3
            
            while (retryCount < maxRetries) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                        Toast.makeText(context, "Gagal membuka file gambar", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    val bytes = inputStream.use { it.readBytes() }
                    
                    // Validate file size
                    if (bytes.isEmpty()) {
                        Toast.makeText(context, "File gambar kosong", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    
                    // Get file name from URI or create unique name
                    val fileName = "photo_${System.currentTimeMillis()}.jpg"
                    val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("photo", fileName, requestBody)

                    val result = api.uploadGaleri(
                        photo = part,
                        keterangan = keterangan.toRequestBody("text/plain".toMediaTypeOrNull()),
                        tanggal = tanggal.toRequestBody("text/plain".toMediaTypeOrNull()),
                        hari = hari.toRequestBody("text/plain".toMediaTypeOrNull())
                    )
                    
                    // Success - refresh and show success message
                    refresh()
                    Toast.makeText(context, "Foto berhasil diupload!", Toast.LENGTH_SHORT).show()
                    return@launch
                    
                } catch (e: Exception) {
                    retryCount++
                    e.printStackTrace()
                    
                    if (retryCount >= maxRetries) {
                        // Final failure - show detailed error
                        val errorMsg = when {
                            e.message?.contains("timeout", true) == true -> "Koneksi timeout. Coba lagi."
                            e.message?.contains("network", true) == true -> "Tidak ada koneksi internet."
                            e.message?.contains("500", true) == true -> "Server error. Coba beberapa saat lagi."
                            else -> "Upload gagal: ${e.message}"
                        }
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    } else {
                        // Retry with delay
                        kotlinx.coroutines.delay(1000L * retryCount)
                    }
                }
            }
        }
    }

    // Hapus foto berdasarkan ID
    fun deleteFoto(id: Int) {
        viewModelScope.launch {
            try {
                api.deleteGaleri(id)
                refresh()
            } catch (e: Exception) {
                // Log error untuk debugging
                e.printStackTrace()
                // TODO: Tampilkan error ke user
            }
        }
    }

    // Update data foto (edit)
    fun updateFoto(context: Context, item: FotoItem, uri: Uri, keterangan: String, tanggal: String, hari: String) {
        // Backend saat ini tidak punya endpoint update galeri.
        // Solusi sederhana: hapus item lama (jika milik sendiri) lalu upload baru.
        deleteFoto(item.id)
        addFoto(context, uri, keterangan, tanggal, hari)
    }

    // Bersihkan foto yang dipilih setelah disimpan
    fun clearSelectedImage() {
        selectedImageUri.value = null
    }
}
