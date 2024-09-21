package com.example.partyplaylist.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.utils.SharedPreferencesManager

@Suppress("RedundantWith")
class SpotifySyncService : Service() {

    private lateinit var spotifyDataService: SpotifyDataService
    private lateinit var firebaseRepository: FirebaseRepository

    override fun onCreate() {
        super.onCreate()
        spotifyDataService = SpotifyDataService(this)
        firebaseRepository = FirebaseRepository()
        Log.d("SpotifySyncService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SpotifySyncService", "Service started")
        fetchAndSaveData()
        return START_NOT_STICKY
    }

    private fun fetchAndSaveData() { // Replace with actual artist ID or fetch dynamically

        Log.d("SpotifySyncService", "Fetching user profile")
        spotifyDataService.fetchUserProfile { user ->
            if (user != null) {
                Log.d("SpotifySyncService", "User profile fetched: $user")
                firebaseRepository.saveUser(user)
                SharedPreferencesManager.saveUserProfile(this,user)
            } else {
                Log.e("SpotifySyncService", "Failed to fetch user profile")
            }
        }

        Log.d("SpotifySyncService", "Fetching top artists")
        spotifyDataService.fetchTopArtists { response ->
            response?.artists?.let { artists ->
                Log.d("SpotifySyncService", "Top artists fetched: ${artists.size} artists")

                // Save top artists to Firebase
                firebaseRepository.saveArtists(artists)

                // Fetch and save top tracks for each artist
                fetchAndSaveTopTracksForArtists(artists)
            } ?: Log.e("SpotifySyncService", "Failed to fetch top artists")
        }

        Log.d("SpotifySyncService", "Fetching top tracks")
        spotifyDataService.fetchTopTracks { response ->
            response?.tracks?.let {
                Log.d("SpotifySyncService", "Top tracks fetched: ${it.size} tracks")
                firebaseRepository.saveTracks(it)
            } ?: Log.e("SpotifySyncService", "Failed to fetch top tracks")
        }

        Log.d("SpotifySyncService", "Fetching playlists")
        spotifyDataService.fetchPlaylists { response ->
            response?.playlist?.let {
                Log.d("SpotifySyncService", "Playlists fetched: ${it.size} playlists")
                firebaseRepository.savePlaylists(it)
            } ?: Log.e("SpotifySyncService", "Failed to fetch playlists")
        }

        Log.d("SpotifySyncService", "Fetching liked songs")
        spotifyDataService.fetchLikedSongs { likedSongs ->
            likedSongs?.trackIds?.forEach { track ->
                Log.d("SpotifySyncService", "Saving liked song: $track")
                firebaseRepository.addLikedSong("USER_ID", track) // Replace "USER_ID" with actual user ID logic
            } ?: Log.e("SpotifySyncService", "Failed to fetch liked songs")
        }
        Log.d("SpotifySyncService", "Fetching saved albums")
        spotifyDataService.fetchUserSavedAlbums { albums ->
            albums?.let {
                Log.d("SpotifySyncService", "Saved albums fetched: ${it.size} albums")
                Log.d("SpotifySyncService", "Saved albums fetched: ${albums} albums")
                firebaseRepository.saveAlbums((it))
            } ?: Log.e("SpotifySyncService", "Failed to fetch saved albums")
        }
//        fetchAndSaveUserAlbums()

    }


    private fun fetchAndSaveTopTracksForArtists(artists: List<Artist>) {
        for (artist in artists) {
            Log.d("SpotifySyncService", "Fetching top tracks for artist ${artist}")
            spotifyDataService.fetchArtistTopTracks(artist.id) { tracks ->
                if (tracks != null) {
                    Log.d("SpotifySyncService", "Top tracks for artist ${artist.name} fetched: ${tracks.size} tracks")
                    firebaseRepository.saveArtistTopTracks(artist.id, tracks)
                } else {
                    Log.e("SpotifySyncService", "Failed to fetch top tracks for artist ${artist.name}")
                }
            }
        }
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SpotifySyncService", "Service destroyed")
    }
}
