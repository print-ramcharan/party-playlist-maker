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
import com.example.partyplaylist.adapters.PlaylistTracksAdapter
import com.example.partyplaylist.models.PlaylistTrack
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.PlaylistResponse
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.utils.SharedPreferencesManager
import com.example.partyplaylist.utils.SharedPreferencesManager.getUserId
import com.example.partyplaylist.utils.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException

class PlaylistDetailFragment : Fragment() {

    private var playlistId: String? = null
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var playlistTracksAdapter: PlaylistTracksAdapter
    private val tracks: MutableList<PlaylistTrack> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = arguments?.getString("playlistId")
        firebaseRepository = FirebaseRepository(requireContext())
        Log.d("PlaylistDetailFragment", "onCreate: playlistId = $playlistId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.playlist_screen, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.playlist_recycler_view)
//        val playlistImage = view.findViewById<ImageView>(R.id.playlist_image)
//        val playlistNameTextView = view.findViewById<TextView>(R.id.playlist_title)

        playlistTracksAdapter = PlaylistTracksAdapter(tracks) { track ->
            onVoteClicked(track)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = playlistTracksAdapter

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

        // Check if tracks exist in Firebase
        firebaseRepository.getPlaylistTracks(playlistId) { tracksFromFirebase ->
            if (tracksFromFirebase != null && tracksFromFirebase.tracks.items.isNotEmpty()) {
                // Tracks found in Firebase, update UI with Firebase data
                Log.d("PlaylistDetailFragment", "Tracks found in Firebase")
                updateUI(tracksFromFirebase)
            } else {
                // No tracks in Firebase, fetch from Spotify API
                val accessToken = SharedPreferencesManager.getAccessToken(requireContext())


                if (accessToken != null) {
                    fetchPlaylistFromSpotify(playlistId, accessToken) { tracks ->
                        if (tracks != null) {
                            Log.d("PlaylistDetailFragment", "Tracks fetched from Spotify")
                            updateUI(tracks)
                            updatePlaylistTracksInFirebase(playlistId, tracks)  // Save fetched tracks to Firebase
                        } else {
                            Log.e("PlaylistDetailFragment", "Failed to fetch tracks from Spotify")
                        }
                    }
                }
            }
        }
    }

    private fun fetchPlaylistFromSpotify(playlistId: String, accessToken: String, callback: (List<PlaylistTrack>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://api.spotify.com/v1/playlists/$playlistId"
            val client = OkHttpClient()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    callback(null)  // Handle failure
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val playlistResponse = Gson().fromJson(responseBody, PlaylistResponse::class.java)

                        // Map Spotify response to your PlaylistTrack model
                        val tracks = playlistResponse.tracks.items.map {
                            PlaylistTrack(
                                track = it.track, // Using the actual Track object from the response
                                voteCount = 0, // Default initial vote count
                                addedBy = User(), // Default user, you can customize as needed
                                addedCount = 0, // Initialize added count
                                lastUpdated = System.currentTimeMillis() // Current timestamp
                            )
                        }

                        // Now you can call your callback with the newly mapped tracks
                        requireActivity().runOnUiThread {
                            callback(tracks)
                        }
                    } else {
                        callback(null)  // Handle failure (e.g., show error)
                    }
                }
            })
        }
    }

    private fun updatePlaylistTracksInFirebase(playlistId: String, tracks: List<PlaylistTrack>) {
        // Use FirebaseRepository to update the tracks in Firebase
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

        // Clear the current tracks and add the new ones from the Playlist
        this.tracks.clear()
        this.tracks.addAll(playlist.tracks.items) // Access the tracks from the Playlist
        val playlistNameTextView = view?.findViewById<TextView>(R.id.playlist_title)
        val playlistImage = view?.findViewById<ImageView>(R.id.playlist_image)

        playlistNameTextView?.text = playlist.name
        if (playlistImage != null) {
            Glide.with(this)
                .load(playlist.images?.firstOrNull()?.url)
                .placeholder(R.drawable.ic_music_note)
                .into(playlistImage)
        }

        // Update UI components like playlist name, description, etc.
//        playlistNameTextView.text = playlist.name
//        playlistDescriptionTextView.text = playlist.description

        // Load playlist image
//        Glide.with(this)
//            .load(playlist.images?.firstOrNull()?.url)
//            .placeholder(R.drawable.ic_music_note)
//            .into(playlistImageView)

        // Notify the adapter that the data has changed
        playlistTracksAdapter.notifyDataSetChanged()
    }


    private fun onVoteClicked(track: PlaylistTrack) {
        val userId = context?.let { getUserId(it) } // Get the current logged-in user's unique ID

        if (userId != null) {
            Log.d("PlaylistDetailFragment", "onVoteClicked: Voting on track ${track.track.name}")

            // Call the Firebase method to update the vote
            firebaseRepository.updateTrackVote(playlistId!!, track, userId) { success ->
                if (success) {
                    // Successfully voted, update the vote count
                    track.voteCount += 1
                    // Notify the adapter to update the UI
                    playlistTracksAdapter.notifyDataSetChanged()
                    Log.d("PlaylistDetailFragment", "onVoteClicked: Track vote count updated")
                } else {
                    // User has already voted
                    Toast.makeText(context, "You've already voted!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Handle the case where the user is not logged in
            Toast.makeText(context, "Please log in to vote!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(playlistId: String): PlaylistDetailFragment {
            val fragment = PlaylistDetailFragment()
            val args = Bundle()
            args.putString("playlistId", playlistId)
            fragment.arguments = args
            return fragment
        }
    }
}