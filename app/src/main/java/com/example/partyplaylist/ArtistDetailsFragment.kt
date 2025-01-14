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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.partyplaylist.data.Song
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.services.MediaPlayerService

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
    private lateinit var songRecyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "ArtistDetailsFragment onCreateView")
        val view = inflater.inflate(R.layout.fragment_artist_details, container, false)
        imageView = view.findViewById(R.id.artist_image)
        textView = view.findViewById(R.id.artist_name)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchArtistDetails()

        }
        // Initialize Firebase repository
        firebaseRepository = FirebaseRepository(requireContext())

        // Retrieve the artist name from the arguments
        artistName = arguments?.getString(ARG_ARTIST_NAME) ?: ""

        // Set up RecyclerView for songs
        songRecyclerView = view.findViewById(R.id.songRecyclerView)
        songRecyclerView.layoutManager = LinearLayoutManager(context)
        songAdapter = SongAdapter()
        songRecyclerView.adapter = songAdapter

        songAdapter.setOnItemClickListener { song ->
            playSong(song)
        }

        // Fetch artist details and update UI
        fetchArtistDetails()

        return view
    }

    private fun playSong(song: Song) {
        val intent = Intent(requireContext(), MediaPlayerService::class.java).apply {
            action = "PLAY"
            putExtra("SONG_URL", song.previewUrl)
            putExtra("SONG_TITLE", song.name)
        }
        requireContext().startService(intent)
    }

    private fun fetchArtistDetails() {
        // Fetch artist details from Firebase
        firebaseRepository.getArtistDetails(artistName) { artist ->
            artist?.let {
                // Update UI with artist details
                textView.text = it.name
                Glide.with(this).load(it.images.firstOrNull()?.url).into(imageView)

                // Update songs in the RecyclerView
                it.Songs?.let { songs ->
                    songAdapter.updateSongs(songs)
                }
            } ?: run {
                Log.d(TAG, "Artist not found")
            }
        }
        swipeRefreshLayout.isRefreshing = false
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


