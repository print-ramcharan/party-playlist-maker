package com.example.partyplaylist.data

import com.example.partyplaylist.models.ExternalUrls
import com.example.partyplaylist.models.Image
import com.example.partyplaylist.models.Followers
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer


data class User(
    @SerializedName("id") val id: String = "",
    @SerializedName("display_name") val displayName: String? = "",
    @SerializedName("email") val email: String? = "",
    @SerializedName("images") val images: List<Image>? = emptyList(),
    @SerializedName("followers") val followers: Followers = Followers(),
    @SerializedName("external_urls") val externalUrls: ExternalUrls = ExternalUrls(),
    @SerializedName("href") val href: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("uri") val uri: String = ""
)
data class Followers(
    @SerializedName("total") val total: Int = 0
)
