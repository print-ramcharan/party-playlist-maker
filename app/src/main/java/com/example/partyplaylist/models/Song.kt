package com.example.partyplaylist.models

data class Song(
    val id: String = "",
    val name: String = "",
    val artists: List<Artist> = listOf(),
    val album: Album = Album(),
    val durationMs: Int = 0,
    val popularity: Int = 0
)