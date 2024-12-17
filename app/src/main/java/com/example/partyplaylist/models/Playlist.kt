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
    @SerializedName("tracks") val tracks: PlaylistTracks = PlaylistTracks(),
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

data class PlaylistTracks(
    @SerializedName("items") val items: List<PlaylistTrack> = emptyList()
)

data class PlaylistTrack(
    @SerializedName("track") val track: Track = Track(),
    @SerializedName("voteCount") var voteCount: Int = 0,
    @SerializedName("addedBy") val addedBy: User = User(),
    @SerializedName("addedCount") val addedCount: Int = 0,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis()
)

data class TracksResponse(
    @SerializedName("total") val total: Int
)




data class User(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("profilePicture") val profilePicture: String? = null
)
