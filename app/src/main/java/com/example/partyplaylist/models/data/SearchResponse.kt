package com.example.partyplaylist.models.data

import AlbumResponse
import com.example.partyplaylist.models.ArtistResponse
import com.example.partyplaylist.models.TrackResponse

data class SearchResponse(
    val albums: AlbumResponse,
    val artists: ArtistResponse,
    val tracks: TrackResponse
)

//data class AlbumResponse(
//    val items: List<Album>
//)
//
//data class ArtistResponse(
//    val items: List<Artist>
//)
//
//data class TrackResponse(
//    val tracks: List<Track>
//)
//
//data class Album(
//    val id: String,
//    val name: String,
//    val artists: List<Artist>,
//    val images: List<Image>
//)
//
//data class Artist(
//    val id: String,
//    val name: String
//)
//
//data class Track(
//    val id: String,
//    val name: String,
//    val previewUrl: String?,
//    val album: Album?,
//    val artists: List<Artist>?
//)
//
//data class Song(
//    val title: String,
//    val previewUrl: String?,
//    val artist: String,
//    val album: String,
//    val image: String
//)
//
//data class Image(
//    val url: String
//)
