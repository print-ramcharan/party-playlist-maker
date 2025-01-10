package com.example.partyplaylist

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.partyplaylist.LibraryFragment.FragmentTransactionListener
import com.example.partyplaylist.adapters.PlaylistTracksAdapter
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.PlaylistTrack
import com.example.partyplaylist.models.PlaylistTracks
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.utils.SharedPreferencesManager
import com.example.partyplaylist.utils.SharedPreferencesManager.getUserId
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class PlaylistDetailFragment : Fragment() {

    private var playlistId: String? = null
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var playlistTracksAdapter: PlaylistTracksAdapter
    private val tracks: MutableList<PlaylistTrack> = mutableListOf()
    private var playlistName: String? = null
    private var fragmentTransactionListener: FragmentTransactionListener? = null
    private var owner : String? = null
    private var collaborator : String? = null
    private var userid :String? = null
    interface FragmentTransactionListener {
        fun loadSearchFragment(fragment: Fragment?, tag: String?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = arguments?.getString("playlistId")
        playlistName = arguments?.getString("playlistName")
        owner = arguments?.getString("owner")
        collaborator = arguments?.getString("collaborators");
        firebaseRepository = FirebaseRepository(requireContext())
        Log.d("PlaylistDetailFragment", "onCreate: playlistId = $playlistId")
        userid = getUserId(requireContext())
    }

    fun getPlaylistId(): String? {
      return playlistId
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view: View = inflater.inflate(R.layout.playlist_screen, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.playlist_recycler_view)
        val playlisttitle = view.findViewById<TextView>(R.id.playlist_title)
        playlisttitle.text = playlistName

        playlistTracksAdapter = PlaylistTracksAdapter(tracks) { track ->
            onVoteClicked(track)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = playlistTracksAdapter

        val addButton = view.findViewById<FloatingActionButton>(R.id.add_button)
        addButton.setOnClickListener {
            val fragmentManager = parentFragmentManager
            val existingFragment = fragmentManager.findFragmentByTag("SearchFragment")
            if (existingFragment == null) {
                val searchFragment = SearchFragmentToAdd(this)
                fragmentTransactionListener?.loadSearchFragment(searchFragment, "SearchFragment")
            } else {
                Log.d("pdf", "SearchFragment is already loaded")
            }
        }

        if (playlistId != null) {
            Log.d("PlaylistDetailFragment", "onCreateView: Fetching playlist details for $playlistId")
            fetchPlaylistDetails(playlistId!!)
        } else {
            Log.e("PlaylistDetailFragment", "onCreateView: playlistId is null!")
        }

        return view
    }

    private fun fetchPlaylistDetails(playlistId: String) {
        Log.d("PlaylistDetailFragment", "fetchPlaylistDetails: Fetching playlist with ID = $playlistId")
        if(owner == userid){
            firebaseRepository.getPlaylistTracks(playlistId) { tracksFromFirebase ->
                if (tracksFromFirebase != null && tracksFromFirebase.tracks.items.isNotEmpty()) {
                    Log.d("PlaylistDetailFragment", "Tracks found in Firebase" + tracksFromFirebase)
                    updateUI(tracksFromFirebase)
                } else {
                    val accessToken = SharedPreferencesManager.getAccessToken(requireContext())
                    if (accessToken != null) {
                        fetchPlaylistFromSpotify(playlistId, accessToken) { tracks ->
                            if (tracks != null) {
                                Log.d("PlaylistDetailFragment", "Tracks fetched from Spotify")
                                updateUI(tracks)
                                updatePlaylistTracksInFirebase(playlistId, tracks)
                            } else {
                                Log.e("PlaylistDetailFragment", "Failed to fetch tracks from Spotify")
                            }
                        }
                    }
                }
            }
        }else{
            owner?.let {
                firebaseRepository.getPlaylistTracksOwner(playlistId, it) { collaboratorPlaylist ->
                    Log.d("response is", collaboratorPlaylist.toString())
                    if (collaboratorPlaylist != null && collaboratorPlaylist.tracks.items.isNotEmpty()) {
                        Log.d("PlaylistDetailFragment", "Tracks found in Firebase" + collaboratorPlaylist)
                        updateUI(collaboratorPlaylist)
                    }
                }
            }
        }

//            else {
//            Log.d("collaboratornotconverte", "collaborator not converted")
//        }


    }

//    private fun fetchPlaylistFromSpotify(playlistId: String, accessToken: String, callback: (List<PlaylistTrack>?) -> Unit) {
//        Log.d("fetchPlaylist", "Starting fetchPlaylistFromSpotify with playlistId: $playlistId")
//
//        CoroutineScope(Dispatchers.IO).launch {
//            val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks"
//            Log.d("fetchPlaylist", "Request URL: $url")
//
//            val client = OkHttpClient()
//            val request = Request.Builder()
//                .url(url)
//                .addHeader("Authorization", "Bearer $accessToken")
//                .build()
//
//            Log.d("fetchPlaylist", "Request built successfully, executing request...")
//
//            client.newCall(request).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    Log.e("fetchPlaylist", "Request failed with exception: ${e.message}")
//                    e.printStackTrace()
//                    callback(null)
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    Log.d("fetchPlaylist", "Response received, status code: ${response.code}")
//
//                    if (response.isSuccessful) {
//                        val responseBody = response.body?.string()
//                        Log.d("Spotify Response", "Response Body: $responseBody")
//
//                        if (responseBody != null) {
//                            // Log before deserialization
//                            Log.d("fetchPlaylist", "Deserializing the response into PlaylistResponse")
//
//                            val playlistResponse = Gson().fromJson(responseBody, PlaylistResponse::class.java)
//                            Log.d("fetchPlaylist", "Deserialization complete ${playlistResponse}")
//
//                            val tracks = playlistResponse.tracks.items.mapNotNull { item ->
//                                Log.d("fetchPlaylist", "Processing track item: ${item.track?.name}")
//
//                                // Check if the track is not null
//                                item.track?.let { track ->
//                                    // If the track has an added_by field, fetch the user data
//                                    val addedBy = item.track.added_by.externalUrls.spotify.let { userUrl ->
//                                        Log.d("url is", "User URL for addedBy: $userUrl")
//                                        // Call fetchUserData and log before fetching
//                                        Log.d("fetchPlaylist", "Fetching user data for URL: $userUrl")
//                                        fetchUserData(userUrl, accessToken)
//                                    }
//
//                                    PlaylistTrack(
//                                        track = track,
//                                        voteCount = 0,
//                                        addedBy = addedBy,  // Set user data here
//                                        addedCount = 0,
//                                        lastUpdated = System.currentTimeMillis()
//                                    ).also {
//                                        Log.d("fetchPlaylist", "Mapped track: ${it.track.name}")
//                                    }
//                                }
//                            }
//
//                            // Log before returning data
//                            Log.d("fetchPlaylist", "Finished processing tracks. Passing to callback...")
//
//                            requireActivity().runOnUiThread {
//                                callback(tracks)
//                            }
//                        }
//                    } else {
//                        Log.e("fetchPlaylist", "Request failed, response not successful. Status code: ${response.code}")
//                        callback(null)
//                    }
//                }
//            })
//        }
//    }
//fun fetchPlaylistFromSpotify(playlistId: String, accessToken: String, callback: (List<PlaylistTrack>?) -> Unit) {
//    val tracksUrl = "https://api.spotify.com/v1/playlists/$playlistId/tracks"
//    Log.d("SpotifyDataServiceURL", "Request URL for tracks: $tracksUrl")
//
//    val request = Request.Builder()
//        .url(tracksUrl)
//        .header("Authorization", "Bearer $accessToken")
//        .build()
//
//    val client = OkHttpClient()
//
//    Thread {
//        try {
//            val response = client.newCall(request).execute()
//
//            if (response.isSuccessful) {
//                val jsonResponse = response.body?.string()
//                Log.d("SpotifyDataResponse", "Response for tracks: $jsonResponse")
//
//                if (jsonResponse != null) {
//                    try {
//                        val json = Json {
//                            ignoreUnknownKeys = true // This will ignore unknown keys like "href"
//                        }
//                        val tracksResponse = json.decodeFromString<PlaylistTracks>(jsonResponse)
//
//                        // Map Track objects to PlaylistTrack with extra fields
//                        val playlistTracks = tracksResponse.items?.mapNotNull { item ->
//                            try {
//                                val track = item.track
//                                // Ensure the track is not null before processing
//                                if (track != null) {
//                                    PlaylistTrack(
//                                        track = Track(
//                                            album = track.album,
//                                            artists = track.artists,
//                                            availableMarkets = track.availableMarkets,
//                                            discNumber = track.discNumber,
//                                            durationMs = track.durationMs,
//                                            explicit = track.explicit,
//                                            externalIds = track.externalIds,
//                                            externalUrls = track.externalUrls,
//                                            href = track.href,
//                                            id = track.id,
//                                            local = track.local,
//                                            name = track.name,
//                                            popularity = track.popularity,
//                                            previewUrl = track.previewUrl,
//                                            trackNumber = track.trackNumber,
//                                            type = track.type,
//                                            uri = track.uri,
//                                            albumArtUrl = track.albumArtUrl,
//                                            voteCount = 0, // Default value for voteCount
//                                            added_by = track.added_by ?: AddedBy() // Default value for addedBy
//                                        ),
//                                        voteCount = 0,  // Default value for voteCount
//                                        added_by = User(),  // Default value for addedBy
//                                        addedCount = 0,  // Default value for addedCount
//                                        lastUpdated = System.currentTimeMillis()  // Default value for lastUpdated
//                                    )
//                                } else {
//                                    null // Skip this item if the track is null
//                                }
//                            } catch (e: Exception) {
//                                Log.e("SpotifyDataTrackError", "Error parsing track", e)
//                                null // Skip problematic items
//                            }
//                        } ?: emptyList()
//
//                        Log.d("SpotifyDataTracksFound", "Total tracks fetched: ${playlistTracks.size}")
//                        callback(playlistTracks) // Return the PlaylistTrack list
//                    } catch (e: Exception) {
//                        Log.e("SpotifyDataParseError", "Error parsing tracks response", e)
//                        callback(null) // Return null on parse error
//                    }
//                } else {
//                    Log.d("SpotifyDataNoResponse", "No response body received")
//                    callback(null) // Return null if no response body
//                }
//            } else {
//                Log.e("SpotifyDataError", "Error: ${response.code}")
//                callback(null) // Return null on error response
//            }
//        } catch (e: IOException) {
//            Log.e("SpotifyDataRequestFail", "Request failed", e)
//            callback(null) // Return null if request fails
//        }
//    }.start()
//}
//private fun fetchPlaylistFromSpotify(playlistId: String, accessToken: String, callback: (List<PlaylistTrack>?) -> Unit) {
//    CoroutineScope(Dispatchers.IO).launch {
//        val url = "https://api.spotify.com/v1/playlists/$playlistId"
//        val client = OkHttpClient()
//
//        val request = Request.Builder()
//            .url(url)
//            .addHeader("Authorization", "Bearer $accessToken")
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                e.printStackTrace()
//                callback(null)  // Handle failure
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                if (response.isSuccessful) {
//                    val responseBody = response.body?.string()
//                    val playlistResponse = Gson().fromJson(responseBody, PlaylistResponse::class.java)
//
//                    // Map Spotify response to your PlaylistTrack model
//                    val tracks = playlistResponse.tracks.items.mapNotNull {
//                        val track = it.track
//                        if (track != null) {
//                            PlaylistTrack(
//                                track = track,
//                                voteCount = 0,
//                                added_by = User(), // Default user, you can customize as needed
//                                addedCount = 0,
//                                lastUpdated = System.currentTimeMillis() // Current timestamp
//                            )
//                        } else {
//                            // Log the error or handle the null track case
//                            Log.e("PlaylistDetail", "Null track found, skipping this item")
//                            null
//                        }
//                    }
//
//
//                    // Now you can call your callback with the newly mapped tracks
//                    requireActivity().runOnUiThread {
//                        callback(tracks)
//                    }
//                } else {
//                    callback(null)  // Handle failure (e.g., show error)
//                }
//            }
//        })
//    }
//}



    private fun fetchPlaylistFromSpotify(playlistId: String, accessToken: String, callback: (List<PlaylistTrack>?) -> Unit) {
        val tracksUrl = "https://api.spotify.com/v1/playlists/$playlistId/tracks"
        Log.d("SpotifyDataServiceURL", "Request URL for tracks: $tracksUrl")

        val request = Request.Builder()
            .url(tracksUrl)
            .header("Authorization", "Bearer $accessToken")
            .build()

        val client = OkHttpClient()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    Log.d("SpotifyDataResponse", "Response for tracks: $jsonResponse")

                    if (jsonResponse != null) {
                        try {
//                            val json = Json { ignoreUnknownKeys = true } // Ignore unknown keys
//                            val tracksResponse = json.decodeFromString<PlaylistTracks>(jsonResponse)
                            val tracksResponse = Gson().fromJson(jsonResponse, PlaylistTracks::class.java)

                            // Map Track objects to PlaylistTrack with extra fields
                            val playlistTracks = tracksResponse.items?.mapNotNull { item ->
                                try {
                                    val track = item.track
                                    if (track != null) {
                                        // Fetch user data using added_by.externalUrl
                                        val addedByUser = fetchUserData(item.added_by?.id.toString(), accessToken)

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
                                                added_by = addedByUser // Using the fetched user data
                                            ),
                                            voteCount = 0,  // Default value for voteCount
                                            added_by = item.added_by,  // Default value for addedBy
                                            addedCount = 0,  // Default value for addedCount
                                            lastUpdated = System.currentTimeMillis()  // Default value for lastUpdated
                                        )
                                    } else {
                                        null // Skip this item if the track is null
                                    }
                                } catch (e: Exception) {
                                    Log.e("SpotifyDataTrackError", "Error parsing track", e)
                                    null // Skip problematic items
                                }
                            } ?: emptyList()

                            Log.d("SpotifyDataTracksFound", "Total tracks fetched: ${playlistTracks.size}")
                            withContext(Dispatchers.Main) {
                                callback(playlistTracks) // Return the PlaylistTrack list
                            }
                        } catch (e: Exception) {
                            Log.e("SpotifyDataParseError", "Error parsing tracks response", e)
                            withContext(Dispatchers.Main) {
                                callback(null) // Return null on parse error
                            }
                        }
                    } else {
                        Log.d("SpotifyDataNoResponse", "No response body received")
                        withContext(Dispatchers.Main) {
                            callback(null) // Return null if no response body
                        }
                    }
                } else {
                    Log.e("SpotifyDataError", "Error: ${response.code}")
                    withContext(Dispatchers.Main) {
                        callback(null) // Return null on error response
                    }
                }
            } catch (e: IOException) {
                Log.e("SpotifyDataRequestFail", "Request failed", e)
                withContext(Dispatchers.Main) {
                    callback(null) // Return null if request fails
                }
            }
        }
    }


    private val userCache = mutableMapOf<String, User>()

    // Function to fetch user data
    suspend fun fetchUserData(userId: String, accesstoken: String): User {
        // Check if user data is already cached
        userCache[userId]?.let {
            Log.d("SpotifyService", "Returning cached user data for: $userId")
            return it
        }

        // If not cached, make API request
        val validUrl = "https://api.spotify.com/v1/users/$userId"
        val request = Request.Builder()
            .url(validUrl)
            .header("Authorization", "Bearer ${accesstoken}")
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

//    private fun fetchUserData(userUrl: String, accessToken: String): User {
//        val client = OkHttpClient()
//
//        // Log the URL and access token for debugging
//        Log.d("fetchUserData", "URL: $userUrl")
//        Log.d("fetchUserData", "Access Token: $accessToken")
//
//        // Check if the URL has the correct scheme (http or https)
//        if (!userUrl.startsWith("http://") && !userUrl.startsWith("https://")) {
//            Log.e("fetchUserData", "Invalid URL scheme: $userUrl")
//            throw IllegalArgumentException("Expected URL scheme 'http' or 'https' but no scheme was found for $userUrl")
//        }
//
//        val request = Request.Builder()
//            .url(userUrl)
//            .addHeader("Authorization", "Bearer $accessToken")
//            .build()
//
//        try {
//            Log.d("fetchUserData", "Sending request to $userUrl")
//
//            val response = client.newCall(request).execute()
//
//            if (response.isSuccessful) {
//                val userResponse = response.body?.string()
//                Log.d("fetchUserData", "Response: $userResponse")
//
//                userResponse?.let {
//                    return Gson().fromJson(it, User::class.java)
//                }
//            } else {
//                Log.e("fetchUserData", "Request failed with status code: ${response.code}")
//            }
//        } catch (e: Exception) {
//            Log.e("fetchUserData", "Error during API request", e)
//        }
//
//        return User()
//    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is FragmentTransactionListener) {
            fragmentTransactionListener = activity as FragmentTransactionListener
        } else {
            throw RuntimeException(
                activity.toString()
                        + " must implement FragmentTransactionListener"
            )
        }
    }

    private fun updatePlaylistTracksInFirebase(playlistId: String, tracks: List<PlaylistTrack>) {
        firebaseRepository.updatePlaylistTracks(playlistId, tracks) { isSuccess ->
            if (isSuccess) {
                Log.d("PlaylistDetailFragment", "Tracks updated in Firebase")
            } else {
                Log.e("PlaylistDetailFragment", "Failed to update tracks in Firebase")
            }
        }
    }

    private fun updateUI(tracks: List<PlaylistTrack>) {
        Log.d("PlaylistDetailFragment", "updateUI: Updating UI with ${tracks.size} tracks")
        this.tracks.clear()
        this.tracks.addAll(tracks)
        playlistTracksAdapter.notifyDataSetChanged()
//        val playlistNameTextView = view?.findViewById<TextView>(R.id.playlist_title)
//        val playlistImage = view?.findViewById<ImageView>(R.id.playlist_image)
//        playlistNameTextView?.text = .name
//        if (playlistImage != null) {
//            Glide.with(this)
//                .load(playlist.images?.firstOrNull()?.url)
//                .placeholder(R.drawable.ic_music_note)
//                .into(playlistImage)
//        }
    }

    private fun updateUI(playlist: Playlist) {
        Log.d("PlaylistDetailFragment", "updateUI: Updating UI with ${playlist.tracks.items.size} tracks")

        this.tracks.clear()
        this.tracks.addAll(playlist.tracks.items)
        val playlistNameTextView = view?.findViewById<TextView>(R.id.playlist_title)
        val playlistImage = view?.findViewById<ImageView>(R.id.playlist_image)

        playlistNameTextView?.text = playlist.name
        if (playlistImage != null) {
            Glide.with(this)
                .load(playlist.images?.firstOrNull()?.url)
                .placeholder(R.drawable.ic_music_note_black)
                .into(playlistImage)
        }

        playlistTracksAdapter.notifyDataSetChanged()
    }

    private fun onVoteClicked(track: PlaylistTrack) {
        val userId = context?.let { getUserId(it) }

        if (userId != null) {
            Log.d("PlaylistDetailFragment", "onVoteClicked: Voting on track ${track.track.name}")

            firebaseRepository.updateTrackVote(playlistId!!, track, userId) { success ->
                if (success) {
                    track.voteCount += 1
                    playlistTracksAdapter.notifyDataSetChanged()
                    Log.d("PlaylistDetailFragment", "onVoteClicked: Track vote count updated")
                } else {
                    Toast.makeText(context, "You've already voted!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Please log in to vote!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(playlistId: String, playlistName: String): PlaylistDetailFragment {
            val fragment = PlaylistDetailFragment()
            val args = Bundle()
            args.putString("playlistId", playlistId)
            args.putString("playlistName", playlistName)
            fragment.arguments = args
            return fragment
        }
    }

    fun addTrackToPlaylist(track: PlaylistTrack) {
        Log.d("PlaylistDetailFragment", "addTrackToPlaylist: Adding track to playlist")
        tracks.add(track)
        playlistTracksAdapter.notifyDataSetChanged()
        updatePlaylistTracksInFirebase(playlistId!!, tracks)
    }
}