package com.example.ukonnect2.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ukonnect2.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fcm_token")

class FCMTokenManager(private val context: Context) {

    private val api = RetrofitClient.instance
    private val TOKEN_KEY = stringPreferencesKey("fcm_token")

    val ioScope = CoroutineScope(Dispatchers.IO)

    /** Simpan token ke DataStore */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    /** Ambil token dari DataStore */
    suspend fun getToken(): String? {
        return context.dataStore.data
            .map { it[TOKEN_KEY] }
            .first()
    }

    /** Kirim token ke server */
    suspend fun sendTokenToServer(token: String) {
        try {
            // Kirim token ke server
            api.updateFCMToken(token)
            saveToken(token)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Initialize FCM token - dipanggil dari MainActivity */
    fun initializeFCMToken() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                ioScope.launch {
                    sendTokenToServer(token)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
