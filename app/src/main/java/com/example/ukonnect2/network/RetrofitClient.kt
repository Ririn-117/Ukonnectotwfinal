package com.example.ukonnect2.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 🔧 Ganti IP sesuai hasil ipconfig laptop kamu
    // Pastikan HP & laptop di jaringan WiFi yang sama
    const val BASE_URL = "http://10.235.155.99:3000/"

    val instance: ApiService by lazy {
        // Logging interceptor (opsional, tapi sangat membantu debugging)
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        // Interceptor untuk menambahkan token
        val authInterceptor = Interceptor { chain ->
            val token = SessionManager.getToken() // pastikan SessionManager tersedia
            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}
