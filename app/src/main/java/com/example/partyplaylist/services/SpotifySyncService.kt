package com.example.partyplaylist.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.repositories.FirebaseRepository
import com.example.partyplaylist.utils.SharedPreferencesManager
import com.example.partyplaylist.utils.SharedPreferencesManager.getUserId

@Suppress("RedundantWith")
class SpotifySyncService : Service() {

    private lateinit var spotifyDataService: SpotifyDataService
    private lateinit var firebaseRepository: FirebaseRepository

    override fun onCreate() {
        super.onCreate()
        spotifyDataService = SpotifyDataService(this)
        firebaseRepository = FirebaseRepository(this)
        Log.d("SpotifySyncService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SpotifySyncService", "Service started")
//       firebaseRepository.saveUserData()
        fetchAndSaveData()
//        fetchAndSaveData2()
        return START_NOT_STICKY
    }

    private fun fetchAndSaveData() {
        Log.d("SpotifySyncService", "Fetching user profile")
        spotifyDataService.fetchUserProfile { user ->
            if (user != null) {
                Log.d("SpotifySyncService", "User profile fetched: $user")
//                firebaseRepository.saveUserProfile(user)
                SharedPreferencesManager.saveUserProfile(this, user)

                // Proceed with fetching other data after user profile is saved
//                firebaseRepository.saveUserData(user.id)
                fetchAndSaveTopArtists(user.id)
                fetchAndSaveTopTracks(user.id)
                fetchAndSavePlaylists(user.id)
                fetchAndSaveLikedSongs(user.id)
                fetchAndSaveSavedAlbums(user.id)

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
            } else {
                Log.e("SpotifySyncService", "Failed to fetch user profile")
            }
        }
    }


    private fun fetchAndSaveTopArtists(userId: String) {
        Log.d("SpotifySyncService", "Fetching top artists")
        spotifyDataService.fetchTopArtists { response ->
            response?.artists?.let { artists ->
                Log.d("SpotifySyncService", "Top artists fetched: ${artists.size} artists")

                // Save top artists to Firebase
                firebaseRepository.saveArtists(userId, artists)

                // Fetch and save top tracks for each artist
                fetchAndSaveTopTracksForArtists( artists)
               for(artist in artists) {
                   fetchAndSaveArtistTopTracks(artist.id)
               }
            } ?: Log.e("SpotifySyncService", "Failed to fetch top artists")
        }
    }

    private fun fetchAndSaveTopTracks(userId: String) {
        Log.d("SpotifySyncService", "Fetching top tracks")
        spotifyDataService.fetchTopTracks { response ->
            response?.items?.let { tracks ->
                Log.d("SpotifySyncService", "Top tracks fetched: ${tracks.size} tracks")
                firebaseRepository.saveTracks(userId, tracks)
            } ?: Log.e("SpotifySyncService", "Failed to fetch top tracks")
        }
    }


//    private fun fetchAndSavePlaylists(userId: String) {
//        Log.d("SpotifySyncService", "Fetching playlists")
//        spotifyDataService.fetchPlaylists { response ->
//            response?.items?.let { playlists ->
//                Log.d("SpotifySyncService", "Playlists fetched: ${playlists.size} playlists")
//                playlists.forEach { playlist ->
//                    Log.d("SpotifySyncService", "Saving playlist: ${playlist.name}, Tracks: ${playlist.tracks.items.size}")
//                }
//
//                firebaseRepository.savePlaylists(userId, playlists)
//            } ?: Log.e("SpotifySyncService", "Failed to fetch playlists")
//        }
//    }
private fun fetchAndSavePlaylists(userId: String) {
    Log.d("SpotifySyncService", "Fetching playlists")
    spotifyDataService.fetchPlaylists { response ->
        response?.items?.let { playlists ->
            Log.d("SpotifySyncService", "Playlists fetched: ${playlists.size} playlists")

            // Filter playlists where the user is the owner or a listed collaborator

            val filteredPlaylists = playlists.filter { playlist ->
                val isOwner = playlist.owner?.id == userId
                val isCollaborative = playlist.collaborative
                val isCollaborator = playlist.collaborators?.any { collaborator ->
                    collaborator.id == userId
                } ?: false

                // Keep only the ones where the user is the owner or a collaborator
                isOwner || ( isCollaborator)
            }

            filteredPlaylists.forEach { playlist ->
                Log.d("SpotifySyncService", "Saving playlist: ${playlist.name}, Tracks: ${playlist.tracks.items.size}")
            }

            // Save only filtered playlists
            firebaseRepository.savePlaylists(userId, filteredPlaylists)
        } ?: Log.e("SpotifySyncService", "Failed to fetch playlists")
    }
}


    private fun fetchAndSaveLikedSongs(userId: String) {
        Log.d("SpotifySyncService", "Fetching liked songs")
        spotifyDataService.fetchLikedSongs { likedSongs ->
            likedSongs?.trackIds?.forEach { trackId ->
                Log.d("SpotifySyncService", "Saving liked song: $trackId")
                firebaseRepository.addLikedSong(userId, trackId)
            } ?: Log.e("SpotifySyncService", "Failed to fetch liked songs")
        }
    }

    private fun fetchAndSaveSavedAlbums(userId: String) {
        Log.d("SpotifySyncService", "Fetching saved albums")
        spotifyDataService.fetchUserSavedAlbums { albums ->
            albums?.let {
                Log.d("SpotifySyncService", "Saved albums fetched: ${it.size} albums")
                firebaseRepository.saveAlbums(userId, it)
            } ?: Log.e("SpotifySyncService", "Failed to fetch saved albums")
        }
    }
    private fun fetchAndSaveData2() { // Replace with actual artist ID or fetch dynamically

//        Log.d("SpotifySyncService", "Fetching user profile")
//        spotifyDataService.fetchUserProfile { user ->
//            if (user != null) {
//                Log.d("SpotifySyncService", "User profile fetched: $user")
//                firebaseRepository.saveUser(user)
//                SharedPreferencesManager.saveUserProfile(this, user)
//            } else {
//                Log.e("SpotifySyncService", "Failed to fetch user profile")
//            }
//        }

//        Log.d("SpotifySyncService", "Fetching top artists")
//        spotifyDataService.fetchTopArtists { response ->
//            response?.artists?.let { artists ->
//                Log.d("SpotifySyncService", "Top artists fetched: ${artists.size} artists")
//
//                // Save top artists to Firebase
//                firebaseRepository.saveArtists(artists)
//
//                // Fetch and save top tracks for each artist
//                fetchAndSaveTopTracksForArtists(artists)
//            } ?: Log.e("SpotifySyncService", "Failed to fetch top artists")
//        }
//        Log.d("SpotifySyncService", "Fetching top tracks")
//        spotifyDataService.fetchTopTracks { response ->
//            response?.items?.let {
//                Log.d("SpotifySyncService", "Top tracks fetched: ${it.size} tracks")
//                firebaseRepository.saveTracks(it)
//            } ?: Log.e("SpotifySyncService", "Failed to fetch top tracks")
//        }

        Log.d("SpotifySyncService", "Fetching playlists")
        spotifyDataService.fetchPlaylists { response ->
            response?.items?.let {
                Log.d("SpotifySyncService", "Playlists fetched: ${it.size} playlists")
                firebaseRepository.savePlaylists(it)
            } ?: Log.e("SpotifySyncService", "Failed to fetch playlists")
        }

        Log.d("SpotifySyncService", "Fetching liked songs")
        spotifyDataService.fetchLikedSongs { likedSongs ->
            likedSongs?.trackIds?.forEach { track ->
                Log.d("SpotifySyncService", "Saving liked song: $track")
                firebaseRepository.addLikedSong(
                    userId = getUserId(this).toString(),
                    track
                ) // Replace "USER_ID" with actual user ID logic
            } ?: Log.e("SpotifySyncService", "Failed to fetch liked songs")
        }
//        Log.d("SpotifySyncService", "Fetching saved albums")
//        spotifyDataService.fetchUserSavedAlbums { albums ->
//            albums?.let {
//                Log.d("SpotifySyncService", "Saved albums fetched: ${it.size} albums")
//                Log.d("SpotifySyncService", "Saved albums fetched: ${albums} albums")
//                firebaseRepository.saveAlbums((it))
//            } ?: Log.e("SpotifySyncService", "Failed to fetch saved albums")
//        }
    }
    private fun fetchAndSaveArtistTopTracks( artistId: String) {
        Log.d("SpotifySyncService", "Fetching top tracks for artist: $artistId")
        spotifyDataService.fetchArtistTopTracks(artistId) { tracks ->
            tracks?.let {
                Log.d("SpotifySyncService", "Top tracks fetched: ${it.size} tracks")
                firebaseRepository.saveArtistTopTracks(artistId, it) // Ensure you have a method for saving top tracks
            } ?: Log.e("SpotifySyncService", "Failed to fetch top tracks for artist: $artistId")
        }
    }

    private fun fetchAndSaveTopTracksForArtists(artists: List<Artist>) {
        for (artist in artists) {
            Log.d("SpotifySyncService", "Fetching top tracks for artist ${artist}")
            spotifyDataService.fetchArtistTopTracks(artist.id) { tracks ->
                if (tracks != null) {
                    Log.d(
                        "SpotifySyncService",
                        "Top tracks for artist ${artist.name} fetched: ${tracks.size} tracks"
                    )
                    firebaseRepository.saveArtistTopTracks(artist.id, tracks)
                } else {
                    Log.e(
                        "SpotifySyncService",
                        "Failed to fetch top tracks for artist ${artist.name}"
                    )
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