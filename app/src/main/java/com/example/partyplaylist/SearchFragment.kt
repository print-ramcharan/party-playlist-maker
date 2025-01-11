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
import com.example.partyplaylist.adapters.SearchAdapter
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.data.SearchResponse
//import com.example.partyplaylist.models.data.Track
import com.example.partyplaylist.network.RetrofitClient
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.Query

class SearchFragment : Fragment() {
    private var searchBar: EditText? = null
    private var searchResultsRecycler: RecyclerView? = null
    private var searchAdapter: SearchAdapter? = null
    private var searchResults: MutableList<Any> = mutableListOf()  // Using Any to handle both Track and Album
    private var searchJob: Job? = null
    private lateinit var firebaseRepository: FirebaseRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Initialize components
        searchBar = view.findViewById(R.id.search_bar)
        searchResultsRecycler = view.findViewById(R.id.search_results_recycler)
        firebaseRepository =  FirebaseRepository(requireContext())
        // Setup RecyclerView
        searchAdapter = SearchAdapter(searchResults)
        searchResultsRecycler?.layoutManager = LinearLayoutManager(context)
        searchResultsRecycler?.adapter = searchAdapter

        firebaseRepository.getTracks { tracks ->
            tracks?.let {
                searchResults.addAll(it)
                searchAdapter?.notifyDataSetChanged()
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
                    val response: Response<SearchResponse> = RetrofitClient
                        .getSpotifyApiService()
                        .searchSpotify(query, "track,album,artist", 10, "Bearer $accessToken")

                    if (response.isSuccessful) {
                        val searchResponse = response.body()
                        Log.d("SearchFragment", "Response body: $searchResponse")

                        // Clear previous search results
                        withContext(Dispatchers.Main) {
                            searchResults.clear()
                        }

                        // Handle albums first (if available)
                        val albumList = searchResponse?.albums?.albums ?: emptyList<Album>()
                        Log.d("SearchFragment", "Album list size: ${albumList.size}")  // Log album count
                        if (albumList.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                searchResults.addAll(albumList)  // Add albums to the list first
                                Log.d("SearchFragment", "Albums added: ${albumList.size}")  // Log albums added
                            }
                        } else {
                            Log.d("SearchFragment", "No albums found.")  // Log if no albums are found
                        }

                        // Handle tracks second (if available)
                        val trackList = searchResponse?.tracks?.tracks ?: emptyList()
                        Log.d("SearchFragment", "Track list size: ${trackList.size}")  // Log track count
                        if (trackList.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                searchResults.addAll(trackList)  // Add tracks to the list after albums
                                Log.d("SearchFragment", "Tracks added: ${trackList.size}")  // Log tracks added
                            }
                        } else {
                            Log.d("SearchFragment", "No tracks found.")  // Log if no tracks are found
                        }

                        // Update UI after adding both albums and tracks
                        withContext(Dispatchers.Main) {
                            searchAdapter?.notifyDataSetChanged() // Notify adapter that the list has been updated
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

}



