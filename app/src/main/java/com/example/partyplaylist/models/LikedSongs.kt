package com.example.partyplaylist.models

data class LikedSongs(
    val userId: String,
    val trackIds: List<String> // List of track IDs
)
