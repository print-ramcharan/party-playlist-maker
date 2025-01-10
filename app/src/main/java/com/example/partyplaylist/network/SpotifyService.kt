package com.example.partyplaylist.network

import AlbumResponse
import RecentlyPlayedResponse
import com.example.partyplaylist.models.*
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.data.SearchResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotifyService {
    @POST("v1/playlists/{playlistId}/tracks")
    suspend fun addTracksToPlaylist(
        @Header("Authorization") authHeader: String,
        @Path("playlistId") playlistId: String?,
        @Body tracks: Map<String, List<String>>
    ): retrofit2.Response<Unit>

    @GET("v1/me")
    fun getUserProfile(@Header("Authorization") token: String): Call<User>

    @GET("v1/me/playlists")
    fun getPlaylists(@Header("Authorization") token: String): Call<PlaylistResponse>

    @GET("v1/me/top/artists")
    fun getTopArtists(@Header("Authorization") authHeader: String?): Call<TopArtistsResponse>

    @GET("v1/me/top/tracks")
    fun getTopTracks(@Header("Authorization") authHeader: String?): Call<TopTracksResponse>

    @GET("v1/playlists/{playlist_id}/tracks")
    fun getPlaylistTracks(
        @Header("Authorization") token: String,
        @Path("playlist_id") playlistId: String
    ): Call<List<Track>>

    @GET("v1/albums/{id}")
    fun getAlbum(@Path("id") albumId: String, @Header("Authorization") authHeader: String): Call<Album>

    @GET("v1/albums/{id}/tracks")
    fun getAlbumTracks(
        @Path("id") albumId: String,
        @Header("Authorization") authHeader: String
    ): Call<TrackResponse>

    @GET("v1/me/tracks")
    fun getLikedSongs(@Header("Authorization") authHeader: String): Call<LikedSongs>

    @GET("v1/artists/{id}/albums")
    fun getArtistAlbums(@Path("id") artistId: String, @Header("Authorization") token: String): Call<AlbumResponse>

    @GET("v1/artists/{id}/top-tracks")
    fun getArtistTopTracks(
        @Path("id") artistId: String,
        @Header("Authorization") token: String,
        @Query("market") market: String = "US"
    ): Call<TopTracksResponse>

    @GET("v1/me/player/recently-played")
    fun getRecentlyPlayed(@Header("Authorization") authHeader: String): Call<RecentlyPlayedResponse>

    @GET("search")
    suspend fun searchArtists(
        @Query("q") query: String,
        @Query("type") type: String = "artist",
        @Query("limit") limit: Int = 1
    ): SearchResponse
    // Add more endpoints as needed
    @GET("v1/me/albums")
    fun getUserAlbums(@Header("Authorization") token: String): Call<AlbumsResponse>

    @POST("v1/users/{user_id}/playlists")
    fun createPlaylist(
        @Path("user_id") userId: String,
        @Header("Authorization") authHeader: String,
        @Body playlistCreateRequest: PlaylistCreateRequest
    ): Call<PlaylistResponse>
    @GET("v1/me/albums")
    fun getSavedAlbums(
        @Header("Authorization") authHeader: String
    ): Call<List<Album>>
    @GET("v1/search")
    suspend fun searchSpotify(
        @Query("q") query: String,
        @Query("type") type: String = "track,album,artist", // search tracks, albums, and artists
        @Query("limit") limit: Int = 10,
        @Header("Authorization") authHeader: String
    ): retrofit2.Response<SearchResponse>



}

