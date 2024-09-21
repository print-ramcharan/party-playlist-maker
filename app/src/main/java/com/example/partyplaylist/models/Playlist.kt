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
    @SerializedName("collaborators") val collaborators: List<User> = emptyList()
)
{

}

data class PlaylistTracks(
    @SerializedName("items") val items: List<PlaylistTrack> = emptyList()
)

data class PlaylistTrack(
    @SerializedName("track") val track: Song = Song(),
    @SerializedName("voteCount") val votecount: Int = 0

)
//data class PlaylistResponse(
//    @SerializedName("items") val playlists: List<Playlist>
//)
data class TracksResponse(
    @SerializedName("total") val total: Int
)