package com.example.partyplaylist.models

import AlbumResponse
import com.example.partyplaylist.data.Song
import com.google.gson.annotations.SerializedName
import org.json.JSONArray
import org.json.JSONObject

data class TopArtistsResponse(
    @SerializedName("items") val artists: List<Artist>
)

data class Artist(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("images") val images: List<Image> = emptyList(),
    @SerializedName("popularity") val popularity: Int = 0,
    @SerializedName("external_urls") val externalUrls: ExternalUrls = ExternalUrls(""),
    @SerializedName("followers") val followers: Followers = Followers(0),
    @SerializedName("type") val type: String = "",
    @SerializedName("Songs") var Songs: List<Song> = emptyList()
) {
    // Default constructor required for Firebase
    constructor(
        id: String,
        name: String,
        genres: List<String>,
        images: List<Image>,
        popularity: Int,
        externalUrls: ExternalUrls,
        followers: com.example.partyplaylist.data.Followers
    ) : this("", "", emptyList(), emptyList(), 0, ExternalUrls(""), Followers(0), "", emptyList())

    constructor(
        id: String,
        name: String,
        genres: List<String>,
        images: List<Image>,
        popularity: Int,
        externalUrls: ExternalUrls,
        followers: com.example.partyplaylist.trail.Followers,
        type: String,
        Songs: Any
    ) : this()

    constructor(
        id: String,
        name: String,
        genres: List<String>,
        images: List<Image>,
        popularity: Int,
        externalUrls: ExternalUrls,
        followers: com.example.partyplaylist.data.Followers,
        type: String,
        Songs: String
    ) : this()
}

data class Image(
    @SerializedName("height") val height: Int = 0,
    @SerializedName("url") val url: String = "",
    @SerializedName("width") val width: Int = 0
)

data class ExternalUrls(
    @SerializedName("spotify") val spotify: String = ""
)

data class Followers(
    @SerializedName("total") val total: Int = 0
)
data class ArtistResponse(
    @SerializedName("items") val items: List<Artist>
)
//data class SearchResponse(
//    @SerializedName("tracks") val tracks: TrackResponse?,
//    @SerializedName("albums") val albums: AlbumResponse?,
//    @SerializedName("artists") val artists: ArtistResponse?
//)



