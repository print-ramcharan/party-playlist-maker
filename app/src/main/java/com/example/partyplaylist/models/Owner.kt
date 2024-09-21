package com.example.partyplaylist.models

data class Owner(
    val href: String,
    val id: String,
    val type: String,
    val uri: String,
    val display_name: String?,
    val external_urls: Map<String, String>
)