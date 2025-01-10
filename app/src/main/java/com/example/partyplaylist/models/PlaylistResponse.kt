package com.example.partyplaylist.models

import com.example.partyplaylist.data.User
import com.google.gson.annotations.SerializedName

data class PlaylistResponse(
    val id: String,
    val name: String,
    val description: String,
    val externalUrls: ExternalUrls,
    val href: String,
    val images: List<Image>,
    val owner: User,
    val public: Boolean,
    val snapshot_id: String,
    val tracks: PlaylistTracks,
    val type: String,
    val uri: String,
    val items: List<Playlist>
) {
    val playlists: List<Playlist> = emptyList()
}
data class PlaylistResponse2(
    val items: List<Playlist>
)

data class PlaylistRequest(
    val name: String,
    val description: String,
    val public: Boolean,
    val collaborative: Boolean
)
data class PlaylistCreateRequest(
    val name: String,
    val description: String,
    val public: Boolean
)
