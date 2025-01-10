package com.example.partyplaylist.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.partyplaylist.R
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.models.Album
import com.squareup.picasso.Picasso

class SearchAdapterToAdd(
    private val searchResults: MutableList<Any>,
    private val selectedTracks: MutableList<Track> // Tracks selected by the user
) : RecyclerView.Adapter<SearchAdapterToAdd.SearchViewHolder>() {

    private lateinit var listener: SongSelectionListener

    // Interface for communicating with the parent fragment
    interface SongSelectionListener {
        fun onSongSelected(track: Track)
        fun onSongDeselected(track: Track)
    }

    // ViewHolder class to hold the views for each item
    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Bind views directly
        val songTitle = view.findViewById<TextView>(R.id.songTitle)
        val songArtist = view.findViewById<TextView>(R.id.songArtist)
        val songAlbum = view.findViewById<TextView>(R.id.songAlbum)
        val albumImage = view.findViewById<ImageView>(R.id.albumImage)
        val selectionIndicator = view.findViewById<View>(R.id.selectionIndicator) // A view to indicate selection (e.g., checkmark)
    }

    // Set the listener from the parent fragment
    fun setListener(listener: SongSelectionListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_item, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val item = searchResults[position]

        if (item is Track) {
            // Handle Track
            holder.itemView.setOnClickListener {
                if (selectedTracks.contains(item)) {
                    // Deselect the track
                    selectedTracks.remove(item)
                    listener.onSongDeselected(item)
                } else {
                    // Select the track
                    selectedTracks.add(item)
                    listener.onSongSelected(item)
                }
                // Update UI based on selection status
                holder.selectionIndicator.visibility = if (selectedTracks.contains(item)) View.VISIBLE else View.GONE
            }

            holder.songTitle.text = item.name
            holder.songArtist.text = item.artists?.joinToString(", ") { it.name } ?: "Unknown Artist"
            holder.songAlbum.text = item.album?.name ?: "Unknown Album"
            Picasso.get().load(item.album?.images?.firstOrNull()?.url).into(holder.albumImage)

            // Show selection indicator if the track is selected
            holder.selectionIndicator.visibility = if (selectedTracks.contains(item)) View.VISIBLE else View.GONE
        } else if (item is Album) {
            // Handle Album (optional for now)
            holder.songTitle.text = item.name
            holder.songArtist.text = item.artists?.joinToString(", ") { it.name } ?: "Unknown Artist"
            holder.songAlbum.text = "Album"
            Picasso.get().load(item.images?.firstOrNull()?.url).into(holder.albumImage)
            holder.selectionIndicator.visibility = View.GONE // No selection indicator for albums
        }
    }

    override fun getItemCount(): Int {
        return searchResults.size
    }

    // Method to update the list of items
    fun updateList(newList: List<Any>) {
        searchResults.clear()
        searchResults.addAll(newList)
        notifyDataSetChanged() // Notify adapter about the data change
    }
}
