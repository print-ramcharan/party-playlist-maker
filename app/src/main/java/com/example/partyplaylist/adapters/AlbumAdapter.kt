// AlbumAdapter.kt
package com.example.partyplaylist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.partyplaylist.R
import com.example.partyplaylist.models.Album

class AlbumAdapter(private val albums: List<Album>,
                   private val clickListener: (Album) -> Unit) :
    RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_item, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.albumName.text = album.name
        Glide.with(holder.itemView.context)
            .load(album.images.firstOrNull()?.url)
            .into(holder.albumImage)
        holder.itemView.setOnClickListener {
            clickListener(album)
        }
    }

    override fun getItemCount(): Int = albums.size

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumImage: ImageView = itemView.findViewById(R.id.album_image)
        val albumName: TextView = itemView.findViewById(R.id.album_name)
    }
}
