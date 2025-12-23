package com.example.ukonnect2.network

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREF_NAME = "ukonnect_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"

    private lateinit var prefs: SharedPreferences

    // Harus dipanggil sekali di Application atau MainActivity sebelum pakai
    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun setSession(token: String, userId: Int) {
        if (!::prefs.isInitialized) throw IllegalStateException("SessionManager not initialized")
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .apply()
    }

    fun clear() {
        if (!::prefs.isInitialized) throw IllegalStateException("SessionManager not initialized")
        prefs.edit().clear().apply()
    }

    fun getToken(): String? {
        if (!::prefs.isInitialized) throw IllegalStateException("SessionManager not initialized")
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUserId(): Int? {
        if (!::prefs.isInitialized) throw IllegalStateException("SessionManager not initialized")
        val id = prefs.getInt(KEY_USER_ID, -1)
        return if (id == -1) null else id
    }
}
