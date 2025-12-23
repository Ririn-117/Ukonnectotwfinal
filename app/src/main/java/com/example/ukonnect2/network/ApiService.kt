package com.example.ukonnect2.network

import retrofit2.Call
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody

// =========================
// API Service
// =========================
interface ApiService {

    // Login & Register
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    // FCM Token
    @POST("fcm-token")
    suspend fun updateFCMToken(@Body token: String): MessageResponse

    // Aktivitas
    @GET("aktivitas")
    suspend fun getAktivitas(): List<AktivitasDto>

    @POST("aktivitas")
    suspend fun createAktivitas(@Body request: AktivitasUpsertRequest): AktivitasDto

    @PUT("aktivitas/{id}")
    suspend fun updateAktivitas(
        @Path("id") id: Int,
        @Body request: AktivitasUpsertRequest
    ): AktivitasDto

    @DELETE("aktivitas/{id}")
    suspend fun deleteAktivitas(@Path("id") id: Int): MessageResponse

    // Alat (dari server SQLite)
    @GET("alat")
    suspend fun getAlat(): List<AlatDto>

    // Peminjaman
    @GET("peminjaman")
    suspend fun getPeminjaman(): List<PeminjamanDto>

    @POST("peminjaman")
    suspend fun createPeminjaman(@Body request: PeminjamanCreateRequest): PeminjamanDto

    @POST("peminjaman/{id}/kembalikan")
    suspend fun kembalikanPeminjaman(
        @Path("id") id: String,
        @Body request: PeminjamanKembalikanRequest
    ): PeminjamanKembalikanResponse

    @DELETE("peminjaman/{id}")
    suspend fun deletePeminjaman(@Path("id") id: String): MessageResponse

    // Galeri
    @GET("galeri")
    suspend fun getGaleri(): List<GaleriDto>

    @Multipart
    @POST("galeri")
    suspend fun uploadGaleri(
        @Part photo: MultipartBody.Part,
        @Part("keterangan") keterangan: RequestBody,
        @Part("tanggal") tanggal: RequestBody,
        @Part("hari") hari: RequestBody
    ): GaleriDto

    @DELETE("galeri/{id}")
    suspend fun deleteGaleri(@Path("id") id: Int): MessageResponse

    // Absensi
    @GET("absensi")
    suspend fun getAbsensi(): List<AbsensiDto>

    @POST("absensi")
    suspend fun upsertAbsensi(@Body request: AbsensiUpsertRequest): MessageResponse

    @DELETE("absensi/{id}")
    suspend fun deleteAbsensi(@Path("id") id: String): MessageResponse
}

// =========================
// Models
// =========================

// Login/Register
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val message: String, val user: String?, val userId: Int?, val token: String?)
data class RegisterRequest(val username: String, val password: String)
data class RegisterResponse(val message: String, val userId: Int?)

// Aktivitas
data class AktivitasDto(
    val id: Int,
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val status: String
)
data class AktivitasUpsertRequest(
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val status: String
)
data class MessageResponse(val message: String)

// Alat
data class AlatDto(
    val id: String,
    val nama: String,
    val stokTersedia: Int,
    val stokTotal: Int,
    val iconKey: String?
)

// Peminjaman
data class PeminjamanDto(
    val id: String,
    val alatId: String,
    val namaEquipment: String,
    val waktuPinjam: Long,
    val tanggalMulai: Long,
    val tanggalSelesai: Long,
    val jumlah: Int,
    val status: String
)
data class PeminjamanCreateRequest(
    val alatId: String,
    val qty: Int,
    val tanggalMulai: Long,
    val tanggalSelesai: Long
)
data class PeminjamanKembalikanRequest(val qty: Int)
data class PeminjamanKembalikanResponse(val message: String, val id: String, val jumlahSisa: Int, val status: String)

// Galeri
data class GaleriDto(
    val id: Int,
    val imageUrl: String,
    val keterangan: String,
    val tanggal: String,
    val hari: String,
    val uploadedByUserId: Int,
    val createdAtMillis: Long
)

// Absensi
data class AbsensiDto(
    val id: String,
    val tanggal: String,
    val jam: String,
    val tipe: String,
    val qrValue: String,
    val status: String,
    val createdAtMillis: Long
)
data class AbsensiUpsertRequest(
    val id: String,
    val tanggal: String,
    val jam: String,
    val tipe: String,
    val qrValue: String,
    val status: String
)
