package com.example.partyplaylist

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.partyplaylist.models.data.Song
import com.example.partyplaylist.network.RetrofitClient
import com.example.partyplaylist.adapters.SearchAdapter
import com.example.partyplaylist.models.data.SearchResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import com.example.partyplaylist.utils.TokenManager

class SearchFragment : Fragment() {
    private var searchBar: EditText? = null
    private var searchResultsRecycler: RecyclerView? = null
    private var searchAdapter: SearchAdapter? = null
    private var searchResults: MutableList<Song> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Initialize components
        searchBar = view.findViewById(R.id.search_bar)
        searchResultsRecycler = view.findViewById(R.id.search_results_recycler)

        // Setup RecyclerView
        searchAdapter = SearchAdapter(searchResults)
        searchResultsRecycler?.layoutManager = LinearLayoutManager(context)
        searchResultsRecycler?.adapter = searchAdapter

        // Search button click listener (use a button for the actual search or listen to text changes)
        view.findViewById<View>(androidx.appcompat.R.id.search_bar).setOnClickListener {
            performSearch()
        }

        return view
    }

    private fun performSearch() {
        val query = searchBar!!.text.toString().trim()
        Log.d("SearchFragment", "Search query: $query")

        if (TextUtils.isEmpty(query)) {
            Toast.makeText(context, "Please enter a search term!", Toast.LENGTH_SHORT).show()
            Log.d("SearchFragment", "Search query is empty")
            return
        }

        // Use the TokenManager to refresh the token if needed
        val tokenManager = context?.let { TokenManager(it) }

        if (tokenManager != null) {
            Log.d("SearchFragment", "Refreshing token if needed")
            tokenManager.refreshTokenIfNeeded { accessToken ->
                Log.d("SearchFragment", "Access token after refresh: $accessToken")

                // If access token is null after refresh attempt, show a message
                if (accessToken == null) {
                    Toast.makeText(context, "Access token is null, please log in again!", Toast.LENGTH_SHORT).show()
                    Log.e("SearchFragment", "Access token is null")
                    return@refreshTokenIfNeeded
                }

                // Perform the network request in a coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d("SearchFragment", "Making API call with token: $accessToken")
                        val response: Response<SearchResponse> = RetrofitClient
                            .getSpotifyApiService()
                            .searchSpotify(query, "track,album,artist", 10, "Bearer $accessToken")

                        Log.d("SearchFragment", "API response code: ${response.code()}")

                        // Handle the response
                        if (response.isSuccessful) {
                            val searchResponse = response.body()
                            Log.d("SearchFragment", "Response body: $searchResponse")

                            // First, check for tracks, otherwise fallback to albums
                            val trackList = searchResponse?.tracks?.tracks ?: emptyList()
                            if (trackList.isNotEmpty()) {
                                val songList = trackList.map { track ->
                                    val artistNames = track.artists?.joinToString(", ") { it.name } ?: "Unknown Artist"
                                    val albumName = track.album?.name ?: "Unknown Album"
                                    val image = track.album?.images?.firstOrNull()?.url ?: ""  // Safely get the first image URL, or empty string if not available

                                    Song(track.name, track.previewUrl, artistNames, albumName, image)
                                }
                                withContext(Dispatchers.Main) {
                                    Log.d("SearchFragment", "Updating UI with new songs")
                                    searchAdapter?.updateList(songList)
                                    searchResults.clear()
                                    searchResults.addAll(songList)
                                    searchAdapter?.notifyDataSetChanged()
                                }
                            } else {
                                // Fallback to albums if no tracks are found
                                val albumList = searchResponse?.albums?.items ?: emptyList()
                                val albumSongs = albumList.map { album ->
                                    val artistNames = album.artists?.joinToString(", ") { it.name } ?: "Unknown Artist"
                                    val albumName = album.name ?: "Unknown Album"
                                    val image = album.images?.firstOrNull()?.url ?: ""  // Safely get the first image URL, or empty string if not available

                                    Song(album.name, "", artistNames, albumName, image)
                                }
                                withContext(Dispatchers.Main) {
                                    Log.d("SearchFragment", "Updating UI with album songs")
                                    searchAdapter?.updateList(albumSongs)
                                    searchResults.clear()
                                    searchResults.addAll(albumSongs)
                                    searchAdapter?.notifyDataSetChanged()
                                }
                            }
                        } else if (response.code() == 401) {
                            // Token expired, retry the search
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Access token expired, refreshing...", Toast.LENGTH_SHORT).show()
                                Log.d("SearchFragment", "Token expired, refreshing...")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                                Log.e("SearchFragment", "Error response: ${response.message()}")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("SearchFragment", "Exception: ${e.message}")
                        }
                    }
                }
            }
        }
    }

}

