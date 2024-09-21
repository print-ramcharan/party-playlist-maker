package com.example.partyplaylist.repositories

import android.content.Context
import com.example.partyplaylist.models.TopTracksResponse
import com.example.partyplaylist.models.TopArtistsResponse
import com.example.partyplaylist.utils.SharedPreferencesManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SpotifyRepository(private val context: Context) {

    private val client = OkHttpClient()
    private val gson = Gson()

    // Fetch top tracks
    fun getTopTracks(callback: (TopTracksResponse?) -> Unit) {
        val accessToken = SharedPreferencesManager.getAccessToken(context) ?: return
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/top/tracks")
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null)
                    return
                }

                val responseData = response.body?.string() ?: return
                val topTracksResponse = gson.fromJson(responseData, TopTracksResponse::class.java)
                callback(topTracksResponse)
            }
        })
    }

    // Fetch top artists
    fun getTopArtists(callback: (TopArtistsResponse?) -> Unit) {
        val accessToken = SharedPreferencesManager.getAccessToken(context) ?: return
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/top/artists")
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null)
                    return
                }

                val responseData = response.body?.string() ?: return
                val topArtistsResponse = gson.fromJson(responseData, TopArtistsResponse::class.java)
                callback(topArtistsResponse)
            }
        })
    }
}
