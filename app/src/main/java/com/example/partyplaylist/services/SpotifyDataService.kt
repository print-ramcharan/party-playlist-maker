
package com.example.partyplaylist.services

import AlbumTracks
import android.content.ContentProvider
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.partyplaylist.network.RetrofitClient
import com.example.partyplaylist.network.SpotifyService
import com.example.partyplaylist.models.LikedSongs
import com.example.partyplaylist.data.*
import com.example.partyplaylist.models.AddedBy
import com.example.partyplaylist.models.Album

import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.models.ExternalIds
import com.example.partyplaylist.models.ExternalUrls
import com.example.partyplaylist.models.Image
import com.example.partyplaylist.models.LikedSongsResponse
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.PlaylistRequest
import com.example.partyplaylist.models.PlaylistResponse
import com.example.partyplaylist.models.PlaylistResponse2
import com.example.partyplaylist.models.PlaylistTrack
import com.example.partyplaylist.models.PlaylistTracks
import com.example.partyplaylist.models.PlaylistTrackss
import com.example.partyplaylist.models.TopArtistsResponse
import com.example.partyplaylist.models.TopTracksResponse
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.models.TracksResponse
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.utils.SharedPreferencesManager
import com.example.partyplaylist.utils.SharedPreferencesManager.getUserId
import com.example.partyplaylist.utils.SharedPreferencesManager.getUserProfile
import com.example.partyplaylist.utils.TokenManager
import com.google.android.play.core.integrity.e
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // Adapter to parse the liked songs data from the API response
    private val likedSongsAdapter = moshi.adapter(LikedSongsResponse::class.java)

    private val spotifyService: SpotifyService =
        RetrofitClient.getClient().create(SpotifyService::class.java)

    private lateinit var firebaseRepository: FirebaseRepository

  // Use requireContext() directly to get the user ID

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
//    fun fetchPlaylists(callback: (PlaylistResponse2?) -> Unit) {
//        Log.d("SpotifyDataServicex", "Fetching playlists")
//
//        // Get the access token
//        val accessToken = getAccessToken() ?: return refreshTokenIfNeeded { newToken ->
//            if (newToken != null) {
//                fetchPlaylists(callback) // Retry with new token
//            } else {
//                callback(null) // Return null if token is unavailable
//            }
//        }
//
//        CoroutineScope(Dispatchers.IO).launch {
//            // Build the URL for the playlists endpoint
//            val url = "https://api.spotify.com/v1/me/playlists?limit=50" // Endpoint for fetching user's playlists
//            Log.d("SpotifyDataServicex", "Request URL: $url")
//
//            // Build the request with the access token in the Authorization header
//            val request = Request.Builder()
//                .url(url)
//                .header("Authorization", "Bearer $accessToken")
//                .build()
//
//            // Perform the request synchronously
//            val client = OkHttpClient()
//            try {
//                val response = client.newCall(request).execute()
//
//                if (response.isSuccessful) {
//                    val jsonResponse = response.body?.string()
//                    Log.d("SpotifyDataServicex", "Response for playlists : $jsonResponse")
//
//                    if (jsonResponse != null) {
//                        try {
//                            // Parse the JSON response to a PlaylistResponse object
//                            val playlistResponse = Gson().fromJson(jsonResponse, PlaylistResponse2::class.java)
//
//                            Log.d("SpotifyDataServicex", "Parsed PlaylistResponse: ${Gson().toJson(playlistResponse)}")
//                            // Fetch and add the tracks for each playlist
//                            playlistResponse.items?.forEach { playlist ->
//                                // Ensure the ownerId is correctly set from the response
//                                val originalOwnerId = playlist.owner?.id
//
//                                // Check if the collaborators list is not null and mutable
//                                if (playlist.collaborators == null) {
//                                    playlist.collaborators = mutableListOf() // Initialize as a mutable list if it's null
//                                }
//
//                                // Add the current user as a collaborator if they are not the owner
//                                if (originalOwnerId != getUserId(context) ){
//                                    playlist.collaborators = playlist.collaborators.toMutableList().apply {
//                                        getUserProfile(context)?.let { add(it) }
//                                    }
//                                }
//
//                                // Fetch tracks and add them to the playlist object
//                                fetchPlaylistTracks(playlist.id, accessToken) { playlistTracks ->
//                                    playlist.tracks = playlistTracks // Add tracks to the playlist object
//                                    Log.d("playlisttracks", playlistTracks.toString())
//                                }
//                            }
//
//                            // Save the playlists and tracks to Firebase
////                            savePlaylistsToFirebase(playlistResponse, context)
//                            Log.d("Firebase", "PlaylistResponse: ${Gson().toJson(playlistResponse)}")
//
//
//                            callback(playlistResponse) // Return the parsed PlaylistResponse
//                        } catch (e: Exception) {
//                            Log.e("SpotifyDataServicex", "Error parsing playlists response", e)
//                            callback(null) // Return null on parsing error
//                        }
//                    } else {
//                        callback(null) // Return null if no response body
//                    }
//                } else {
//                    Log.e("SpotifyDataServicex", "Error: ${response.code}")
//                    callback(null) // Return null on error response
//                }
//            } catch (e: IOException) {
//                Log.e("SpotifyDataServicex", "Request failed", e)
//                callback(null) // Return null if request fails
//            }
//        }
//    }
fun fetchPlaylists(callback: (PlaylistResponse2?) -> Unit) {
    Log.d("SpotifyDataService", "Fetching playlists")

    // Get the access token
    val accessToken = getAccessToken() ?: return refreshTokenIfNeeded { newToken ->
        if (newToken != null) {
            fetchPlaylists(callback) // Retry with new token
        } else {
            callback(null) // Return null if token is unavailable
        }
    }

    CoroutineScope(Dispatchers.IO).launch {
        val url = "https://api.spotify.com/v1/me/playlists?limit=50"
        Log.d("SpotifyDataService", "Request URL: $url")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()

        val client = OkHttpClient()
        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonResponse = response.body?.string()
                if (jsonResponse != null) {
                    try {
                        val playlistResponse = Gson().fromJson(jsonResponse, PlaylistResponse2::class.java)
                        Log.d("SpotifyDataService", "Parsed PlaylistResponse: ${Gson().toJson(playlistResponse)}")
                        Log.d("SpotifyDataServicex", "Parsed PlaylistResponse: ${Gson().toJson(playlistResponse)}")
                        // Fetch and add the tracks for each playlist
                        playlistResponse.items?.forEach { playlist ->
                            // Ensure the ownerId is correctly set from the response
                            val originalOwnerId = playlist.owner?.id

                            // Check if the collaborators list is not null and mutable
                            if (playlist.collaborators == null) {
                                playlist.collaborators = mutableListOf() // Initialize as a mutable list if it's null
                            }

                            // Add the current user as a collaborator if they are not the owner
                            if (originalOwnerId != getUserId(context) ){
                                playlist.collaborators = playlist.collaborators.toMutableList().apply {
                                    getUserProfile(context)?.let { add(it) }
                                }
                            }

                            // Fetch tracks and add them to the playlist object
                            fetchPlaylistTracks(playlist.id, accessToken) { playlistTracks ->
                                playlist.tracks = playlistTracks // Add tracks to the playlist object
                                Log.d("playlisttracks", playlistTracks.toString())
                            }
                        }
                        // Filter playlists where the user is the owner or a collaborator
                        Log.d("Firebase", "PlaylistResponse: ${Gson().toJson(playlistResponse)}")

                        callback(playlistResponse)
                    } catch (e: Exception) {
                        Log.e("SpotifyDataService", "Error parsing playlists response", e)
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            } else {
                Log.e("SpotifyDataService", "Error: ${response.code}")
                callback(null)
            }
        } catch (e: IOException) {
            Log.e("SpotifyDataService", "Request failed", e)
            callback(null)
        }
    }
}

    // Function to fetch tracks for a specific playlist
    suspend fun fetchPlaylistTracks(playlistId: String, accessToken: String, callback: (PlaylistTracks) -> Unit) {
        val tracksUrl = "https://api.spotify.com/v1/playlists/$playlistId/tracks"
        Log.d("SpotifyDataServiceURL", "Request URL for tracks: $tracksUrl")

        val request = Request.Builder()
            .url(tracksUrl)
            .header("Authorization", "Bearer $accessToken")
            .build()

        val client = OkHttpClient()
        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonResponse = response.body?.string()
                Log.d("SpotifyDataResponse", "Response for tracks: $jsonResponse")

                if (jsonResponse != null) {
                    try {
                        val tracksResponse = Gson().fromJson(jsonResponse, PlaylistTracks::class.java)

                        // Check if the response has items
                        val playlistTracks = if (tracksResponse.items.isNullOrEmpty()) {
                            Log.d("SpotifyDataEmptyList", "No tracks found in playlist")
                            emptyList() // Return empty list if no tracks are present
                        } else {
                            // Map Track objects to PlaylistTrack with extra fields
                            tracksResponse.items.map { item ->
                                val user =
                                    item.added_by?.id?.let { fetchUserData(it) }  // Assuming externalUrls holds the URL to get user info

                                val track = item.track
                                PlaylistTrack(
                                    track = Track(
                                        album = track.album,
                                        artists = track.artists,
                                        availableMarkets = track.availableMarkets,
                                        discNumber = track.discNumber,
                                        durationMs = track.durationMs,
                                        explicit = track.explicit,
                                        externalIds = track.externalIds,
                                        externalUrls = track.externalUrls,
                                        href = track.href,
                                        id = track.id,
                                        local = track.local,
                                        name = track.name,
                                        popularity = track.popularity,
                                        previewUrl = track.previewUrl,
                                        trackNumber = track.trackNumber,
                                        type = track.type,
                                        uri = track.uri,
                                        albumArtUrl = track.albumArtUrl,
                                        voteCount = 0, // Default value for voteCount
                                        added_by = user ?: track.added_by, // Default value for addedBy

                                    ),
                                    voteCount = 0,  // Default value for voteCount
                                    added_by = item.added_by,  // Default value for addedBy
                                    addedCount = 0,  // Default value for addedCount
                                    lastUpdated = System.currentTimeMillis()  // Default value for lastUpdated
                                )
                            }
                        }

                        val playlistTracksResponse = PlaylistTracks(
                            total = tracksResponse.total ?: 0,
                            items = playlistTracks
                        )

                        Log.d("SpotifyDataTracksFound", "Total tracks fetched: ${playlistTracksResponse.total}" + "  ${playlistTracksResponse.items}")
                        callback(playlistTracksResponse) // Return the PlaylistTracks object
                    } catch (e: Exception) {
                        Log.e("SpotifyDataParseError", "Error parsing tracks response", e)
                        callback(PlaylistTracks()) // Return empty PlaylistTracks on error
                    }
                } else {
                    Log.d("SpotifyDataNoResponse", "No response body received")
                    callback(PlaylistTracks()) // Return empty PlaylistTracks if no response body
                }
            } else {
                Log.e("SpotifyDataError", "Error: ${response.code}")
                callback(PlaylistTracks()) // Return empty PlaylistTracks on error response
            }
        } catch (e: IOException) {
            Log.e("SpotifyDataRequestFail", "Request failed", e)
            callback(PlaylistTracks()) // Return empty PlaylistTracks if request fails
        }
    }




    private val userCache = mutableMapOf<String, User>()

    // Function to fetch user data
    suspend fun fetchUserData(userId: String): User {
        // Check if user data is already cached
        userCache[userId]?.let {
            Log.d("SpotifyService", "Returning cached user data for: $userId")
            return it
        }

        // If not cached, make API request
        val validUrl = "https://api.spotify.com/v1/users/$userId"
        val request = Request.Builder()
            .url(validUrl)
            .header("Authorization", "Bearer ${getAccessToken()}")
            .build()

        val client = OkHttpClient()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    if (jsonResponse != null) {
                        val user = Gson().fromJson(jsonResponse, User::class.java)
                        Log.d("SpotifyService", "Fetched and cached user data for: $userId")

                        // Cache the user data
                        userCache[userId] = user

                        return@withContext user
                    }
                } else {
                    Log.e("SpotifyService", "Failed to fetch user data: ${response.code}")
                }
            } catch (e: IOException) {
                Log.e("SpotifyService", "Request failed: ${e.localizedMessage}", e)
            }

            // Return an empty User object in case of failure
            return@withContext User()
        }
    }
//    fun savePlaylistsToFirebase(playlistResponse: PlaylistResponse2, context: Context) {
//        val database = FirebaseDatabase.getInstance().reference
//
//        // Get the userId (make sure this function fetches the userId correctly)
//        val userId = getUserId(context)
//
//        // Check if userId is not null
//        if (userId != null) {
//            // Define the path in the Firebase database
//            val userPlaylistsRef = database.child("users").child(userId).child("playlists")
//
//            Log.d("Firebase", "PlaylistResponse: ${Gson().toJson(playlistResponse)}")
//
//
//            // Loop through each playlist in the response
//            playlistResponse.items.forEach { playlist ->
//                // Save each playlist under the "playlists" node with its unique playlistId
//                val playlistId = playlist.id
//                userPlaylistsRef.child(playlistId).setValue(playlist)
//                    .addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            Log.d("Firebase", "Playlist saved: ${playlist.name}")
//                        } else {
//                            Log.e("Firebase", "Failed to save playlist: ${playlist.name}")
//                        }
//                    }
//            }
//        } else {
//            Log.e("Firebase", "User ID is null. Cannot save playlists.")
//        }
//    }
//
    private fun savePlaylistsToFirebase(playlistResponse: PlaylistResponse2, context: Context) {
        val database = FirebaseDatabase.getInstance().reference

        // Get the userId (make sure this function fetches the userId correctly)
        val userId = getUserId(context)

        // Check if userId is not null
        if (userId != null) {
            // Define the path in the Firebase database
            val userPlaylistsRef = database.child("users").child(userId).child("playlists")

            Log.d("Firebase", "PlaylistResponse: ${Gson().toJson(playlistResponse)}")

            // Loop through each playlist in the response
            playlistResponse.items.forEach { playlist ->
                val playlistId = playlist.id
                // Check if the playlist already exists in Firebase
                userPlaylistsRef.child(playlistId).get().addOnSuccessListener { dataSnapshot ->
                    // If the playlist exists
                    if (dataSnapshot.exists()) {
                        // Fetch existing tracks for this playlist
                        val existingTracks = dataSnapshot.child("tracks").child("items").children.mapNotNull {
                            it.child("track").getValue(Track::class.java)
                        }.toMutableList()

                        // Add new tracks from the playlist
                        val newTracks = playlist.tracks.items?.map { it.track } ?: emptyList()

                        // Combine the existing tracks with the new ones (avoiding duplicates)
                        val updatedTracks = existingTracks.union(newTracks).toList()

                        // Update the tracks list for the existing playlist
                        userPlaylistsRef.child(playlistId).child("tracks").child("items").setValue(updatedTracks)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("Firebase", "Updated tracks for playlist: ${playlist.name}")
                                } else {
                                    Log.e("Firebase", "Failed to update tracks for playlist: ${playlist.name}")
                                }
                            }
                    } else {
                        // If the playlist doesn't exist, save it as a new playlist
                        userPlaylistsRef.child(playlistId).setValue(playlist)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("Firebase", "Playlist saved: ${playlist.name}")
                                } else {
                                    Log.e("Firebase", "Failed to save playlist: ${playlist.name}")
                                }
                            }
                    }
                }.addOnFailureListener { exception ->
                    Log.e("Firebase", "Error fetching playlist: ", exception)
                }
            }
        } else {
            Log.e("Firebase", "User ID is null. Cannot save playlists.")
        }
    }

//    private fun savePlaylistsToFirebase(playlistResponse: PlaylistResponse2, context: Context) {
//        val database = FirebaseDatabase.getInstance().reference
//
//        // Get the userId (make sure this function fetches the userId correctly)
//        val userId = getUserId(context)
//
//        // Check if userId is not null
//        if (userId != null) {
//            // Define the path in the Firebase database
//            val userPlaylistsRef = database.child("users").child(userId).child("playlists")
//
//            Log.d("Firebase", "PlaylistResponse: ${Gson().toJson(playlistResponse)}")
//
//            // Loop through each playlist in the response
//            playlistResponse.items.forEach { playlist ->
//                val playlistId = playlist.id
//                // Check if the playlist already exists in Firebase
//                userPlaylistsRef.child(playlistId).get().addOnSuccessListener { dataSnapshot ->
//                    // If the playlist exists
//                    if (dataSnapshot.exists()) {
//                        // Fetch existing PlaylistTracks for this playlist
//                        val existingPlaylistTracks = dataSnapshot.child("tracks").child("items").children.mapNotNull {
//                            it.getValue(PlaylistTrack::class.java)
//                        }.toMutableList()
//
//                        // Add new PlaylistTracks from the playlist
//                        val newPlaylistTracks = playlist.tracks.items?.map { it } ?: emptyList() // Include full PlaylistTrack objects
//
//                        // Combine the existing tracks with the new ones (avoiding duplicates)
//                        val updatedPlaylistTracks = existingPlaylistTracks.union(newPlaylistTracks).toList()
//
//                        // Update the tracks list for the existing playlist
//                        userPlaylistsRef.child(playlistId).child("tracks").child("items").setValue(updatedPlaylistTracks)
//                            .addOnCompleteListener { task ->
//                                if (task.isSuccessful) {
//                                    Log.d("Firebase", "Updated tracks for playlist: ${playlist.name}")
//                                } else {
//                                    Log.e("Firebase", "Failed to update tracks for playlist: ${playlist.name}")
//                                }
//                            }
//                    } else {
//                        // If the playlist doesn't exist, save it as a new playlist
//                        userPlaylistsRef.child(playlistId).setValue(playlist)
//                            .addOnCompleteListener { task ->
//                                if (task.isSuccessful) {
//                                    Log.d("Firebase", "Playlist saved: ${playlist.name}")
//                                } else {
//                                    Log.e("Firebase", "Failed to save playlist: ${playlist.name}")
//                                }
//                            }
//                    }
//                }.addOnFailureListener { exception ->
//                    Log.e("Firebase", "Error fetching playlist: ", exception)
//                }
//            }
//        } else {
//            Log.e("Firebase", "User ID is null. Cannot save playlists.")
//        }
//    }

    // Function to save playlists to Firebase
// Function to save playlists to Firebase
//    fun savePlaylistsToFirebase(playlistResponse: PlaylistResponse2, context: Context) {
//        val database = FirebaseDatabase.getInstance().reference
//
//        // Get the userId (make sure this function fetches the userId correctly)
//        val userId = getUserId(context)
//
//        // Check if userId is not null
//        if (userId != null) {
//            // Define the path in the Firebase database
//            val userPlaylistsRef = database.child("users").child(userId).child("playlists")
//
//            // Loop through each playlist in the response
//            playlistResponse.items.forEach { playlist ->
//                val playlistId = playlist.id
//
//                // Check if playlist already exists in Firebase
//                userPlaylistsRef.child(playlistId).get().addOnSuccessListener { snapshot ->
//                    if (snapshot.exists()) {
//                        // Playlist exists, update only the metadata fields
//                        userPlaylistsRef.child(playlistId).updateChildren(
//                            mapOf(
//                                "collaborative" to playlist.collaborative,
//                                "description" to playlist.description,
//                                "externalUrls" to playlist.externalUrls,
//                                "href" to playlist.href,
//                                "id" to playlist.id,
//                                "images" to playlist.images,
//                                "name" to playlist.name,
//                                "owner" to playlist.owner,
//                                "public" to playlist.public,
//                                "snapshotId" to playlist.snapshotId,
//                                "type" to playlist.type,
//                                "uri" to playlist.uri,
//                                "collaborators" to playlist.collaborators,
//                                "totalVotes" to playlist.totalVotes,
//                                "tracks" to playlist.tracks,
//                                "lastModified" to playlist.lastModified
//                            )
//                        ).addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                Log.d("Firebase", "Playlist updated: ${playlist.name}")
//                            } else {
//                                Log.e("Firebase", "Failed to update playlist: ${playlist.name}")
//                            }
//                        }
//                    } else {
//                        // Playlist doesn't exist, add it as a new playlist
//                        userPlaylistsRef.child(playlistId).setValue(playlist)
//                            .addOnCompleteListener { task ->
//                                if (task.isSuccessful) {
//                                    Log.d("Firebase", "Playlist added: ${playlist.name}")
//                                } else {
//                                    Log.e("Firebase", "Failed to add playlist: ${playlist.name}")
//                                }
//                            }
//                    }
//                }
//            }
//        } else {
//            Log.e("Firebase", "User ID is null. Cannot save playlists.")
//        }
//    }

    fun fetchLikedSongs(callback: (LikedSongs?) -> Unit) {
        Log.d("SpotifyDataService", "Fetching liked songs")

        val accessToken = getAccessToken() ?: return refreshTokenIfNeeded { newToken ->
            if (newToken != null) {
                fetchLikedSongs(callback) // Retry with new token
            } else {
                callback(null) // Return null if token is still unavailable
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://api.spotify.com/v1/me/tracks?limit=50"
            Log.d("SpotifyDataService", "Request URL: $url")

            val request = Request.Builder()
                .url(url)
                .header(
                    "Authorization",
                    "Bearer $accessToken"
                ) // Authorization header with Bearer token
                .build()

            try {
                val response = client.newCall(request).execute() // Synchronous request

                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    Log.d("SpotifyDataService", "Response: $jsonResponse")

                    if (jsonResponse != null) {
                        try {
                            val jsonObject = JSONObject(jsonResponse)
                            val itemsArray = jsonObject.getJSONArray("items")
                            val songNames = mutableListOf<String>()

                            for (i in 0 until itemsArray.length()) {
                                val item = itemsArray.getJSONObject(i)
                                val track = item.getJSONObject("track")
                                val trackName = track.getString("name")
                                songNames.add(trackName) // Add song name to the list
                            }

                            // Convert to LikedSongs object and pass to callback
                            val likedSongs = LikedSongs(trackIds = songNames)
                            callback(likedSongs) // Pass the LikedSongs object to the callback
                        } catch (e: Exception) {
                            Log.e("SpotifyDataService", "Error parsing liked songs response", e)
                            callback(null) // Return null on parsing error
                        }
                    } else {
                        callback(null) // Return null if no response body
                    }
                } else {
                    Log.e("SpotifyDataService", "Error: ${response.code}")
                    callback(null) // Return null on error response
                }
            } catch (e: IOException) {
                Log.e("SpotifyDataService", "Error during request", e)
                callback(null) // Return null if request fails
            }
        }
    }

    fun fetchArtistTopTracks(artistId: String, callback: (List<Track>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var attempt = 0
            val maxAttempts = 5
            var delayTime = 1000L // Start with a 1-second delay

            while (attempt < maxAttempts) {
                try {
                    delay(delayTime) // Introduce delay before making the request
                    val url = "https://api.spotify.com/v1/artists/$artistId/top-tracks?market=US&access_token=${getAccessToken()}"
                    Log.d("SpotifyDataService", "Request URL: $url")
                    val request = Request.Builder().url(url).build()

                    val response = OkHttpClient().newCall(request).execute()
                    if (response.isSuccessful) {
                        val jsonResponse = response.body?.string()
                        Log.d("SpotifyDataServicec", "Response: $jsonResponse")

                        if (jsonResponse != null) {
                            try {
                                val jsonObject = JSONObject(jsonResponse)
                                val tracksArray = jsonObject.getJSONArray("tracks")
                                val tracks = mutableListOf<Track>()

                                for (i in 0 until tracksArray.length()) {
                                    val trackJson = tracksArray.getJSONObject(i)
                                    Log.d("track data",trackJson.toString())
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
                                return@launch // Exit after a successful fetch
                            } catch (e: Exception) {
                                Log.e("SpotifyDataService", "Parsing error", e)
                                callback(null)
                                return@launch
                            }
                        } else {
                            Log.e("SpotifyDataService", "Empty response body")
                            callback(null)
                            return@launch
                        }
                    } else {
                        Log.e("SpotifyDataService", "Error: ${response.code} - ${response.message}")
                        if (response.code == 429) {
                            // Use the Retry-After header if available
                            val retryAfter = response.header("Retry-After")?.toLongOrNull()?.let { it * 1000 } ?: delayTime
                            Log.e("SpotifyDataService", "Rate limit exceeded. Retrying after ${retryAfter / 1000} seconds.")
                            delay(retryAfter) // Wait based on the Retry-After header
                            attempt++
                            delayTime = (delayTime * 2).coerceAtMost(30000) // Exponential backoff, max 30 seconds
                        } else {
                            callback(null)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SpotifyDataService", "Request error", e)
                    callback(null)
                    return@launch
                }
            }
            Log.e("SpotifyDataService", "Max retry attempts reached")
            callback(null)
        }
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
            uri = albumJson.getString("uri"),
//            tracks = albumJson.getString("tracks")
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

                                callback(detailedAlbums)

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

        // Create a recursive function to fetch each album with a delay
        fun fetchAlbumWithDelay(albumIdIndex: Int) {
            if (albumIdIndex >= albumIds.size) {
                // If we've processed all albums, call the callback
                callback(albums)
                return
            }

            val albumId = albumIds[albumIdIndex]
            val url = "https://api.spotify.com/v1/albums/$albumId?access_token=${getAccessToken()}"
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e("SpotifyDataService", "Request failed for album ID $albumId", e)
                    checkIfAllRequestsCompleted(albumIdIndex)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (response.isSuccessful) {
                        val jsonResponse = response.body?.string()
                        Log.d("SpotifyDataService", "Response for album ID $albumId: $jsonResponse")

                        if (jsonResponse != null) {
                            try {
                                val albumResponse = Gson().fromJson(jsonResponse, Album::class.java)
                                albums.add(albumResponse)
                                Log.d("albumrequest4", "Album converted using Gson() $albumResponse")
                            } catch (e: Exception) {
                                Log.e("SpotifyDataService", "Parsing error for album ID $albumId", e)
                            }
                        } else {
                            Log.e("SpotifyDataService", "Empty response body for album ID $albumId")
                        }
                    } else if (response.code == 429) {
                        // Handle rate limiting (429 error) by waiting and retrying
                        val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: 1
                        Log.e("SpotifyDataService", "Rate limited. Retrying after $retryAfter seconds.")
                        Thread.sleep(retryAfter * 1000)
                        fetchAlbumWithDelay(albumIdIndex) // Retry the same request
                        return // Avoid calling checkIfAllRequestsCompleted to retry
                    } else {
                        Log.e("SpotifyDataService", "Error for album ID $albumId: ${response.code} - ${response.message}")
                    }

                    checkIfAllRequestsCompleted(albumIdIndex)
                }

                private fun checkIfAllRequestsCompleted(albumIdIndex: Int) {
                    completedRequests++
                    if (completedRequests == requests) {
                        callback(albums)
                    } else {
                        // Delay the next request to avoid hitting the rate limit
                        Thread.sleep(500) // 500 ms delay between requests
                        fetchAlbumWithDelay(albumIdIndex + 1) // Fetch the next album
                    }
                }
            })
        }

        // Start fetching albums with delay
        fetchAlbumWithDelay(0)
    }
}
