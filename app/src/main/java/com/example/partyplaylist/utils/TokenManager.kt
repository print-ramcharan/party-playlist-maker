package com.example.partyplaylist.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class TokenManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("SpotifyPrefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()
    private val clientId = "9e55757a811a432c88d740c04711f5a0" // Replace with your actual client ID
    private val clientSecret = "d38dcbead2224dc9a50b6a978e83c295" // Replace with your actual client secret

     fun getAccessToken(): String? {
        return SharedPreferencesManager.getAccessToken(context)
    }

    private fun getRefreshToken(): String? {
        return SharedPreferencesManager.getRefreshToken(context)
    }

    private fun getTokenExpiry(): Long {
        return SharedPreferencesManager.getTokenExpiry(context)
    }

    fun refreshTokenIfNeeded(callback: (String?) -> Unit) {
        val currentTime = System.currentTimeMillis()
        val expiryTime = getTokenExpiry()

        if (expiryTime - currentTime < 5 * 60 * 1000) { // Refresh if less than 5 minutes to expiry
            val refreshToken = getRefreshToken() ?: return callback(null)
            refreshToken(refreshToken, callback)
        } else {
            callback(getAccessToken())
        }
    }

     fun refreshToken(refreshToken: String, callback: (String?) -> Unit) {
        val tokenUrl = "https://accounts.spotify.com/api/token"
        val authHeader = "Basic " + Base64.encodeToString("$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP)

        val requestBody: RequestBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url(tokenUrl)
            .post(requestBody)
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("TokenManager", "Refresh token request failed", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody)
                    val newAccessToken = json.optString("access_token")
                    val newRefreshToken = json.optString("refresh_token")
                    val expiresIn = json.optLong("expires_in", 3600) // Default to 1 hour if not provided

                    if (newAccessToken.isNotEmpty()) {
                        saveAccessToken(newAccessToken, System.currentTimeMillis() + expiresIn * 1000)
                        if (newRefreshToken.isNotEmpty()) {
                            saveRefreshToken(newRefreshToken)
                        }
                        callback(newAccessToken)
                    } else {
                        Log.e("TokenManager", "No access token found in response")
                        callback(null)
                    }
                } else {
                    Log.e("TokenManager", "Failed to refresh token, response code: ${response.code}")
                    callback(null)
                }
            }
        })
    }

    private fun saveAccessToken(token: String, expiryTime: Long) {
        SharedPreferencesManager.saveAccessToken(context, token)
        SharedPreferencesManager.saveTokenExpiry(context, expiryTime)
    }
    private fun saveAccessToken(token: String, expiresIn: Int) {
        SharedPreferencesManager.saveAccessToken(context, token, expiresIn)
    }

    private fun saveRefreshToken(token: String) {
        SharedPreferencesManager.saveRefreshToken(context, token)
    }
}
