package com.example.partyplaylist.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class TopTracksResponse(
    @SerializedName("items") val items: List<Track> = emptyList()
)

data class SpotifyTrack(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("artists") val artists: List<Artist> = listOf(),
    @SerializedName("album") val album: Album = Album(),
    @SerializedName("duration_ms") val durationMs: Int = 0,
    @SerializedName("popularity") val popularity: Int = 0
)
data class Album (
    @SerializedName("album_type") val albumType: String = "",
    @SerializedName("artists") val artists: List<Artist> = listOf(),
    @SerializedName("available_markets") val availableMarkets: List<String> = emptyList(),
    @SerializedName("external_urls") val externalUrls: ExternalUrls = ExternalUrls(),
    @SerializedName("href") val href: String = "",
    @SerializedName("id") val id: String = "",
    @SerializedName("images") val images: List<Image> = listOf(),
    @SerializedName("name") val name: String = "",
    @SerializedName("release_date") val releaseDate: String = "",
    @SerializedName("release_date_precision") val releaseDatePrecision: String = "",
    @SerializedName("total_tracks") val totalTracks: Int = 0,
    @SerializedName("tracks") var tracks: TopTracksResponse = TopTracksResponse(),
    @SerializedName("type") val type: String = "",
    @SerializedName("preview_url") val uri: String = ""
):Parcelable {
    constructor(parcel: Parcel) : this() {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(albumType)
        parcel.writeString(availableMarkets.toString())
        parcel.writeString(href)
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(releaseDate)
        parcel.writeString(releaseDatePrecision)
        parcel.writeInt(totalTracks)
        parcel.writeString(type)
        parcel.writeString(uri)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Album> {
        override fun createFromParcel(parcel: Parcel): Album {
            return Album(parcel)
        }

        override fun newArray(size: Int): Array<Album?> {
            return arrayOfNulls(size)
        }
    }
}

data class AlbumsResponse(
    @SerializedName("albums") val albums: List<SavedAlbum>
)
data class SavedAlbum(
    val album: Album
)
data class TrackList(
    @SerializedName("items") val items: List<Track> = listOf()
)