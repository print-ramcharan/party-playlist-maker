package com.example.partyplaylist.models

import com.example.partyplaylist.data.Song
import com.example.partyplaylist.data.User
import com.google.gson.annotations.SerializedName

data class Playlist(
    @SerializedName("collaborative") val collaborative: Boolean = true,
    @SerializedName("description") val description: String? = "",
    @SerializedName("externalUrls") val externalUrls: ExternalUrls = ExternalUrls(),
    @SerializedName("href") val href: String = "",
    @SerializedName("id") val id: String = "",
    @SerializedName("images") val images: List<Image>? = emptyList(),
    @SerializedName("name") val name: String = "",
    @SerializedName("owner") val owner: User = User(),
    @SerializedName("public") val public: Boolean? = null,
    @SerializedName("snapshotId") val snapshotId: String = "",
    @SerializedName("tracks") var tracks: PlaylistTracks = PlaylistTracks(),
    @SerializedName("type") val type: String = "",
    @SerializedName("uri") val uri: String = "",
    @SerializedName("collaborators") val collaborators: List<User> = emptyList(),
    @SerializedName("totalVotes") val totalVotes: Int = 0,
    @SerializedName("lastModified") val lastModified: Long = System.currentTimeMillis()
) {

    fun getTopRankedTracks(): List<PlaylistTrack> {
        return this.tracks.items.sortedByDescending { it.voteCount }
    }


    fun getRecentlyAddedTracks(): List<PlaylistTrack> {
        return this.tracks.items.sortedByDescending { it.lastUpdated }
    }
}
data class PlaylistTrackss(

    @SerializedName("tracks") var items: List<Track> = emptyList()
)
data class PlaylistTracks(
    @SerializedName("total") val total: Int = 0,
    @SerializedName("items") var items: List<PlaylistTrack> = emptyList()
)

data class PlaylistTrack(
    @SerializedName("track") val track: Track = Track(),
    @SerializedName("voteCount") var voteCount: Int = 0,
    @SerializedName("addedBy") val addedBy: User = User(),
    @SerializedName("addedCount") val addedCount: Int = 0,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis(),
    @SerializedName("votedBy") var voters: MutableSet<String> = mutableSetOf()
) {
    fun isValidTrack(): Boolean {

        return track != null && track.id?.isNotEmpty() ==true
    }
    // Function to check if a user has already voted
    fun hasVoted(userId: String): Boolean {
        return voters.contains(userId)
    }

    // Function to add a vote for a user
    fun vote(userId: String) {
        if (!hasVoted(userId)) {
            // Add the user's ID to the voters set
            voters.add(userId)
            // Increment vote count
            voteCount++
        }
    }
}

data class TracksResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("items") val items: List<TrackItem> // List of track items
)
data class TrackItem(
    @SerializedName("track") val track: Track
)



data class User(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("profilePicture") val profilePicture: String? = null
)
