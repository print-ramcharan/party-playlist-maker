package com.example.partyplaylist.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.partyplaylist.R
import com.example.partyplaylist.models.Playlist

class PlaylistAdapter(
    private val playlists: MutableList<Playlist>,
    private val onPlaylistClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.bind(playlist)
    }

    override fun getItemCount(): Int = playlists.size

    fun addPlaylist(playlist: Playlist) {
        playlists.add(playlist)
        notifyItemInserted(playlists.size - 1)
    }

    fun clearPlaylists() {
        playlists.clear()
        notifyDataSetChanged()
    }

    fun addPlaylists(newPlaylists: List<Playlist>) {
        playlists.addAll(newPlaylists)
        notifyDataSetChanged()
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playlistNameTextView: TextView =
            itemView.findViewById(R.id.playlist_name_text_view)
        private val playlistImageView: ImageView = itemView.findViewById(R.id.playlist_image)

        fun bind(playlist: Playlist) {
            playlistNameTextView.text = playlist.name
            Log.d("PlaylistAdapter", "Playlist: ${playlist.images} \n ${playlist.name}")


            val imageUrl = playlist.images?.getOrNull(0)?.url
            Log.d("PlaylistAdapter", "Image URL: $imageUrl")


            if (imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_music_note_black)
                    .into(playlistImageView)
            } else {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_music_note_black)
                    .into(playlistImageView)
            }

            itemView.setOnClickListener {
                onPlaylistClick(playlist)
            }


    }
    }
}
