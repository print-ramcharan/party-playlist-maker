package com.example.partyplaylist.models
import com.google.gson.annotations.SerializedName


data class TrackResponse(
    @SerializedName("items") val tracks: List<Track>
)

