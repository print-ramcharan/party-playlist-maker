package com.example.partyplaylist

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.data.Song
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.services.MediaPlayerService
import com.google.gson.Gson

class ArtistDetailsFragment : Fragment() {

    companion object {
        private const val ARG_ARTIST_NAME = "artist_name"

        @JvmStatic
        fun newInstance(artistName: String?): ArtistDetailsFragment {
            val fragment = ArtistDetailsFragment()
            val args = Bundle()
            args.putString(ARG_ARTIST_NAME, artistName)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var artistName: String
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var songRecyclerView: RecyclerView
    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var songAdapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "ArtistDetailsFragment onCreateView")
        val view = inflater.inflate(R.layout.fragment_artist_details, container, false)

        // Initialize Firebase repository
        firebaseRepository = FirebaseRepository()

        // Retrieve the artist name from the arguments
        artistName = arguments?.getString(ARG_ARTIST_NAME) ?: ""

        // Set up RecyclerView for artist details
        recyclerView = view.findViewById(R.id.artistRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        artistAdapter = ArtistAdapter()
        recyclerView.adapter = artistAdapter

        // Set up RecyclerView for songs
        songRecyclerView = view.findViewById(R.id.songRecyclerView)
        songRecyclerView.layoutManager = LinearLayoutManager(context)
        songAdapter = SongAdapter()
        songRecyclerView.adapter = songAdapter

        songAdapter.setOnItemClickListener { song ->
            playSong(song)
        }
        // Fetch artist details and update the RecyclerViews
        fetchArtistDetails()

        return view
    }
    private fun playSong(song: Song) {
        val intent = Intent(requireContext(), MediaPlayerService::class.java).apply {
            action = "PLAY"
            putExtra("SONG_URL", song.previewUrl) // Adjust as needed for the actual song URL
            putExtra("SONG_TITLE", song.name)
        }
        requireContext().startService(intent)
    }
    private fun fetchArtistDetails() {
        // Fetch artist details from Firebase
        firebaseRepository.getArtistDetails(artistName) { artist ->
            artist?.let { artist ->
                // Convert artist object to JSON string for debugging
                val gson = Gson()
                val artistJson = gson.toJson(artist)

                // Log the whole JSON response
                Log.d(TAG, "Artist Response: $artistJson")

                // Print individual fields from the response
                Log.d(TAG, "Artist Name: ${artist.name}")
                Log.d(TAG, "Artist Image URL: ${artist.images.firstOrNull()?.url}")
                Log.d(TAG, "Artist Genres: ${artist.genres.joinToString()}")
                Log.d(TAG, "Artist Popularity: ${artist.popularity}")
                Log.d(TAG, "Artist Followers: ${artist.followers?.total}")

                // Log all the songs associated with the artist
                artist.Songs?.let { songs ->
                    for (song in songs) {
                        Log.d(TAG, "Song Title: ${song.name}")
                        Log.d(TAG, "Song Album: ${song.album.name}") // Assuming 'album' has a 'name' field
                        Log.d(TAG, "Song Duration: ${song.durationMs}")
                        // Add more fields if needed
                    }

                    // Update the adapters with the fetched data
                    artistAdapter.updateArtist(artist)
                    songAdapter.updateSongs(artist.Songs)
                }
            } ?: run {
                Log.d(TAG, "Artist not found")
            }
        }
    }

    private inner class ArtistAdapter : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

        private var artist: Artist? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_artist, parent, false)
            return ArtistViewHolder(view)
        }

        override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
            artist?.let { holder.bind(it) }
        }

        override fun getItemCount(): Int = if (artist != null) 1 else 0

        fun updateArtist(newArtist: Artist) {
            artist = newArtist
            notifyDataSetChanged()
        }

        inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val artistImageView: ImageView = itemView.findViewById(R.id.itemArtistImage)
            private val artistNameTextView: TextView = itemView.findViewById(R.id.itemArtistName)

            fun bind(artist: Artist) {
                // Load image and text into the ViewHolder
                Glide.with(itemView.context).load(artist.images.firstOrNull()?.url).into(artistImageView)
                artistNameTextView.text = artist.name
            }
        }
    }

    private inner class SongAdapter : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

        private var songs: List<Song> = emptyList()
        private var onItemClickListener :((Song) -> Unit)? = null
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_song, parent, false)
            return SongViewHolder(view)
        }

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            songs.getOrNull(position)?.let { holder.bind(it) }
        }

        override fun getItemCount(): Int = songs.size

        fun updateSongs(newSongs: List<Song>) {
            songs = newSongs
            notifyDataSetChanged()
        }
        fun setOnItemClickListener(listener: (Song) -> Unit) {
            onItemClickListener = listener
        }

        inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val songImageView: ImageView = itemView.findViewById(R.id.itemSongImage)
            private val songTitleTextView: TextView = itemView.findViewById(R.id.itemSongTitle)

            fun bind(song: Song) {
                // Load image and text into the ViewHolder
                Glide.with(itemView.context).load(song.album.images.firstOrNull()?.url).into(songImageView)
                songTitleTextView.text = song.name

                itemView.setOnClickListener{
                    onItemClickListener?.invoke(song)
                }
            }
        }
    }
}
