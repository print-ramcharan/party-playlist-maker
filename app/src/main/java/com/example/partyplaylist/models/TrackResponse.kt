package com.example.partyplaylist.models
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName


data class TrackResponse(
    @SerializedName("items") val tracks: List<Track>
)

