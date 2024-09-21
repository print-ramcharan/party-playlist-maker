package com.example.partyplaylist

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
import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.services.MediaPlayerService

class AlbumTracksFragment : Fragment() {

    companion object {
        private const val ARG_ALBUM_ID = "album_id"

        @JvmStatic
        fun newInstance(albumId: String): AlbumTracksFragment {
            val fragment = AlbumTracksFragment()
            val args = Bundle().apply {
                putString(ARG_ALBUM_ID, albumId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var albumId: String
    private lateinit var albumCoverImageView: ImageView
    private lateinit var albumNameTextView: TextView
    private lateinit var albumArtistTextView: TextView
    private lateinit var tracksRecyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_album_tracks, container, false)

        // Initialize Firebase repository
        firebaseRepository = FirebaseRepository()

        // Retrieve the album ID from the arguments
        albumId = arguments?.getString(ARG_ALBUM_ID).orEmpty()

        // Initialize UI elements
        albumCoverImageView = view.findViewById(R.id.album_cover_image)
        albumNameTextView = view.findViewById(R.id.album_name_text)
        albumArtistTextView = view.findViewById(R.id.album_artist_text)
        tracksRecyclerView = view.findViewById(R.id.tracks_recyclerview)
        tracksRecyclerView.layoutManager = LinearLayoutManager(context)
        trackAdapter = TrackAdapter(mutableListOf())
        tracksRecyclerView.adapter = trackAdapter

        trackAdapter.setOnItemClickListener { track ->
            playTrack(track)
        }
        // Fetch album details and update the UI
        fetchAlbumDetails()

        return view
    }

    private fun playTrack(track : Track){
        val intent = Intent(requireContext(),MediaPlayerService::class.java).apply {
            action = "PLAY"
            putExtra("SONG_URL",track.previewUrl)
            putExtra("SONG_TITLE",track.name)
        }
        requireContext().startService(intent)
    }
    private fun fetchAlbumDetails() {
        firebaseRepository.getAlbum(albumId) { album ->
            if (album != null) {
                Log.d("AlbumTracksFragment", "Fetched album details: $album")
                displayAlbumDetails(album)
            } else {
                Log.e("AlbumTracksFragment", "Failed to fetch album details for ID: $albumId")
            }
        }
    }

    private fun displayAlbumDetails(album: Album) {
        // Display album details in the UI
        Log.d("AlbumTracksFragment", "Displaying album details: $album")
        Glide.with(this)
            .load(album.images.firstOrNull()?.url)
            .into(albumCoverImageView)
        albumNameTextView.text = album.name
        albumArtistTextView.text = album.artists.joinToString(", ") { it.name }

        // Fetch and display tracks
        fetchAndDisplayTracks(album.id)
    }

    private fun fetchAndDisplayTracks(albumId: String) {
        Log.d("AlbumTracksFragment", "Fetching tracks for album ID: $albumId")
        firebaseRepository.getAlbumTracks(albumId) { fetchedTracks ->
            if (fetchedTracks != null) {
                Log.d("AlbumTracksFragment", "Fetched tracks: $fetchedTracks")
                trackAdapter.updateTracks(fetchedTracks)
            } else {
                Log.e("AlbumTracksFragment", "Failed to fetch tracks for album ID: $albumId")
            }
        }
    }

    private inner class TrackAdapter(private val tracks: MutableList<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {
        private var onItemClickListener : ((Track) -> Unit)? = null
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
            return TrackViewHolder(view)
        }

        override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
            val track = tracks[position]
            holder.bind(track)
        }

        override fun getItemCount() = tracks.size

        fun updateTracks(newTracks: List<Track>) {
            tracks.clear()
            tracks.addAll(newTracks)
            notifyDataSetChanged()
        }
        fun setOnItemClickListener(listener: (Track) -> Unit){
            onItemClickListener = listener
        }
        inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val trackImageView: ImageView = itemView.findViewById(R.id.track_image)
            private val trackNameTextView: TextView = itemView.findViewById(R.id.track_title)
            private var artistNameTextView: TextView = itemView.findViewById(R.id.track_duration)

            fun bind(track: Track) {
                trackNameTextView.text = track.name
                track.artists?.joinToString { it.name }.also { artistNameTextView.text = it }
                artistNameTextView.text = track.durationMs?.toString()
                val imageUrl = track.album?.images?.firstOrNull()?.url ?: ""
                if (imageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.spotify_green_icon) // Add a placeholder image
                        .into(trackImageView)
                } else {
                    trackImageView.setImageResource(R.drawable.spotify_green_icon) // Set a default image if URL is null
                }
                //set click listener
                itemView.setOnClickListener{

                    Log.d("TrackViewHolder", "Track clicked: ${track.name}")
                    onItemClickListener?.invoke(track)
                }
            }
        }
    }
}
