package com.example.partyplaylist

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.partyplaylist.adapters.UserAdapter
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.ExternalUrls
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.PlaylistCreateRequest
import com.example.partyplaylist.models.PlaylistResponse
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
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var userAdapter: UserAdapter
    private lateinit var recyclerView: RecyclerView
    private var users : MutableList<User> = mutableListOf()
    private var userIds : List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_collaborative_playlist, container, false)
        firebaseRepository = FirebaseRepository(requireContext())
        initializeUIComponents(view)

        // Log for userId retrieval
        Log.d("CollaborativePlaylist", "Fetching user IDs from Firebase")
        firebaseRepository.getUserIds { userIds ->
            Log.d("CollaborativePlaylist", "User IDs retrieved: $userIds")
            this.userIds = userIds

            // Log for user data retrieval
            Log.d("CollaborativePlaylist", "Fetching users for IDs: $userIds")
            firebaseRepository.getUsers(userIds) { users ->
                activity?.runOnUiThread {
                    Log.d("CollaborativePlaylist", "Users retrieved: $users")
                    if (users.isNotEmpty()) {
                        userAdapter = UserAdapter(users)
                        recyclerView.adapter = userAdapter
                    } else {
                        // Handle empty state if necessary
                        Log.d("CollaborativePlaylist", "No users to display")
                    }
                }
            }
        }

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
        recyclerView = view.findViewById(R.id.playlists_recycler_view_2)
        recyclerView.layoutManager = LinearLayoutManager(context)
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
                        val playlistRequest = PlaylistCreateRequest(name, description, false)
                        val call = RetrofitClient.getSpotifyApiService().createPlaylist(
                            userId = currentUser.id,
                            authHeader = "Bearer $newAccessToken",
                            playlistCreateRequest = playlistRequest
                        )

                        call.enqueue(object : Callback<PlaylistResponse> {
                            override fun onResponse(
                                call: Call<PlaylistResponse>,
                                response: Response<PlaylistResponse>
                            ) {
                                if (response.isSuccessful) {
                                    Log.d("CollaborativePlaylist", "Playlist created successfully: ${response.body()}")
                                    val playlistResponse = response.body()
                                    playlistResponse?.let {
                                        val playlist = Playlist(
                                            id = it.id,
                                            name = it.name,
                                            description = it.description,
                                            externalUrls = it.externalUrls ?: ExternalUrls(),
                                            href = it.href,
                                            images = it.images,
                                            owner = it.owner,
                                            public = it.public,
                                            snapshotId = it.snapshot_id,
                                            tracks = it.tracks,
                                            type = it.type,
                                            uri = it.uri,
                                            collaborative = true,
                                            collaborators = listOf(currentUser)
                                        )
                                        savePlaylistToDatabase(playlist)
                                        Toast.makeText(context, "Playlist ${playlist.name} Created", Toast.LENGTH_SHORT).show()
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
        Log.d("CollaborativePlaylist", "Current access token: $accessToken")
        if (accessToken == null || isTokenExpired()) {
            val refreshToken = SharedPreferencesManager.getRefreshToken(requireContext())
            Log.d("CollaborativePlaylist", "Refreshing token with refresh token: $refreshToken")
            if (refreshToken != null) {
                val tokenManager = TokenManager(requireContext())
                tokenManager.refreshToken(refreshToken) { newAccessToken ->
                    if (newAccessToken != null) {
                        Log.d("CollaborativePlaylist", "New access token: $newAccessToken")
                        SharedPreferencesManager.saveAccessToken(requireContext(), newAccessToken)
                        callback(newAccessToken)
                    } else {
                        Log.e("CollaborativePlaylist", "Failed to refresh token")
                        callback(null)
                    }
                }
            } else {
                Log.e("CollaborativePlaylist", "No refresh token available")
                callback(null)
            }
        } else {
            callback(accessToken)
        }
    }

    private fun savePlaylistToDatabase(playlist: Playlist) {
        Log.d("CollaborativePlaylist", "Saving playlist to Firebase: ${playlist.name}")
        firebaseRepository.savePlaylist(playlist)
    }

    private fun isTokenExpired(): Boolean {
        val prefs = requireContext().getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
        val expiryTime = prefs.getLong("access_token_expiry", 0)
        val isExpired = System.currentTimeMillis() >= expiryTime
        Log.d("CollaborativePlaylist", "Token expired: $isExpired")
        return isExpired
    }
}
