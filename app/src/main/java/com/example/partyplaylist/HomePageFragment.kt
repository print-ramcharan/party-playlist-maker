package com.example.partyplaylist

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.partyplaylist.adapters.AlbumAdapter
import com.example.partyplaylist.adapters.TrackAdapter
import com.example.partyplaylist.databinding.FragmentHomePageBinding
import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.services.SpotifyDataService
import com.example.partyplaylist.utils.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class HomePageFragment : Fragment() {

    interface OnArtistSelectedListener {
        fun onArtistSelected(artistName: String?)
        fun onAlbumSelected(albumId: String?)
    }

    private var listener: OnArtistSelectedListener? = null

    // Firebase Repository
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var albumRecyclerView: RecyclerView
    private lateinit var trackRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout : SwipeRefreshLayout

    private var isArtistsLoaded : Boolean = false
    private var isAlbumsLoaded : Boolean = false
    private var isTracksLoaded : Boolean = false


    private val albums = mutableListOf<Album>()
    private val tracks = mutableListOf<Track>()

    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var trackAdapter: TrackAdapter

    // ImageView and TextView variables
    private lateinit var imageView1: ImageView
    private lateinit var textView1: TextView
    private lateinit var imageView2: ImageView
    private lateinit var textView2: TextView
    private lateinit var imageView3: ImageView
    private lateinit var textView3: TextView
    private lateinit var imageView4: ImageView
    private lateinit var textView4: TextView
    private lateinit var imageView5: ImageView
    private lateinit var textView5: TextView
    private lateinit var imageView6: ImageView
    private lateinit var textView6: TextView
    private lateinit var imageView7: ImageView
    private lateinit var textView7: TextView
    private lateinit var imageView8: ImageView
    private lateinit var textView8: TextView

    // LinearLayout variables for recent items
    private lateinit var recentLayout1: LinearLayout
    private lateinit var recentLayout2: LinearLayout
    private lateinit var recentLayout3: LinearLayout
    private lateinit var recentLayout4: LinearLayout
    private lateinit var recentLayout5: LinearLayout
    private lateinit var recentLayout6: LinearLayout
    private lateinit var recentLayout7: LinearLayout
    private lateinit var recentLayout8: LinearLayout

    private val imageUrls = mutableListOf<String?>()
    private val artistNames = mutableListOf<String?>()


    private lateinit var spotifyDataService: SpotifyDataService

    // View Binding
    private var _binding: FragmentHomePageBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomePageBinding.inflate(inflater, container, false)
        val view = binding.root

        firebaseRepository = FirebaseRepository(this.requireContext())

        spotifyDataService = SpotifyDataService(this.requireContext())
        trackRecyclerView = binding.tracksRecyclerview
        trackRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        trackAdapter = TrackAdapter(tracks)
        trackRecyclerView.adapter = trackAdapter

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);



        // Initialize RecyclerView for albums
        albumRecyclerView = binding.albumRecyclerview
        albumRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        albumAdapter = AlbumAdapter(albums) { album ->
            openAlbumTracksFragment(album)
        }
        albumRecyclerView.adapter = albumAdapter

        // Initialize ImageViews and TextViews
        imageView1 = view.findViewById(R.id.imageView1)
        textView1 = view.findViewById(R.id.recenttext1)
        imageView2 = view.findViewById(R.id.imageView2)
        textView2 = view.findViewById(R.id.recenttext2)
        imageView3 = view.findViewById(R.id.imageView3)
        textView3 = view.findViewById(R.id.recenttext3)
        imageView4 = view.findViewById(R.id.imageView4)
        textView4 = view.findViewById(R.id.recenttext4)
        imageView5 = view.findViewById(R.id.recentimg5)
        textView5 = view.findViewById(R.id.recenttext5)
        imageView6 = view.findViewById(R.id.recentimg6)
        textView6 = view.findViewById(R.id.recenttext6)
        imageView7 = view.findViewById(R.id.recentimg7)
        textView7 = view.findViewById(R.id.recenttext7)
        imageView8 = view.findViewById(R.id.recentimg8)
        textView8 = view.findViewById(R.id.recenttext8)

        // Initialize LinearLayouts for recent items
        recentLayout1 = view.findViewById(R.id.recent1)
        recentLayout2 = view.findViewById(R.id.recent2)
        recentLayout3 = view.findViewById(R.id.recent3)
        recentLayout4 = view.findViewById(R.id.recent4)
        recentLayout5 = view.findViewById(R.id.recent5)
        recentLayout6 = view.findViewById(R.id.recent6)
        recentLayout7 = view.findViewById(R.id.recent7)
        recentLayout8 = view.findViewById(R.id.recent8)

//        // Fetch and display artists
//        fetchAndDisplayArtists()
////
////        // Fetch and display albums
//        fetchAndDisplayAlbums()
//        fetchAndDisplayTracks()
        // Setup click listeners
        setupClickListeners()

        swipeRefreshLayout.setOnRefreshListener {
            // Fetch the data when the user swipes down
            reloadData()
        }
        return view
    }

    fun enableSwipeRefresh() {
        binding?.swipeRefreshLayout?.isEnabled = true
    }

    override fun onResume() {
        super.onResume()
        binding.swipeRefreshLayout.isEnabled = true
        enableSwipeRefresh()
        reloadData()
    }


    private fun reloadData() {
        swipeRefreshLayout.isRefreshing = true // Show the refresh spinner
        fetchAndDisplayArtists()
        fetchAndDisplayAlbums()
        fetchAndDisplayTracks()

       // Hide the refresh spinner
    }

    private fun openAlbumTracksFragment(album: Album) {
        val fragment = AlbumTracksFragment().apply {
            arguments = Bundle().apply {
                putParcelable("album", album) // Assuming Album implements Parcelable
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.view_page, fragment) // Use the correct container ID
            .addToBackStack(null)
            .commit()
    }

    private fun fetchAndDisplayAlbums() {
        firebaseRepository.getAllAlbums { fetchedAlbums ->
            if (fetchedAlbums != null) {
                Log.d("HomePageFragment", "Albums fetched: ${fetchedAlbums.size}")
                albums.clear()
                albums.addAll(fetchedAlbums) // Add all fetched albums
                albumAdapter.notifyDataSetChanged() // Notify adapter of the data change
            } else {
                Log.e("HomePageFragment", "No albums fetched or error occurred")
            }
            isAlbumsLoaded = true
            if(isAlbumsLoaded && isArtistsLoaded && isTracksLoaded){
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun fetchAndDisplayTracks() {
        firebaseRepository.getAllTracks { fetchedTracks ->
            if (fetchedTracks != null) {
                Log.d("HomePageFragment", "Albums fetched: ${fetchedTracks.size}")
                tracks.clear()
                tracks.addAll(fetchedTracks) // Add all fetched albums
                trackAdapter.notifyDataSetChanged() // Notify adapter of the data change
            } else {
                Log.e("HomePageFragment", "No albums fetched or error occurred")
            }
            isTracksLoaded = true
            if(isAlbumsLoaded && isArtistsLoaded && isTracksLoaded){
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    private fun fetchAndDisplayArtists() {
        // First, fetch artists from Firebase
        firebaseRepository.getAllArtists { artists ->
            val artistList = artists?.takeLast(8)?.toMutableList() ?: mutableListOf()

            // Clear existing data
            imageUrls.clear()
            artistNames.clear()

            // Add the Firebase artists to the lists
            for (artist in artistList) {
                imageUrls.add(artist.images.firstOrNull()?.url)
                artistNames.add(artist.name)
            }

            // If there are less than 8 artists, fetch more from the Spotify API
            if (artistList.size < 8) {
                // Define the artist IDs to fetch
                val artistIds = listOf(
                    "5cj0lLjcoR7YOSnhnX0Po5", // Doja Cat
                    "6M2wZ9GZgrQXHCFfjv46we", // Dua Lipa
                    "6qqNVTkY8uBg9cP3Jd7DAH", // Billie Eilish
                    "0EmeFodog0BfCgMzAIvKQp", // Shakira
                    "2YZyLoL8N0Wb9xBt1NhZWg", // Kendrick Lamar
                    "3TVXtAsR1Inumwj472S9r4", // Drake
                    "7jVv8c5Fj3E9VhNjxT4snq", // Lil Nas X
                    "6l3HvQ5sa6mXTsMTB19rO5"  // J. Cole
                )
                // Fetch additional artists from the Spotify API
                fetchArtistsFromSpotifyByIds(artistIds) { additionalArtists ->
                    additionalArtists?.let {
                        val remainingSlots = 8 - artistList.size
                        val additionalArtistsToAdd = it.take(remainingSlots)

                        // Add the additional artists to the lists
                        for (artist in additionalArtistsToAdd) {
                            imageUrls.add(artist.images.firstOrNull()?.url)
                            artistNames.add(artist.name)
                        }
                    }

                    // Display the artists
                    displayArtists()

//                    swipeRefreshLayout.isRefreshing = false
                }
            } else {
                // If there are enough artists, display them
                displayArtists()
//                swipeRefreshLayout.isRefreshing = false
            }
//            isArtistsLoaded = true
        }
    }


    private fun displayArtists() {
        // Display artists' images and names
        imageUrls.getOrNull(0)?.let { Glide.with(this).load(it).into(imageView1) }
        textView1.text = artistNames.getOrNull(0)

        imageUrls.getOrNull(1)?.let { Glide.with(this).load(it).into(imageView2) }
        textView2.text = artistNames.getOrNull(1)

        imageUrls.getOrNull(2)?.let { Glide.with(this).load(it).into(imageView3) }
        textView3.text = artistNames.getOrNull(2)

        imageUrls.getOrNull(3)?.let { Glide.with(this).load(it).into(imageView4) }
        textView4.text = artistNames.getOrNull(3)

        imageUrls.getOrNull(4)?.let { Glide.with(this).load(it).into(imageView5) }
        textView5.text = artistNames.getOrNull(4)

        imageUrls.getOrNull(5)?.let { Glide.with(this).load(it).into(imageView6) }
        textView6.text = artistNames.getOrNull(5)

        imageUrls.getOrNull(6)?.let { Glide.with(this).load(it).into(imageView7) }
        textView7.text = artistNames.getOrNull(6)

        imageUrls.getOrNull(7)?.let { Glide.with(this).load(it).into(imageView8) }
        textView8.text = artistNames.getOrNull(7)
       isArtistsLoaded = true
        if(isAlbumsLoaded && isArtistsLoaded && isTracksLoaded){
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun fetchArtistsFromSpotifyByIds(
        artistIds: List<String>,
        callback: (List<Artist>?) -> Unit
    ) {
        val tokenManager = context?.let { TokenManager(it) }

        tokenManager?.refreshTokenIfNeeded { accessToken ->
            if (accessToken == null) {
                Toast.makeText(
                    context,
                    "Access token is null, please log in again!",
                    Toast.LENGTH_SHORT
                ).show()
//                swipeRefreshLayout.isRefreshing = false
                return@refreshTokenIfNeeded
            }

            // Start a coroutine for background task
            CoroutineScope(Dispatchers.IO).launch {
                val artistsList = mutableListOf<Artist>()

                // Fetch artists one by one
                for (artistId in artistIds) {
                    try {
                        val url = URL("https://api.spotify.com/v1/artists/$artistId")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.setRequestProperty("Authorization", "Bearer $accessToken")
                        connection.requestMethod = "GET"

                        // Read the response
                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val response = connection.inputStream.bufferedReader().readText()
                            val artist = parseArtistResponse(response)
                            artist?.let { artistsList.add(it) }
                        } else {
                            Log.e("FetchArtists", "Error fetching artist ID $artistId: $responseCode")
                        }

                        connection.disconnect()
                    } catch (e: Exception) {
                        Log.e("FetchArtists", "Exception for artist ID $artistId: ${e.message}")
                    }
                }

                // Pass the list of artists to the callback
                withContext(Dispatchers.Main) {
                    firebaseRepository.saveArtists(artistsList)
                    for(artist in artistsList) {
                        fetchAndSaveArtistTopTracks(artist.id)
                    }
                    callback(artistsList)
//                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }
    private fun fetchAndSaveArtistTopTracks( artistId: String) {
        Log.d("SpotifySyncService", "Fetching top tracks for artist: $artistId")
        spotifyDataService.fetchArtistTopTracks(artistId) { tracks ->
            tracks?.let {
                Log.d("SpotifySyncService", "Top tracks fetched: ${it.size} tracks")
                firebaseRepository.saveArtistTopTracks(artistId, it) // Ensure you have a method for saving top tracks
            } ?: Log.e("SpotifySyncService", "Failed to fetch top tracks for artist: $artistId")
        }
    }
    // Function to parse the artist response (you may need to adjust it based on the actual response structure)
    private fun parseArtistResponse(response: String): Artist? {
        try {
            // Use Gson or another JSON parser to convert the response into an Artist object
            val gson = Gson()
            val artist = gson.fromJson(response, Artist::class.java)
            return artist
        } catch (e: Exception) {
            Log.e("FetchArtists", "Error parsing artist response: ${e.message}")
            return null
        }
    }


    private fun setupClickListeners() {
        albumAdapter = AlbumAdapter(albums) { album ->
            openAlbumPage(album.id)
        }
        albumRecyclerView.adapter = albumAdapter

        // Setting up click listeners for recent LinearLayouts
        recentLayout1.setOnClickListener {
            Log.d("HomePageFragment", "RecentLayout1 clicked, artist: ${artistNames.getOrNull(0)}")
            openDetailsPage(artistNames.getOrNull(0))
        }

        recentLayout2.setOnClickListener {
            Log.d("HomePageFragment", "RecentLayout2 clicked, artist: ${artistNames.getOrNull(1)}")
            openDetailsPage(artistNames.getOrNull(1))
        }

        recentLayout3.setOnClickListener {
            Log.d("HomePageFragment", "RecentLayout3 clicked, artist: ${artistNames.getOrNull(2)}")
            openDetailsPage(artistNames.getOrNull(2))
        }

        recentLayout4.setOnClickListener {
            Log.d("HomePageFragment", "RecentLayout4 clicked, artist: ${artistNames.getOrNull(3)}")
            openDetailsPage(artistNames.getOrNull(3))
        }

        recentLayout5.setOnClickListener {
            Log.d("HomePageFragment", "RecentLayout5 clicked, artist: ${artistNames.getOrNull(4)}")
            openDetailsPage(artistNames.getOrNull(4))
        }

        recentLayout6.setOnClickListener {
            Log.d("HomePageFragment", "RecentLayout6 clicked, artist: ${artistNames.getOrNull(5)}")
            openDetailsPage(artistNames.getOrNull(5))
        }

        recentLayout7.setOnClickListener {
            Log.d("HomePageFragment", "RecentLayout7 clicked, artist: ${artistNames.getOrNull(6)}")
            openDetailsPage(artistNames.getOrNull(6))
        }

        recentLayout8.setOnClickListener {
            Log.d("HomePageFragment", "RecentLayout8 clicked, artist: ${artistNames.getOrNull(7)}")
            openDetailsPage(artistNames.getOrNull(7))
        }
    }

//    private fun openDetailsPage(artistName: String?) {
//        Log.d("HomePageFragment", "openDetailsPage called with artistName: $artistName")
//        listener?.onArtistSelected(artistName)
//    }
//
//    private fun openAlbumPage(albumId: String?) {
//        Log.d("HomePageFragment", "openAlbumPage called with albumId: $albumId")
//        listener?.onAlbumSelected(albumId)
//    }
private fun openDetailsPage(artistName: String?) {
    binding.swipeRefreshLayout.isRefreshing = false // Stop refreshing
    binding.swipeRefreshLayout.isEnabled = false    // Disable SwipeRefresh
    Log.d("HomePageFragment", "openDetailsPage called with artistName: $artistName")
    listener?.onArtistSelected(artistName)
}

    private fun openAlbumPage(albumId: String?) {
        binding.swipeRefreshLayout.isRefreshing = false // Stop refreshing
        binding.swipeRefreshLayout.isEnabled = false    // Disable SwipeRefresh
        Log.d("HomePageFragment", "openAlbumPage called with albumId: $albumId")
        listener?.onAlbumSelected(albumId)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnArtistSelectedListener) {
            listener = context
            Log.d("HomePageFragment", "Listener attached")
        } else {
            throw RuntimeException(context.toString() + " must implement OnArtistSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        Log.d("HomePageFragment", "Listener detached")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
