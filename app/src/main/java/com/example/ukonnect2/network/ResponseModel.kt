package com.example.ukonnect2.network

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)
