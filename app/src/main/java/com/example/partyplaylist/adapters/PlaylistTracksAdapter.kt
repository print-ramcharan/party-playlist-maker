package com.example.partyplaylist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.partyplaylist.R
import com.example.partyplaylist.models.PlaylistTrack

class PlaylistTracksAdapter(
    private val tracks: List<PlaylistTrack>,
    private val onVoteClicked: (PlaylistTrack) -> Unit
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

        fun bind(track: PlaylistTrack) {
            trackName.text = track.track.name
            trackArtist.text = track.track.artists?.joinToString(", ") { it.name }
            voteCount.text = track.voteCount.toString()

            voteButton.setOnClickListener {
                onVoteClicked(track)
            }
        }
    }
}
