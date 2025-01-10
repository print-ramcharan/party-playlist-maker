package com.example.partyplaylist

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.partyplaylist.adapters.SearchAdapterToAdd
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.network.RetrofitClient
import com.example.partyplaylist.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class SearchFragmentToAdd(private val playlistDetailFragment: PlaylistDetailFragment) : Fragment() {

    private var searchBar: EditText? = null
    private var searchResultsRecycler: RecyclerView? = null
    private var searchAdapterToAdd: SearchAdapterToAdd? = null
    private var searchResults: MutableList<Any> = mutableListOf()  // To handle both Track and Album
    private var selectedTracks: MutableList<Track> = mutableListOf()  // Tracks that are selected by the user
    private var searchJob: Job? = null
    private var confirmButton: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_add, container, false)

        // Initialize components
        searchBar = view.findViewById(R.id.search_bar)
        searchResultsRecycler = view.findViewById(R.id.search_results_recycler)
        confirmButton = view.findViewById(R.id.confirm_button)  // Confirm button for adding songs

        // Setup RecyclerView
        searchAdapterToAdd = SearchAdapterToAdd(searchResults, selectedTracks)
        searchAdapterToAdd!!.setListener(object :SearchAdapterToAdd.SongSelectionListener{
            override fun onSongSelected(track: Track) {
                // Handle song selection
                Log.d("SearchAdapter", "Song selected: ${track.name}")
                selectedTracks.add(track)
            }

            override fun onSongDeselected(track: Track) {
                // Handle song deselection
                Log.d("SearchAdapter", "Song deselected: ${track.name}")
                selectedTracks.remove(track)
            }
        })
        searchResultsRecycler?.layoutManager = LinearLayoutManager(context)
        searchResultsRecycler?.adapter = searchAdapterToAdd

        // Handle Confirm Button Click
        confirmButton?.setOnClickListener {
            if (selectedTracks.isNotEmpty()) {
                // Perform API request to add selected tracks to the playlist
                addTracksToPlaylistApiRequest(selectedTracks)
            } else {
                Toast.makeText(context, "No tracks selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Search text listener
        searchBar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                searchJob?.cancel() // Cancel any previous search job
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(300) // Wait for 300ms after typing stops before searching
                    performSearch(editable.toString().trim()) // Perform the search with the current text
                }
            }
        })

        return view
    }

    private fun performSearch(query: String) {
        Log.d("SearchFragment", "Search query: $query")

        if (TextUtils.isEmpty(query)) {
            Toast.makeText(context, "Please enter a search term!", Toast.LENGTH_SHORT).show()
            return
        }

        // Use the TokenManager to refresh the token if needed
        val tokenManager = context?.let { TokenManager(it) }

        tokenManager?.refreshTokenIfNeeded { accessToken ->
            if (accessToken == null) {
                Toast.makeText(context, "Access token is null, please log in again!", Toast.LENGTH_SHORT).show()
                return@refreshTokenIfNeeded
            }

            // Perform the network request in a coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient
                        .getSpotifyApiService()
                        .searchSpotify(query, "track,album,artist", 10, "Bearer $accessToken")

                    if (response.isSuccessful) {
                        val searchResponse = response.body()
                        Log.d("SearchFragment", "Response body: $searchResponse")

                        // Clear previous search results
                        withContext(Dispatchers.Main) {
                            searchResults.clear()
                        }

                        // Handle tracks if available
                        val trackList = searchResponse?.tracks?.tracks ?: emptyList()
                        Log.d("SearchFragment", "Track list size: ${trackList.size}")
                        if (trackList.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                searchResults.addAll(trackList)
                                Log.d("SearchFragment", "Tracks added: ${trackList.size}")
                            }
                        } else {
                            Log.d("SearchFragment", "No tracks found.")
                        }

                        // Handle albums if available
                        val albumList = searchResponse?.albums?.albums ?: emptyList()
                        Log.d("SearchFragment", "Album list size: ${albumList.size}")
                        if (albumList.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                searchResults.addAll(albumList)
                                Log.d("SearchFragment", "Albums added: ${albumList.size}")
                            }
                        } else {
                            Log.d("SearchFragment", "No albums found.")
                        }

                        // Update UI after adding both tracks and albums
                        withContext(Dispatchers.Main) {
                            searchAdapterToAdd?.notifyDataSetChanged()
                            Log.d("SearchFragment", "Adapter notified with updated results.")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                        Log.e("SearchFragment", "API response error: ${response.message()}")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("SearchFragment", "Exception: ${e.message}")
                }
            }
        }
    }

    private fun addTracksToPlaylistApiRequest(selectedTracks: List<Track>) {
        // Prepare the API request body (Spotify expects URIs of tracks)
        val playlistId = playlistDetailFragment.getPlaylistId()
        val trackUris = selectedTracks.map { "spotify:track:${it.id}" }  // Assuming 'id' is the track ID

        // Prepare the request body for Spotify API
        val requestBody = mapOf("uris" to trackUris)

        // Get the access token
        val tokenManager = context?.let { TokenManager(it) }
        val accessToken = tokenManager?.getAccessToken()

        if (accessToken != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response: Response<Unit> = RetrofitClient.getSpotifyApiService().addTracksToPlaylist(
                        "Bearer $accessToken",
                        playlistId,
                        requestBody
                    )

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            // On success, show a message and close the fragment
                            Toast.makeText(context, "Tracks added to the playlist successfully!", Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(context, "Failed to add tracks. Try again later.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("SearchFragment", "API request failed: ${e.message}")
                }
            }
        } else {
            Toast.makeText(context, "Access token is missing", Toast.LENGTH_SHORT).show()
        }
    }
}
