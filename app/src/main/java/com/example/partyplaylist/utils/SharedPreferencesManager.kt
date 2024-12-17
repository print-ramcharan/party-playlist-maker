package com.example.partyplaylist.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.Image

object SharedPreferencesManager {

    private const val PREFS_NAME = "spotify_prefs"
    private const val TOKEN_KEY = "access_token"
    private const val RESET_TOKEN_KEY = "refresh_token"

    @SuppressLint("LongLogTag")
    fun getAccessToken(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(TOKEN_KEY, null)
        Log.d("SharedPreferencesManager", "Retrieved access token: $token")
        return token
    }

    fun saveUserProfile(context: Context, user: User) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        with(prefs.edit()) {
            putString("user_id", user.id)
            putString("user_name", user.displayName)
            putString("user_email", user.email)
            putString("user_image", user.images?.firstOrNull()?.url) // Save the first image URL
            apply()
        }
    }
    @JvmStatic
    fun  getUserProfile(context: Context): User? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val id = prefs.getString("user_id", null) ?: return null
        val name = prefs.getString("user_name", null)
        val email = prefs.getString("user_email", null)
        val imageUrl = prefs.getString("user_image", null)
        val followers: com.example.partyplaylist.models.Followers = com.example.partyplaylist.models.Followers(prefs.getInt("followers",0))

        return User(
            id = id,
            displayName = name,
            email = email,
            images = listOfNotNull(imageUrl?.let { Image(url = it) }),
            followers = followers  // Add appropriate logic if needed
        )
    }
    @SuppressLint("LongLogTag")
    fun saveAccessToken(context: Context, token: String, expiresIn: Int? = null) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putString(TOKEN_KEY, token)

        // Save the expiration time if provided
        if (expiresIn != null) {
            val expiryTime = System.currentTimeMillis() + (expiresIn * 1000L)
            editor.putLong("access_token_expiry", expiryTime)
            Log.d("SharedPreferencesManager", "Saved access token with expiry time: $expiryTime")
        } else {
            // If no expiry time is provided, remove any previous expiry time
            editor.remove("access_token_expiry")
            Log.d("SharedPreferencesManager", "Saved access token without expiry time")
        }

        editor.apply()
    }

    @SuppressLint("LongLogTag")
    fun saveRefreshToken(context: Context, token: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(RESET_TOKEN_KEY, token).apply()
        Log.d("SharedPreferencesManager", "Saved refresh token: $token")
    }

    @SuppressLint("LongLogTag")
    fun getRefreshToken(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(RESET_TOKEN_KEY, null)
        Log.d("SharedPreferencesManager", "Retrieved refresh token: $token")
        return token
    }
    fun saveTokenExpiry(context: Context, expiryTime: Long) {
        val sharedPreferences = context.getSharedPreferences("SpotifyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("token_expiry", expiryTime).apply()
    }

    fun getTokenExpiry(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences("SpotifyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("token_expiry", 0)
    }
    @JvmStatic
     fun  getUserId(context: Context): String? {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("user_id", null)
    }

    @JvmStatic
    fun getUserName(context: Context): String? {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("user_name", null) // Return the saved user name
    }

    @JvmStatic
    fun clearUserData(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply() // Clear all stored user data
        Log.d("SharedPreferencesManage", "Cleared user data")
    }

}
