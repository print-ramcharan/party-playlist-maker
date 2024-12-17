package com.example.partyplaylist


import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_collaborative_playlist, container, false)
        firebaseRepository = FirebaseRepository(requireContext())
        initializeUIComponents(view)

        createPlaylistButton.setOnClickListener {
            createPlaylist()
        }

        return view
    }

    private fun initializeUIComponents(view: View) {
        playlistNameEditText = view.findViewById(R.id.playlist_name_edit_text)
        playlistDescriptionEditText = view.findViewById(R.id.playlist_description_edit_text)
        createPlaylistButton = view.findViewById(R.id.create_playlist_button)
    }

    private fun createPlaylist() {
        val name = playlistNameEditText.text.toString().trim()
        val description = playlistDescriptionEditText.text.toString().trim()
        val currentUser = SharedPreferencesManager.getUserProfile(requireContext())

        if (currentUser != null) {
            if (name.isNotEmpty()) {
                refreshTokenIfNeeded { newAccessToken ->
                    if (newAccessToken != null) {
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
                                    val playlistResponse = response.body()
                                    playlistResponse?.let {
                                        val playlist = Playlist(
                                            id = it.id,
                                            name = it.name,
                                            description = it.description,
                                            externalUrls = it.external_urls,
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
                                    Toast.makeText(context, "Failed to create playlist", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<PlaylistResponse>, t: Throwable) {
                                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(context, "Failed to refresh access token", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Playlist name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        } else {
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
                        callback(newAccessToken)
                    } else {
                        callback(null)
                    }
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
