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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
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
    private lateinit var swipeRefreshLayout : SwipeRefreshLayout
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
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        swipeRefreshLayout.setOnRefreshListener {
            fetchPlaylistDetails(playlistId!!)
            swipeRefreshLayout.isRefreshing = false
        }
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
    fun addNewTrack(newTrack: MutableList<PlaylistTrack>) {
        // Add the new track to the existing playlist
        tracks.addAll(newTrack)

        // Update the adapter with the new track list
        playlistTracksAdapter.updatePlaylistTracks(tracks)

        // Optionally, update Firebase or perform any other necessary actions
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




    }




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