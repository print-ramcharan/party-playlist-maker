package com.example.partyplaylist.data

import com.example.partyplaylist.models.Image
import com.example.partyplaylist.models.Followers
import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: String = "",
    @SerializedName("display_name") val displayName: String? = "",
    @SerializedName("email") val email: String? = "",
    @SerializedName("images") val images: List<Image>? = emptyList(),
    @SerializedName("followers") val followers: Followers = Followers()
)

data class Followers(
    @SerializedName("total") val total: Int = 0
)
