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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.partyplaylist.adapters.AlbumAdapter
import com.example.partyplaylist.databinding.FragmentHomePageBinding
import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.repositories.FirebaseRepository

class HomePageFragment : Fragment() {

    interface OnArtistSelectedListener {
        fun onArtistSelected(artistName: String?)
        fun onAlbumSelected(albumId: String?)
    }

    private var listener: OnArtistSelectedListener? = null

    // Firebase Repository
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var albumRecyclerView: RecyclerView

    private val albums = mutableListOf<Album>()
    private lateinit var albumAdapter: AlbumAdapter

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

        // Initialize RecyclerView for albums
        albumRecyclerView = binding.albumRecyclerview
        albumRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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

        // Fetch and display artists
        fetchAndDisplayArtists()

        // Fetch and display albums
        fetchAndDisplayAlbums()

        // Setup click listeners
        setupClickListeners()

        return view
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
        }
    }

    private fun fetchAndDisplayArtists() {
        firebaseRepository.getAllArtists { artists ->
            // Assuming you want to display the first eight artists
            val artistList = artists?.take(8)
            if (artistList != null) {
                imageUrls.clear()
                artistNames.clear()

                for (artist in artistList) {
                    imageUrls.add(artist.images.firstOrNull()?.url)
                    artistNames.add(artist.name)
                }

                // Display images and names
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
            } else {
                // Handle cases where there are fewer than 8 artists
                // Optionally, display placeholder images or texts
            }
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

    private fun openDetailsPage(artistName: String?) {
        Log.d("HomePageFragment", "openDetailsPage called with artistName: $artistName")
        listener?.onArtistSelected(artistName)
    }

    private fun openAlbumPage(albumId: String?) {
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
