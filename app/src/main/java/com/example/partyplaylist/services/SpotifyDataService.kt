package com.example.partyplaylist.services

import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.partyplaylist.network.RetrofitClient
import com.example.partyplaylist.network.SpotifyService
import com.example.partyplaylist.models.LikedSongs
import com.example.partyplaylist.data.*
import com.example.partyplaylist.models.Album

import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.models.ExternalIds
import com.example.partyplaylist.models.ExternalUrls
import com.example.partyplaylist.models.Image
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.PlaylistRequest
import com.example.partyplaylist.models.PlaylistResponse
import com.example.partyplaylist.models.TopArtistsResponse
import com.example.partyplaylist.models.TopTracksResponse
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.utils.SharedPreferencesManager
import com.example.partyplaylist.utils.TokenManager
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class SpotifyDataService(private val context: Context) {

    private val spotifyService: SpotifyService =
        RetrofitClient.getClient().create(SpotifyService::class.java)

    private lateinit var firebaseRepository: FirebaseRepository

    private fun getAccessToken(): String? {
        val token = SharedPreferencesManager.getAccessToken(context)
        Log.d("SpotifyDataService", "Access token retrieved: $token")
        return token
    }

    private fun refreshTokenIfNeeded(callback: (String?) -> Unit) {
        Log.d("SpotifyDataService", "Checking if refresh token is needed")

        val refreshToken = SharedPreferencesManager.getRefreshToken(context)

        if (refreshToken != null) {
            Log.d("SpotifyDataService", "Refreshing token using refresh token: $refreshToken")

            val tokenManager = TokenManager(context)
            tokenManager.refreshToken(refreshToken) { newAccessToken ->
                if (newAccessToken != null) {
                    Log.d("SpotifyDataService", "New access token received: $newAccessToken")
                    SharedPreferencesManager.saveAccessToken(context, newAccessToken)
                    callback(newAccessToken)
                } else {
                    Log.e("SpotifyDataService", "Failed to refresh token")
                    callback(null)
                }
            }
        } else {
            Log.e("SpotifyDataService", "No refresh token available")
            callback(null)
        }
    }

    private fun handleApiError(response: Response<*>, message: String) {
        Log.e("SpotifyDataService", "$message: ${response.errorBody()?.string()}")
    }

    private fun <T> makeCall(call: Call<T>, callback: (T?) -> Unit) {
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    Log.d("SpotifyDataService", "API call successful: ${response.body()}")
                    callback(response.body())
                } else {
                    Log.e("SpotifyDataService", "API call error: ${response.errorBody()?.string()}")
                    callback(null)
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                Log.e("SpotifyDataService", "API call failed", t)
                callback(null)
            }
        })
    }

    fun fetchUserProfile(callback: (User?) -> Unit) {

        Log.d("SpotifyDataService", "Fetching user profile")
        val accessToken = getAccessToken() ?: return refreshTokenIfNeeded { newToken ->
            if (newToken != null) {
                fetchUserProfile(callback)
            } else {
                callback(null)
            }
        }
        val call: Call<User> = spotifyService.getUserProfile("Bearer $accessToken")
        makeCall(call, callback)
    }


    fun fetchTopArtists(callback: (TopArtistsResponse?) -> Unit) {
        Log.d("SpotifyDataService", "Fetching top artists")
        val accessToken = getAccessToken() ?: return refreshTokenIfNeeded { newToken ->
            if (newToken != null) {
                fetchTopArtists(callback)
            } else {
                callback(null)
            }
        }
        val call: Call<TopArtistsResponse> = spotifyService.getTopArtists("Bearer $accessToken")
        makeCall(call, callback)
    }

    fun fetchTopTracks(callback: (TopTracksResponse?) -> Unit) {
        Log.d("SpotifyDataService", "Fetching top tracks")
        val accessToken = getAccessToken() ?: return refreshTokenIfNeeded { newToken ->
            if (newToken != null) {
                fetchTopTracks(callback)
            } else {
                callback(null)
            }
        }
        val call: Call<TopTracksResponse> = spotifyService.getTopTracks("Bearer $accessToken")
        makeCall(call, callback)
    }

    fun fetchPlaylists(callback: (PlaylistResponse?) -> Unit) {
        Log.d("SpotifyDataService", "Fetching playlists")
        val accessToken = getAccessToken() ?: return refreshTokenIfNeeded { newToken ->
            if (newToken != null) {
                fetchPlaylists(callback)
            } else {
                callback(null)
            }
        }
        val call: Call<PlaylistResponse> = spotifyService.getPlaylists("Bearer $accessToken")
        makeCall(call, callback)
    }

    fun fetchLikedSongs(callback: (LikedSongs?) -> Unit) {
        Log.d("SpotifyDataService", "Fetching liked songs")
        val accessToken = getAccessToken() ?: return refreshTokenIfNeeded { newToken ->
            if (newToken != null) {
                fetchLikedSongs(callback)
            } else {
                callback(null)
            }
        }
        val call: Call<LikedSongs> = spotifyService.getLikedSongs("Bearer $accessToken")
        makeCall(call, callback)
    }


    fun fetchArtistTopTracks(artistId: String, callback: (List<Track>?) -> Unit) {
        val url =
            "https://api.spotify.com/v1/artists/$artistId/top-tracks?market=US&access_token=${getAccessToken()}"
        Log.d("SpotifyDataService", "Request URL: $url")
        val request = Request.Builder()
            .url(url)
            .build()

        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("SpotifyDataService", "Request failed", e)
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    Log.d("SpotifyDataService", "Response: $jsonResponse")
                    if (jsonResponse != null) {
                        try {
                            // Manual JSON parsing
                            val jsonObject = JSONObject(jsonResponse)
                            val tracksArray = jsonObject.getJSONArray("tracks")
                            val tracks = mutableListOf<Track>()

                            for (i in 0 until tracksArray.length()) {
                                val trackJson = tracksArray.getJSONObject(i)
                                val track = Track(
                                    album = parseAlbum(trackJson.getJSONObject("album")),
                                    artists = parseArtists(trackJson.getJSONArray("artists")),
                                    availableMarkets = emptyList(),
                                    discNumber = trackJson.getInt("disc_number"),
                                    durationMs = trackJson.getInt("duration_ms"),
                                    explicit = trackJson.getBoolean("explicit"),
                                    externalIds = parseExternalIds(trackJson.getJSONObject("external_ids")),
                                    externalUrls = parseExternalUrls(trackJson.getJSONObject("external_urls")),
                                    href = trackJson.getString("href"),
                                    id = trackJson.getString("id"),
                                    local = trackJson.getBoolean("is_local"),
                                    name = trackJson.getString("name"),
                                    popularity = trackJson.getInt("popularity"),
                                    previewUrl = trackJson.optString("preview_url"),
                                    trackNumber = trackJson.getInt("track_number"),
                                    type = trackJson.getString("type"),
                                    uri = trackJson.getString("uri")
                                )
                                tracks.add(track)
                            }
                            Log.d("SpotifyDataService", "Parsed ${tracks.size} tracks")
                            callback(tracks)
                        } catch (e: Exception) {
                            Log.e("SpotifyDataService", "Parsing error", e)
                            callback(null)
                        }
                    } else {
                        Log.e("SpotifyDataService", "Empty response body")
                        callback(null)
                    }
                } else {
                    Log.e("SpotifyDataService", "Error: ${response.code} - ${response.message}")
                    callback(null)
                }
            }
        })
    }

    private fun parseAlbum(albumJson: JSONObject): Album {
        return Album(
            albumType = albumJson.getString("album_type"), // Changed to match the data class
            artists = parseArtists(albumJson.getJSONArray("artists")),
            availableMarkets = emptyList(),
            externalUrls = parseExternalUrls(albumJson.getJSONObject("external_urls")), // Changed to match the data class
            href = albumJson.getString("href"),
            id = albumJson.getString("id"),
            images = parseImageList(albumJson.getJSONArray("images")),
            name = albumJson.getString("name"),
            releaseDate = albumJson.getString("release_date"), // Changed to match the data class
            releaseDatePrecision = albumJson.getString("release_date_precision"), // Changed to match the data class
            totalTracks = albumJson.getInt("total_tracks"), // Changed to match the data class
            type = albumJson.getString("type"),
            uri = albumJson.getString("uri")
        )
    }

    private fun parseArtists(artistsJson: JSONArray): List<Artist> {
        val artists = mutableListOf<Artist>()
        for (i in 0 until artistsJson.length()) {
            val artistJson = artistsJson.getJSONObject(i)
            val artist = Artist(
                id = artistJson.optString("id", ""),
                name = artistJson.optString("name", ""),
                genres = parseStringList(artistJson.optJSONArray("genres") ?: JSONArray()),
                images = parseImageList(artistJson.optJSONArray("images") ?: JSONArray()),
                popularity = artistJson.optInt("popularity", 0),
                externalUrls = parseExternalUrls(
                    artistJson.optJSONObject("external_urls") ?: JSONObject()
                ),
                followers = Followers(
                    artistJson.optJSONObject("followers")?.optInt("total", 0) ?: 0
                ),
                type = artistJson.optString("type", ""),
                Songs = ""
            )
            artists.add(artist)
        }
        return artists
    }

    private fun parseSong(songJson: JSONObject): Song {
        return Song(
            id = songJson.getString("id"),
            name = songJson.getString("name"),
            durationMs = songJson.getInt("duration_ms"),

            artists = parseArtists(songJson.getJSONArray("artists")),
            album = parseAlbum(songJson.getJSONObject("album")),
            popularity = songJson.getInt("popularity")
        )

    }

    private fun parseSongs(songsJson: JSONArray): List<Song> {
        val songs = mutableListOf<Song>()
        for (i in 0 until songsJson.length()) {
            songs.add(parseSong(songsJson.getJSONObject(i)))
        }
        return songs
    }

    private fun parseFollowers(followersJson: JSONObject): Followers {
        return Followers(
            total = followersJson.optInt("total", 0)
        )
    }

    private fun parseStringList(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    private fun parseExternalIds(externalIdsJson: JSONObject): ExternalIds {
        return ExternalIds(
            isrc = externalIdsJson.optString("isrc", ""),
            ean = externalIdsJson.optString("ean", ""),
            upc = externalIdsJson.optString("upc", "")
        )
    }

    private fun parseExternalUrls(externalUrlsJson: JSONObject): ExternalUrls {
        return ExternalUrls(
            spotify = externalUrlsJson.getString("spotify")
        )
    }

    private fun parseImageList(imagesJson: JSONArray): List<Image> {
        val images = mutableListOf<Image>()
        for (i in 0 until imagesJson.length()) {
            val imageJson = imagesJson.getJSONObject(i)
            val image = Image(
                height = imageJson.getInt("height"),
                url = imageJson.getString("url"),
                width = imageJson.getInt("width")
            )
            images.add(image)
        }
        return images
    }


    fun fetchUserSavedAlbums(callback: (List<Album>?) -> Unit) {
        val url = "https://api.spotify.com/v1/me/albums?access_token=${getAccessToken()}"
        Log.d("SpotifyDataService", "Request URL: $url")
        val request = Request.Builder()
            .url(url)
            .build()

        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("SpotifyDataService", "Request failed", e)
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    Log.d("SpotifyDataService", "Response: $jsonResponse")

                    if (jsonResponse != null) {
                        try {
                            val jsonObject = JSONObject(jsonResponse)
                            val itemsArray = jsonObject.getJSONArray("items")
                            val albumIds = mutableListOf<String>()

                            // Extract album IDs
                            for (i in 0 until itemsArray.length()) {
                                val albumJson = itemsArray.getJSONObject(i).getJSONObject("album")
                                Log.d("beforeadding", albumJson.toString())
                                val albumId = albumJson.getString("id")
                                albumIds.add(albumId)
                            }

                            // Fetch details for each album
                            fetchAlbumDetails(albumIds) { detailedAlbums ->
//                                if (detailedAlbums != null) {
//                                    // Combine the responses: update the initial list with detailed information
//                                    val updatedAlbums = albumIds.mapNotNull { albumId ->
//                                        detailedAlbums.find { it.id == albumId }?.let { detailedAlbum ->
//                                            // Assuming that the original list of albums only has basic info
//                                            // and the detailedAlbums list contains the detailed info
//                                            detailedAlbum.copy(
//                                                // Add or update specific properties if necessary
//                                            )
//                                        }
//                                    }
//                                    Log.d("SpotifyDataServicev", "Response: $updatedAlbums")

                                callback(detailedAlbums)
//                                } else {
//                                    callback(null) // Handle error scenario
//                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SpotifyDataService", "Parsing error", e)
                            callback(null)
                        }
                    } else {
                        Log.e("SpotifyDataService", "Empty response body")
                        callback(null)
                    }
                } else {
                    Log.e("SpotifyDataService", "Error: ${response.code} - ${response.message}")
                    callback(null)
                }
            }
        })
    }

    fun fetchAlbumDetails(albumIds: List<String>, callback: (List<Album>?) -> Unit) {
        val albums = mutableListOf<Album>()
        val client = OkHttpClient()
        val requests = albumIds.size
        var completedRequests = 0

        // Create a request for each album ID
        for (albumId in albumIds) {
            val url = "https://api.spotify.com/v1/albums/$albumId?access_token=${getAccessToken()}"
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e("SpotifyDataService", "Request failed for album ID $albumId", e)
                    // Handle failure if needed
                    checkIfAllRequestsCompleted()
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (response.isSuccessful) {
                        val jsonResponse = response.body?.string()
                        Log.d("SpotifyDataService", "Response for album ID $albumId: $jsonResponse")

                        if (jsonResponse != null) {
                            try {
                                val albumResponse = Gson().fromJson(jsonResponse, Album::class.java)

                                albums.add(albumResponse)
                                Log.d("albumrequest", jsonResponse)
//                                Log.d("albumrequest2","      json response"+ jsonObject)
//                                Log.d("albumrequest3","album converted data $album")
                                Log.d(
                                    "albumrequest4",
                                    "album converted data using Gson() $albumResponse"
                                )
                            } catch (e: Exception) {
                                Log.e(
                                    "SpotifyDataService",
                                    "Parsing error for album ID $albumId",
                                    e
                                )
                                // Handle parsing error if needed
                            }
                        } else {
                            Log.e("SpotifyDataService", "Empty response body for album ID $albumId")
                            // Handle empty response if needed
                        }
                    } else {
                        Log.e(
                            "SpotifyDataService",
                            "Error for album ID $albumId: ${response.code} - ${response.message}"
                        )
                        // Handle error response if needed
                    }
                    checkIfAllRequestsCompleted()
                }

                private fun checkIfAllRequestsCompleted() {
                    completedRequests++
                    if (completedRequests == requests) {
                        callback(albums)
                    }
                }
            })
        }
    }
}