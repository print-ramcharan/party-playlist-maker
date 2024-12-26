package com.example.partyplaylist.adapters



import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.partyplaylist.R
import com.example.partyplaylist.models.PlaylistTrack
import com.example.partyplaylist.utils.SharedPreferencesManager.getUserId


class PlaylistTracksAdapter(
    private val tracks: List<PlaylistTrack>,
    private val onVoteClicked: (PlaylistTrack) -> Unit,
) : RecyclerView.Adapter<PlaylistTracksAdapter.PlaylistTrackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistTrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.playlist_track_item, parent, false)
        return PlaylistTrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistTrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track)
    }

    override fun getItemCount(): Int = tracks.size

    inner class PlaylistTrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val trackName: TextView = itemView.findViewById(R.id.track_name)
        private val trackArtist: TextView = itemView.findViewById(R.id.track_artist)
        private val voteButton: Button = itemView.findViewById(R.id.vote_button)
        private val voteCount: TextView = itemView.findViewById(R.id.vote_count)
        private val image: ImageView = itemView.findViewById(R.id.track_image)



        fun bind(track: PlaylistTrack) {
            // Set track name and artist names
            trackName.text = track.track.name
            trackArtist.text = track.track.artists?.joinToString(", ") { it.name }

            // Set vote count
            voteCount.text = track.voteCount.toString()

            // Get the image URL from the track's album
            val imageUrl = track.track.album?.images?.firstOrNull()?.url

            // Log the image URL for debugging
            Log.d("TrackBind", "Image URL: $imageUrl")

            // Check if image URL is null or empty and load the appropriate image
            if (imageUrl.isNullOrEmpty()) {
                Log.d("TrackBind", "Image URL is null or empty, loading default image.")
                Glide.with(itemView.context)
                    .load(R.drawable.ic_music_note) // Default placeholder image
                    .into(image)
            } else {
                Log.d("TrackBind", "Loading image from URL: $imageUrl")
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_music_note) // Placeholder while loading
                    .error(R.drawable.ic_music_note) // Fallback image if loading fails
                    .into(image)
            }



            voteButton.setOnClickListener {
                onVoteClicked(track)
            }


        }
    }
}
