package com.example.partyplaylist.models

import com.google.gson.annotations.SerializedName

data class Track(
    @SerializedName("album") val album: Album? = null,
    @SerializedName("artists") val artists: List<Artist>? = null,
    @SerializedName("available_markets") val availableMarkets: List<String>? = null,
    @SerializedName("disc_number") val discNumber: Int? = null,
    @SerializedName("duration_ms") val durationMs: Int? = null,
    @SerializedName("explicit") val explicit: Boolean? = null,
    @SerializedName("external_ids") val externalIds: ExternalIds? = null,
    @SerializedName("external_urls") val externalUrls: ExternalUrls? = null,
    @SerializedName("href") val href: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("is_local") val local: Boolean? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("popularity") val popularity: Int? = null,
    @SerializedName("preview_url") val previewUrl: String? = null,
    @SerializedName("track_number") val trackNumber: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("uri") val uri: String? = null,

)