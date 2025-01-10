package com.example.partyplaylist.models

import com.example.partyplaylist.data.Song
import com.example.partyplaylist.data.User
import com.google.firebase.ktx.Firebase
import com.google.gson.annotations.SerializedName

data class Playlist(
    @SerializedName("collaborative") val collaborative: Boolean = true,
    @SerializedName("description") val description: String? = "",
    @SerializedName("external_urls") val externalUrls: ExternalUrls = ExternalUrls(),
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
    @SerializedName("collaborators") var collaborators: List<User> = emptyList(),
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
//@Serializable
//data class PlaylistTracks(
//    @SerializedName("total") val total: Int = 0,
//    @SerializedName("items") var items: List<PlaylistTrack> = emptyList()
//)

data class PlaylistTracks(
    @SerializedName("total") val total: Int = 0,
    @SerializedName("items") var items: List<PlaylistTrack> = emptyList()
)


data class PlaylistTrack(
    @SerializedName("track") val track: Track = Track(),
    @SerializedName("voteCount") var voteCount: Int = 0,
    @SerializedName("added_by") val added_by: AddedBy = AddedBy(),
    @SerializedName("addedCount") val addedCount: Int = 0,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis(),
    @SerializedName("voters") var voters: MutableList<String> = mutableListOf() // Store as List for Firebase
){

    // Convert MutableList<User> to List<String> (User IDs) before saving to Firebase
//    fun toFirebaseVoters(): List<String> {
//        return voters.map { it.id } // Extract the 'id' from each User object
//    }
//
//    // Convert List<String> (User IDs) back to MutableList<User> after reading from Firebase
//    fun fromFirebaseVoters(firebaseVoters: List<String>, usersList: List<User>) {
//        voters = usersList.filter { it.id in firebaseVoters }.toMutableList()
//    }


//     Convert MutableSet to List before saving to Firebase
    fun toFirebaseVoters(): List<String> {
        return voters.toList()
    }

//     Convert List back to MutableSet after reading from Firebase
    fun fromFirebaseVoters(firebaseVoters: List<String>) {
        voters = firebaseVoters.toMutableList() // Convert List back to MutableList (from Firebase)
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
