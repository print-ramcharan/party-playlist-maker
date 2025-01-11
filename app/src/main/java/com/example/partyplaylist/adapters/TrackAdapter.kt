package com.example.partyplaylist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.partyplaylist.R
import com.example.partyplaylist.models.Track

class TrackAdapter(private val tracks: List<Track>) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    // ViewHolder class to hold the views for each item
    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val trackImage: ImageView = itemView.findViewById(R.id.trackImage)
        val trackTitle: TextView = itemView.findViewById(R.id.trackTitle)
        val trackArtist: TextView = itemView.findViewById(R.id.trackArtist)
    }

    // Inflating the layout for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_item_layout, parent, false)
        return TrackViewHolder(view)
    }

    // Binding data to the views
    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]

        // Set track data to the views
        holder.trackTitle.text = track.name
        holder.trackArtist.text = track.artists?.joinToString(", ") { it.name }
        Glide.with(holder.itemView).load(track.album!!.images[0].url).into(holder.trackImage)
//        holder.trackImage.setImageResource(track.albumArtUrl)
    }

    // Returning the number of items in the list
    override fun getItemCount(): Int {
        return tracks.size
    }
}
