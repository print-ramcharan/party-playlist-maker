package com.example.partyplaylist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.partyplaylist.R
import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.Track
import com.squareup.picasso.Picasso

class SearchAdapterToAdd(
    private val searchResults: MutableList<Any>,
    private val selectedTracks: MutableList<Track>
) : RecyclerView.Adapter<SearchAdapterToAdd.SearchViewHolder>() {

    private lateinit var listener: SongSelectionListener


    interface SongSelectionListener {
        fun onSongSelected(track: Track)
        fun onSongDeselected(track: Track)
    }


    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val songTitle = view.findViewById<TextView>(R.id.songTitle)
        val songArtist = view.findViewById<TextView>(R.id.songArtist)
        val songAlbum = view.findViewById<TextView>(R.id.songAlbum)
        val albumImage = view.findViewById<ImageView>(R.id.albumImage)
        val selectionIndicator = view.findViewById<View>(R.id.selectionIndicator)
    }


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

            holder.itemView.setOnClickListener {
                if (selectedTracks.contains(item)) {

                    selectedTracks.remove(item)
                    listener.onSongDeselected(item)
                } else {

                    selectedTracks.add(item)
                    listener.onSongSelected(item)
                }

                holder.selectionIndicator.visibility = if (selectedTracks.contains(item)) View.VISIBLE else View.GONE
            }

            holder.songTitle.text = item.name
            holder.songArtist.text = item.artists?.joinToString(", ") { it.name } ?: "Unknown Artist"
            holder.songAlbum.text = item.album?.name ?: "Unknown Album"
            Picasso.get().load(item.album?.images?.firstOrNull()?.url).into(holder.albumImage)


            holder.selectionIndicator.visibility = if (selectedTracks.contains(item)) View.VISIBLE else View.GONE
        } else if (item is Album) {

            holder.songTitle.text = item.name
            holder.songArtist.text = item.artists?.joinToString(", ") { it.name } ?: "Unknown Artist"
            holder.songAlbum.text = "Album"
            Picasso.get().load(item.images?.firstOrNull()?.url).into(holder.albumImage)
            holder.selectionIndicator.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return searchResults.size
    }


    fun updateList(newList: List<Any>) {
        searchResults.clear()
        searchResults.addAll(newList)
        notifyDataSetChanged()
    }
}
