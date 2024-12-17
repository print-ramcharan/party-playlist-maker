package com.example.partyplaylist.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.partyplaylist.R
import com.example.partyplaylist.models.data.Song
import com.squareup.picasso.Picasso

class SearchAdapter(private val searchResults: MutableList<Song>) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    // ViewHolder class to hold the views for each item
    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Bind views directly
        val songTitle = view.findViewById<TextView>(R.id.songTitle)
        val songArtist = view.findViewById<TextView>(R.id.songArtist)
        val songAlbum = view.findViewById<TextView>(R.id.songAlbum)
        val albumImage = view.findViewById<ImageView>(R.id.albumImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_item, parent, false)
        Log.d("SearchAdapter", "Created a new ViewHolder for item at position $viewType")
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val song = searchResults[position]
        Log.d("SearchAdapter", "Binding data for item at position $position: ${song.title}, ${song.artist}, ${song.album}")

        // Set data
        holder.songTitle.text = song.title
        holder.songArtist.text = song.artist
        holder.songAlbum.text = song.album

        // Use Picasso to load the image if URL is available
        Log.d("SearchAdapter", "Loading image for song: ${song.title} from URL: ${song.image}")
        Picasso.get().load(song.image).into(holder.albumImage)
    }

    override fun getItemCount(): Int {
        Log.d("SearchAdapter", "Item count: ${searchResults.size}")
        return searchResults.size
    }

    // Method to update the list of songs
    fun updateList(newList: List<Song>) {
        Log.d("SearchAdapter", "Updating list with ${newList.size} new items")
        searchResults.clear()
        searchResults.addAll(newList)
        notifyDataSetChanged() // Notify adapter about the data change
    }
}
