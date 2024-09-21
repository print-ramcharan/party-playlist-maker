package com.example.partyplaylist.trail

import com.example.partyplaylist.data.Followers
//import com.example.partyplaylist..Song

data class Album(
    val albumType: String,
    val artists: List<Artist>,
    val availableMarkets: List<String>,
    val externalUrls: ExternalUrls,
    val href: String,
    val id: String,
    val images: List<Image>,
    val name: String,
    val releaseDate: String,
    val releaseDatePrecision: String,
    val totalTracks: Int,
    val tracks: TopTracksResponse,
    val type: String,
    val uri: String
)

data class Artist(
    val id: String,
    val name: String,
    val genres: List<String>,
    val images: List<Image>,
    val popularity: Int,
    val externalUrls: ExternalUrls,
    val followers: Followers,
    val type: String,
    val songs: List<Song>
)

data class ExternalUrls(
    val spotify: String
)

data class Followers(
    val total: Int
)

data class Image(
    val height: Int,
    val url: String,
    val width: Int
)

data class TopTracksResponse(
    val tracks: List<Track>
)

data class Track(
    val album: Album?,
    val artists: List<Artist>,
    val availableMarkets: List<String>,
    val discNumber: Int,
    val durationMs: Int,
    val explicit: Boolean,
    val externalIds: Map<String, String>?,
    val externalUrls: ExternalUrls,
    val href: String,
    val id: String,
    val isLocal: Boolean,
    val name: String,
    val popularity: Int,
    val previewUrl: String?,
    val trackNumber: Int,
    val type: String,
    val uri: String
)

data class Song(
    val id: String,
    val name: String,
    val durationMs: Int,
    val previewUrl: String?
)