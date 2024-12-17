package com.example.partyplaylist.models

data class LikedSongs(

    val trackIds: List<String> // List of track IDs
) {
    val tracks: List<Track> = emptyList()

}
