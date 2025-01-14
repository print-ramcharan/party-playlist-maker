package com.example.partyplaylist

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.partyplaylist.models.ExternalUrls
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.PlaylistCreateRequest
import com.example.partyplaylist.models.PlaylistResponse
import com.example.partyplaylist.models.PlaylistResponse2
import com.example.partyplaylist.network.RetrofitClient
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.utils.SharedPreferencesManager
import com.example.partyplaylist.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CollaborativePlaylistFragment : Fragment() {

    private lateinit var playlistNameEditText: EditText
    private lateinit var playlistDescriptionEditText: EditText
    private lateinit var createPlaylistButton: Button
    private lateinit var collaborativeSwitch: Switch
    private lateinit var privateSwitch: Switch

    private lateinit var firebaseRepository: FirebaseRepository

    private var isCreatingPlaylist = false // Flag to track if a playlist is being created

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_collaborative_playlist, container, false)
        firebaseRepository = FirebaseRepository(requireContext())

        initializeUIComponents(view)

        // Button to create playlist
        createPlaylistButton.setOnClickListener {
            Log.d("CollaborativePlaylist", "Create playlist button clicked")
            createPlaylist()
        }

        return view
    }

    private fun initializeUIComponents(view: View) {
        playlistNameEditText = view.findViewById(R.id.playlist_name_edit_text)
        playlistDescriptionEditText = view.findViewById(R.id.playlist_description_edit_text)
        createPlaylistButton = view.findViewById(R.id.create_playlist_button)
        collaborativeSwitch = view.findViewById(R.id.collaborative_switch)
        privateSwitch = view.findViewById(R.id.private_mode_switch)

        // Disable the button initially
        createPlaylistButton.isEnabled = false

        // Add text listeners to enable/disable button
        playlistNameEditText.addTextChangedListener {
            createPlaylistButton.isEnabled = isFormValid()
        }

        playlistDescriptionEditText.addTextChangedListener {
            createPlaylistButton.isEnabled = isFormValid()
        }
    }

    private fun isFormValid(): Boolean {
        val name = playlistNameEditText.text.toString().trim()
        val description = playlistDescriptionEditText.text.toString().trim()
        return name.isNotEmpty() && description.isNotEmpty()
    }

    private fun createPlaylist() {
        val name = playlistNameEditText.text.toString().trim()
        val description = playlistDescriptionEditText.text.toString().trim()

        val currentUser = SharedPreferencesManager.getUserProfile(requireContext())

        if (currentUser != null) {
            Log.d("CollaborativePlaylist", "User logged in: ${currentUser.id}")
            if (name.isNotEmpty()) {
                refreshTokenIfNeeded { newAccessToken ->
                    if (newAccessToken != null) {
                        Log.d("CollaborativePlaylist", "New access token retrieved: $newAccessToken")

                        // Fetch the user's playlists first
                        val call = RetrofitClient.getSpotifyApiService().getUserPlaylists(
                            authHeader = "Bearer $newAccessToken"
                        )

                        call.enqueue(object : Callback<PlaylistResponse2> {  // Ensure PlaylistResponse is the correct type here
                            override fun onResponse(
                                call: Call<PlaylistResponse2>,
                                response: Response<PlaylistResponse2>
                            ) {
                                if (response.isSuccessful) {
                                    val existingPlaylists = response.body()?.items ?: emptyList() // Adjust according to response format

                                    // Check if the playlist already exists by comparing names
                                    val isDuplicate = existingPlaylists.any { it.name.equals(name, ignoreCase = true) }

                                    if (isDuplicate) {
                                        Log.w("CollaborativePlaylist", "Playlist with the same name already exists")
                                        Toast.makeText(context, "Playlist with this name already exists", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Proceed with creating the playlist if it's unique
                                        val playlistRequest = PlaylistCreateRequest(name, description)
                                        val createCall = RetrofitClient.getSpotifyApiService().createPlaylist(
                                            userId = currentUser.id,
                                            authHeader = "Bearer $newAccessToken",
                                            playlistCreateRequest = playlistRequest,
                                            collaborative = true,
                                            public = true
                                        )

                                        createCall.enqueue(object : Callback<PlaylistResponse> {
                                            override fun onResponse(
                                                call: Call<PlaylistResponse>,
                                                response: Response<PlaylistResponse>
                                            ) {
                                                if (response.isSuccessful) {
                                                    Log.d("CollaborativePlaylist", "Playlist created successfully")
                                                    response.body()?.let { playlistResponse ->
                                                        val playlist = Playlist(
                                                            id = playlistResponse.id,
                                                            name = playlistResponse.name,
                                                            description = playlistResponse.description,
                                                            externalUrls = playlistResponse.externalUrls ?: ExternalUrls(),
                                                            href = playlistResponse.href,
                                                            images = playlistResponse.images,
                                                            owner = playlistResponse.owner,
                                                            public = playlistResponse.public,
                                                            snapshotId = playlistResponse.snapshot_id,
                                                            tracks = playlistResponse.tracks,
                                                            type = playlistResponse.type,
                                                            uri = playlistResponse.uri,
                                                            collaborative = true,
                                                            collaborators = listOf(currentUser)
                                                        )
                                                        savePlaylistToDatabase(playlist)
                                                        Toast.makeText(context, "Playlist Created", Toast.LENGTH_SHORT).show()
                                                        requireActivity().supportFragmentManager.popBackStack()
                                                    }
                                                } else {
                                                    Log.e("CollaborativePlaylist", "Failed to create playlist: ${response.message()}")
                                                    Toast.makeText(context, "Failed to create playlist", Toast.LENGTH_SHORT).show()
                                                }
                                            }

                                            override fun onFailure(call: Call<PlaylistResponse>, t: Throwable) {
                                                Log.e("CollaborativePlaylist", "Network error: ${t.message}")
                                                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                    }
                                } else {
                                    Log.e("CollaborativePlaylist", "Failed to fetch user playlists: ${response.message()}")
                                    Toast.makeText(context, "Failed to fetch user playlists", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<PlaylistResponse2>, t: Throwable) {
                                Log.e("CollaborativePlaylist", "Network error while fetching playlists: ${t.message}")
                                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Log.e("CollaborativePlaylist", "Failed to refresh access token")
                        Toast.makeText(context, "Failed to refresh access token", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.w("CollaborativePlaylist", "Playlist name cannot be empty")
                Toast.makeText(context, "Playlist name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w("CollaborativePlaylist", "User details not found")
            Toast.makeText(context, "User details not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshTokenIfNeeded(callback: (String?) -> Unit) {
        val accessToken = SharedPreferencesManager.getAccessToken(requireContext())
        if (accessToken == null || isTokenExpired()) {
            val refreshToken = SharedPreferencesManager.getRefreshToken(requireContext())
            if (refreshToken != null) {
                val tokenManager = TokenManager(requireContext())
                tokenManager.refreshToken(refreshToken) { newAccessToken ->
                    if (newAccessToken != null) {
                        SharedPreferencesManager.saveAccessToken(requireContext(), newAccessToken)
                    }
                    callback(newAccessToken)
                }
            } else {
                callback(null)
            }
        } else {
            callback(accessToken)
        }
    }

    private fun savePlaylistToDatabase(playlist: Playlist) {
        firebaseRepository.savePlaylist(playlist)
    }

    private fun isTokenExpired(): Boolean {
        val prefs = requireContext().getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
        val expiryTime = prefs.getLong("access_token_expiry", 0)
        return System.currentTimeMillis() >= expiryTime
    }
}
