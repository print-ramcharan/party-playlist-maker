package com.example.partyplaylist.adapters

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.partyplaylist.R
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.PlaylistTrack
import com.example.partyplaylist.utils.SharedPreferencesManager.getAccessToken
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class PlaylistTracksAdapter(
    private var tracks: List<PlaylistTrack>,
    private val onVoteClicked: (PlaylistTrack) -> Unit
) : RecyclerView.Adapter<PlaylistTracksAdapter.PlaylistTrackViewHolder>() {

    private val userCache = mutableMapOf<String, User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistTrackViewHolder {
        Log.d("AdapterLifecycle", "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.playlist_track_item, parent, false)
        return PlaylistTrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistTrackViewHolder, position: Int) {
        Log.d("AdapterLifecycle", "onBindViewHolder called for position: $position")
        tracks = tracks.sortedByDescending { it.voteCount }
        val track = tracks[position]
        holder.bind(track)
//        val track = tracks[position]
//        holder.bind(track)
    }

    override fun getItemCount(): Int {
        Log.d("AdapterLifecycle", "getItemCount: ${tracks.size}")
        return tracks.size
    }
    fun updatePlaylistTracks(newTracks: List<PlaylistTrack>) {
        tracks = newTracks.sortedByDescending {it.voteCount  }
        notifyDataSetChanged()
    }

    inner class PlaylistTrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val voter1Image: ImageView = itemView.findViewById(R.id.voter1_image)
        private val voter2Image: ImageView = itemView.findViewById(R.id.voter2_image)
        private val trackName: TextView = itemView.findViewById(R.id.track_name)
        private val trackArtist: TextView = itemView.findViewById(R.id.track_artist)
        private val voteButton: Button = itemView.findViewById(R.id.vote_button)
//        private val voteCount: TextView = itemView.findViewById(R.id.vote_count)
        private val image: ImageView = itemView.findViewById(R.id.track_image)
        private val addedByImage: ImageView = itemView.findViewById(R.id.added_by_image)

        fun bind(track: PlaylistTrack) {
            Log.d("BindTrack", "Binding track: ${track.track.name}")


            trackName.text = track.track.name
            trackArtist.text = track.track.artists?.joinToString(", ") { it.name }


            Log.d("VotersInfo", "Voters: ${track.voters}")


            voteButton.text = track.voteCount.toString()+"\nVote"

            // Load album image
            val imageUrl = track.track.album?.images?.firstOrNull()?.url
            loadTrackImage(imageUrl)


            val addedByImageUrl = track.track.added_by.images?.firstOrNull()?.url
//            loadAddedByImage(addedByImageUrl)
            addedByImage.visibility = View.GONE

            voteButton.setOnClickListener {
                Log.d("VoteButton", "Vote button clicked for track: ${track.track.name}")
                onVoteClicked(track)
            }


            CoroutineScope(Dispatchers.Main).launch {
                Log.d("track voters ",track.voters.toString() )
                Log.d("track voters ",track.voteCount.toString() )
                updateVoterImages(track.voters, getAccessToken(itemView.context))
            }
        }

        private fun loadTrackImage(imageUrl: String?) {
            Log.d("TrackImage", "Image URL: $imageUrl")
            if (imageUrl.isNullOrEmpty()) {
                Log.d("TrackImage", "Loading default image")
                Glide.with(itemView.context)
                    .load(R.drawable.ic_music_note)
                    .into(image)
            } else {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_music_note_black)
                    .error(R.drawable.ic_music_note)
                    .into(image)
                Log.d("TrackImage", "Loading image from URL")

            }
        }

        private fun loadAddedByImage(addedByImageUrl: String?) {
            Log.d("AddedByImage", "Added by image URL: $addedByImageUrl")
            if (addedByImageUrl.isNullOrEmpty()) {
                Log.d("AddedByImage", "Loading default added by image")
                Glide.with(itemView.context)
                    .load(R.drawable.ic_person)
                    .into(addedByImage)
            } else {
                Glide.with(itemView.context)
                    .load(addedByImageUrl)
                    .override(30, 30)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(addedByImage)
                Log.d("AddedByImage", "Loaded added by image from URL")
            }
        }

        private suspend fun updateVoterImages(votedBy: List<String>, accessToken: String?) {
            Log.d("VoterImages", "Updating voter images for: $votedBy")

            voter1Image.visibility = View.GONE
            voter2Image.visibility = View.GONE

            val voterImages = listOf(voter1Image, voter2Image)

            for ((index, userId) in votedBy.take(2).withIndex()) {
                val user = fetchUserData(userId, accessToken)
                val imageUrl = user.images?.firstOrNull()?.url

                withContext(Dispatchers.Main) {
                    if (imageUrl != null) {
                        Log.d("VoterImageLoad", "Loading voter image for $userId from URL: $imageUrl")
                        Glide.with(itemView.context)
                            .load(imageUrl)
                            .override(40, 40)
                            .circleCrop()
                            .into(voterImages[index])
                        voterImages[index].visibility = View.VISIBLE
                    } else {
                        Log.d("VoterImageLoad", "No image URL found for voter: $userId")
                    }
                }
            }

            if (votedBy.size > 2) {
                voter2Image.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(R.drawable.ic_add)
                    .into(voter2Image)
                voter2Image.setOnClickListener {
                    showVoterPopup(votedBy)
                }
                Log.d("VoterImageLoad", "More than 2 voters, showing '+' icon")
            }
        }

        private suspend fun fetchUserData(userId: String, accessToken: String?): User {
            Log.d("FetchUserData", "Fetching data for user: $userId")

            userCache[userId]?.let {
                Log.d("FetchUserData", "User found in cache: $userId")
                return it
            }

            val request = Request.Builder()
                .url("https://api.spotify.com/v1/users/$userId")
                .header("Authorization", "Bearer $accessToken")
                .build()

            val client = OkHttpClient()
            return withContext(Dispatchers.IO) {
                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val jsonResponse = response.body?.string()
                        Log.d("FetchUserData", "API Response: $jsonResponse")
                        if (jsonResponse != null) {
                            val user = Gson().fromJson(jsonResponse, User::class.java)
                            userCache[userId] = user
                            return@withContext user
                        }
                    } else {
                        Log.e("FetchUserData", "Failed to fetch user data: ${response.code}")
                    }
                } catch (e: IOException) {
                    Log.e("FetchUserData", "Exception: ${e.localizedMessage}")
                }
                User()
            }
        }

        private fun showVoterPopup(votedBy: List<String>) {
            Log.d("VoterPopup", "Showing voter popup for: $votedBy")

            val dialog = AlertDialog.Builder(itemView.context)
            val layout = LinearLayout(itemView.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

            CoroutineScope(Dispatchers.Main).launch {
                for (userId in votedBy) {
                    val user = fetchUserData(userId, getAccessToken(itemView.context))
                    val textView = TextView(itemView.context).apply {
                        text = user.displayName ?: "Unknown User"
                        setPadding(8, 8, 8, 8)
                    }
                    layout.addView(textView)
                }

                dialog.setView(layout)
                dialog.setPositiveButton("Close") { d, _ -> d.dismiss() }
                dialog.show()
            }
        }
    }
}
