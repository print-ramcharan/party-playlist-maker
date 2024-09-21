package com.example.partyplaylist.data

import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.Artist
import com.google.gson.annotations.SerializedName

data class Song(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("artists") val artists: List<Artist> = listOf(),
    @SerializedName("album") val album: Album = Album(),
    @SerializedName("duration_ms") val durationMs: Int = 0,
    @SerializedName("popularity") val popularity: Int = 0,
    @SerializedName("previewUrl") val previewUrl: String = "",
    @SerializedName("discNumber") val discNumber: Int = 0
)
