package com.example.partyplaylist.models

import com.google.gson.annotations.SerializedName

data class ExternalIds(
    @SerializedName("isrc") val isrc: String?, // International Standard Recording Code
    @SerializedName("ean") val ean: String?,   // European Article Number (if available)
    @SerializedName("upc") val upc: String?    // Universal Product Code (if available)
){
    constructor() : this(isrc = null, ean = null, upc = null)
}