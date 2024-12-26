package com.example.partyplaylist.repositories

import android.content.Context
import android.util.Log
import com.example.partyplaylist.data.Song
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.models.Image
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.PlaylistTrack
import com.example.partyplaylist.models.PlaylistTracks
import com.example.partyplaylist.models.SpotifyTrack
import com.example.partyplaylist.models.TopTracksResponse
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.utils.SharedPreferencesManager
import com.example.partyplaylist.utils.SharedPreferencesManager.getUserId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener


class FirebaseRepository(private val context: Context) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
//    private val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
//    private val authid: String? = currentUser?.uid

    private val userId: String? = getUserId(context);

    // Reference for the current user's node in the database
    private val userRef: DatabaseReference = database.getReference("users").child(userId ?: "")

    // All other references are now children of the userRef
    private val userdataRef : DatabaseReference = userRef.child("userId")

    private val playlistsRef: DatabaseReference = userRef.child("playlists")
    private val tracksRef: DatabaseReference = userRef.child("tracks")
    private val albumsRef: DatabaseReference = database.getReference("users").child(userId?:"").child("albums")
    private val likedSongsRef: DatabaseReference = userRef.child("liked_songs")
    private val artistsRef: DatabaseReference = userRef.child("artists")

//    private fun getUserId(): String? {
//        val sharedPreferences = context.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
//        return sharedPreferences.getString("user_id", null) // Adjust key as needed
//        Log.d("userid and authid", "User ID retrieved: $userId and Auth ID retrieved: $authId")
//    }

    interface PlaylistsCallback {
        fun onPlaylistsFetched(playlists: List<Playlist?>?)
    }

//    fun saveUserData(userId: String) {
//        userdataRef.child(userId).setValue(true)
//    }
    fun addUser(userId: String, userData: Map<String, Any>) {
        userRef.child(userId).setValue(userData)
            .addOnSuccessListener {
                // Handle success
            }
            .addOnFailureListener {
                // Handle failure
            }
    }

    fun addSongToPlaylist(playlistId: String, songData: Map<String, Any>) {
        playlistsRef.child(playlistId).push().setValue(songData)
            .addOnSuccessListener {
                // Handle success
            }
            .addOnFailureListener {
                // Handle failure
            }
    }

    fun deleteSongFromPlaylist(playlistId: String, songId: String) {
        playlistsRef.child(playlistId).child(songId).removeValue()
            .addOnSuccessListener {
                // Handle success
            }
            .addOnFailureListener {
                // Handle failure
            }
    }


    fun saveTracks(userId: String, tracks: List<Track>?) {
        if (tracks == null) {
            Log.e("FirebaseRepository", "Tracks list is null, cannot save.")
            return // Do not proceed if tracks is null
        }

        val userTracksRef = database.getReference("users").child(userId).child("tracks")

        for (track in tracks) {
            val trackRef = userTracksRef.child(track.id.toString())

            // Fetch the track to check if it exists
            trackRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Track exists, retrieve the existing data
                    val existingTrack = snapshot.getValue(Track::class.java)

                    // Increment the vote count if it exists, otherwise set to 0 and add 1
                    val currentVoteCount = existingTrack?.voteCount ?: 0

                    // Update the track with the incremented vote count
                    trackRef.setValue(existingTrack?.copy(voteCount = currentVoteCount + 1))
                        .addOnSuccessListener {
                            Log.d("FirebaseRepository", "Track updated successfully for user $userId: ${track.id}")
//                            Log.d("auth id ", currentUser.toString()+ authid)
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseRepository", "Failed to update track for user $userId: ${track.id}", e)
                        }

                } else {
                    // Track doesn't exist, add a new one with voteCount starting from 0
                    trackRef.setValue(track.copy(voteCount = 0)) // Starting vote count as 0
                        .addOnSuccessListener {
                            Log.d("FirebaseRepository", "New track saved successfully for user $userId: ${track.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseRepository", "Failed to save new track for user $userId: ${track.id}", e)
                        }
                }
            }.addOnFailureListener { exception ->
                Log.e("FirebaseRepository", "Error fetching track for user $userId: ${track.id}", exception)
            }
        }
    }

    fun saveTracks(tracks: List<Track>) {
        val tracksRef = database.getReference("user").child("tracks")
        for (track in tracks) {
            tracksRef.child(track.id.toString()).setValue(track)
                .addOnSuccessListener {
                    // Handle success
                }
                .addOnFailureListener {
                    // Handle failure
                }
        }
    }

    fun saveAlbums(albums: List<Album>) {
        for (album in albums) {
            database.getReference("users").child(userId.toString()).child("albums").child(album.id).setValue(album)

                .addOnSuccessListener {
                    Log.d("FirebaseRepository", "Album saved successfully: ${album.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseRepository", "Failed to save album: ${album.id}", e)
                }
        }
    }

    fun saveAlbums(userId: String, albums: List<Album>?) {
        if (albums == null) {
            Log.e("FirebaseRepository", "Albums list is null, cannot save.")
            return // Do not proceed if albums is null
        }

        val userAlbumsRef = database.getReference("users").child(userId).child("albums")

        for (album in albums) {
            // Reference to the specific album
            val albumRef = userAlbumsRef.child(album.id)

            // Check if the album already exists
            albumRef.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    // Album exists, retrieve the existing album and its tracks
                    val existingAlbum = dataSnapshot.getValue(Album::class.java)
                    val existingTracks = existingAlbum?.tracks?.items?.map { it.id } ?: emptyList()

                    // Prepare new tracks list without duplicates
                    val newTracks = album.tracks.items.filter { track ->
                        !existingTracks.contains(track.id) // Filter out already existing tracks
                    }

                    // If there are new tracks, update the album
                    if (newTracks.isNotEmpty()) {
                        val updatedAlbum = album.copy(tracks = TopTracksResponse(newTracks))

                        albumRef.setValue(updatedAlbum).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("FirebaseRepository", "Album updated successfully for user $userId: ${album.id}")
                            } else {
                                Log.e("FirebaseRepository", "Failed to update album for user $userId: ${album.id}", task.exception)
                            }
                        }
                    } else {
                        Log.d("FirebaseRepository", "No new tracks to add for album $userId: ${album.id}")
                    }
                } else {
                    // If album doesn't exist, save the new album with all tracks
                    albumRef.setValue(album).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FirebaseRepository", "New album saved successfully for user $userId: ${album.id}")
                        } else {
                            Log.e("FirebaseRepository", "Failed to save new album for user $userId: ${album.id}", task.exception)
                        }
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("FirebaseRepository", "Error checking existing album for user $userId: ${album.id}", exception)
            }
        }
    }


    fun savePlaylists(userId: String, playlists: List<Playlist>?) {
        if (playlists == null) {
            Log.e("FbRepo_SavePlaylists", "Playlists list is null, cannot save.")
            return
        }

        val userPlaylistsRef = database.getReference("users").child(userId).child("playlists") // Reference to user's playlists
        for (playlist in playlists) {
            if (playlist.id.isNullOrEmpty()) {
                Log.e(
                    "FbRepo_SavePlaylists",
                    "Playlist ID is null or empty for playlist: ${playlist.name}"
                )
                continue
            }

            // If tracks are null, handle the case
            if (playlist.tracks.items == null) {
                Log.e("FbRepo_SavePlaylists", "Tracks are null for playlist: ${playlist.name}, cannot save.")
                continue
            }

            // Log the playlist snapshot and track details
            Log.d("FbRepo_SavePlaylists", "Saving playlist: ${playlist.name}, ID: ${playlist.id}")
            Log.d("FbRepo_SavePlaylists", "Playlist data snapshot: $playlist")

            // Log track details in the playlist
            playlist.tracks.items.forEachIndexed { index, track ->
                Log.d("FbRepo_SavePlaylists", "Track $index: ${track.track.name}, Preview URL: ${track.track.previewUrl}")
            }

            // Reference to the specific playlist
            val playlistRef = userPlaylistsRef.child(playlist.id)

            // Check if the playlist exists
            playlistRef.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    // Playlist exists, update it
                    playlistRef.setValue(playlist).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FbRepo_SavePlaylists", "Playlist ${playlist.name} updated successfully for user $userId")
                        } else {
                            Log.e("FbRepo_SavePlaylists", "Failed to update playlist ${playlist.name} for user $userId", task.exception)
                        }
                    }
                } else {
                    // Playlist doesn't exist, save it with empty tracks if tracks are null
                    if (playlist.tracks.items.isNullOrEmpty()) {
                        Log.w("FbRepo_SavePlaylists", "Tracks are empty for playlist ${playlist.name}, saving with empty tracks.")
                    }
                    // Save the new playlist (even if tracks are empty)
                    playlistRef.setValue(playlist).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FbRepo_SavePlaylists", "New playlist ${playlist.name} saved successfully for user $userId")
                        } else {
                            Log.e("FbRepo_SavePlaylists", "Failed to save new playlist ${playlist.name} for user $userId", task.exception)
                        }
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("FbRepo_SavePlaylists", "Error checking existing playlist for user $userId: ${playlist.id}", exception)
            }
        }
    }



    fun savePlaylists(playlists: List<Playlist>) {
        Log.d("SavePlaylists", "Saving playlists, count: ${playlists.size}")

        for (playlist in playlists) {
            Log.d("SavePlaylists", "Processing playlist: ${playlist.name}, ID: ${playlist.id}")

            if (playlist.id.isNullOrEmpty()) {
                Log.e("SavePlaylists", "Playlist ID is null or empty for playlist: ${playlist.name}")
                continue // Skip saving this playlist
            }

            // Log the playlist details and snapshot data to see the content
            Log.d("SavePlaylists", "Playlist data snapshot: $playlist")
            Log.d("SavePlaylists", "Tracks in playlist: ${playlist.tracks.items.size} tracks")

            // Log the tracks for debugging
            playlist.tracks.items.forEachIndexed { index, track ->
                Log.d("SavePlaylists", "Track $index: ${track.track.name}, Preview URL: ${track.track.previewUrl}")
            }

            // Reference to the playlist in Firebase
            val playlistRef = playlistsRef.child(playlist.id)

            // Check if playlist exists
            playlistRef.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    // Playlist exists, we update it
                    Log.d("SavePlaylists", "Playlist ${playlist.name} exists, updating.")

                    // Check if tracks are null or empty
                    if (playlist.tracks.items.isNullOrEmpty()) {
                        Log.w("SavePlaylists", "Tracks are empty for playlist ${playlist.name}, updating with empty tracks.")
                    }

                    // Update playlist with new data
                    playlistRef.setValue(playlist).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("SavePlaylists", "Playlist ${playlist.name} updated successfully")
                        } else {
                            Log.e("SavePlaylists", "Failed to update playlist ${playlist.name}", task.exception)
                        }
                    }
                } else {
                    // Playlist doesn't exist, save it as new
                    Log.d("SavePlaylists", "Playlist ${playlist.name} does not exist, saving new.")

                    // Handle case where tracks are null or empty
                    if (playlist.tracks.items.isNullOrEmpty()) {
                        Log.w("SavePlaylists", "Tracks are empty for playlist ${playlist.name}, saving with empty tracks.")
                    }

                    // Save new playlist with tracks
                    playlistRef.setValue(playlist).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("SavePlaylists", "New playlist ${playlist.name} saved successfully")
                        } else {
                            Log.e("SavePlaylists", "Failed to save new playlist ${playlist.name}", task.exception)
                        }
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("SavePlaylists", "Error checking existing playlist for ${playlist.id}", exception)
            }
        }
    }

    fun savePlaylist(playlist: Playlist) {
        // Log user ID to ensure it's being retrieved
        Log.d("SavePlaylist", "Saving playlist for user. User ID: $userId")

        if (userId == null) {
            Log.e("SavePlaylist", "User ID is null, cannot save playlist.")
            return
        }

        if (playlist.id.isNullOrEmpty()) {
            Log.e(
                "SavePlaylist",
                "Playlist ID is null or empty for playlist: ${playlist.name}"
            )
            return
        }

        // Log the playlist details and snapshot data to see the content
        Log.d("SavePlaylist", "Saving playlist: ${playlist.name}, Data: $playlist")

        // Log tracks for this particular playlist
        Log.d("SavePlaylist", "Tracks in playlist: ${playlist.tracks.items.size} tracks")
        playlist.tracks.items.forEachIndexed { index, playlistTrack ->
            Log.d("SavePlaylist", "Track $index: ${playlistTrack.track.name}, Preview URL: ${playlistTrack.track.previewUrl}")
        }

        val userPlaylistsRef = database.getReference("users").child(userId).child("playlists")
        // Reference to the specific playlist node
        val playlistRef = userPlaylistsRef.child(playlist.id)

        // Check if playlist already exists
        playlistRef.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                // Playlist exists, check for tracks and update accordingly
                Log.d("SavePlaylist", "Playlist exists, updating tracks...")

                // Get existing playlist data
                val existingPlaylist = dataSnapshot.getValue(Playlist::class.java)

                // Proceed only if tracks exist in the new playlist
                if (!playlist.tracks.items.isNullOrEmpty()) {
                    // Combine existing tracks with new ones, avoiding duplicates
                    val updatedTracks = existingPlaylist?.tracks?.items?.toMutableList() ?: mutableListOf()
                    for (playlistTrack in playlist.tracks.items) {
                        if (!updatedTracks.contains(playlistTrack)) {
                            updatedTracks.add(playlistTrack)
                        }
                    }

                    // Update the playlist with the new track list
                    val updatedPlaylist = playlist.copy(tracks = PlaylistTracks(updatedTracks.size,updatedTracks))

                    // Update the playlist in the database
                    playlistRef.setValue(updatedPlaylist).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("SavePlaylist", "Playlist updated successfully for user $userId")
                        } else {
                            Log.e("SavePlaylist", "Failed to update playlist for user $userId", task.exception)
                        }
                    }
                }
            } else {
                // Playlist doesn't exist, save a new playlist
                Log.d("SavePlaylist", "Playlist doesn't exist, saving new playlist...")

                // Save the new playlist to Firebase
                playlistRef.setValue(playlist).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("SavePlaylist", "New playlist saved successfully for user $userId")
                    } else {
                        Log.e("SavePlaylist", "Failed to save new playlist for user $userId", task.exception)
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("SavePlaylist", "Error checking playlist for user $userId: ${playlist.id}", exception)
        }
    }


    fun saveUser(user: User) {
        userRef.child(user.id).setValue(user)
            .addOnSuccessListener {
                // Handle success
                Log.d("FirebaseRepository", "User saved successfully")
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e("FirebaseRepository", "Error saving user", e)
            }

    }

    fun addLikedSong(userId: String, trackId: String) {
        if (userId.isNullOrEmpty()) {
            Log.e("AddLikedSong", "User ID is null or empty, cannot add liked song.")
            return
        }

        if (trackId.isNullOrEmpty()) {
            Log.e("AddLikedSong", "Track ID is null or empty, cannot add liked song.")
            return
        }

        // Reference to the user's liked songs in the database
        val likedSongsRef = database.getReference("users").child(userId).child("liked_songs")

        // Log for debugging
        Log.d("AddLikedSong", "Adding track $trackId to user $userId liked songs")

        // Save the track ID as a liked song with a value of true
        likedSongsRef.child(trackId).setValue(true)
            .addOnSuccessListener {
                Log.d("AddLikedSong", "Track $trackId successfully added to liked songs for user $userId.")
            }
            .addOnFailureListener { exception ->
                Log.e("AddLikedSong", "Failed to add track $trackId to liked songs for user $userId", exception)
            }
    }


    fun removeLikedSong(userId: String, trackId: String) {
        likedSongsRef.child(userId).child(trackId).removeValue()
            .addOnSuccessListener {
                // Handle success
            }
            .addOnFailureListener {
                // Handle failure
            }
    }


    fun getTrack(trackId: String, callback: (Track?) -> Unit) {
        tracksRef.child(trackId).get()
            .addOnSuccessListener { dataSnapshot ->
                val track = dataSnapshot.getValue(Track::class.java)
                callback(track)
            }
            .addOnFailureListener {
                // Handle failure
                callback(null)
            }
    }


    fun getAlbum(albumId: String, callback: (Album?) -> Unit) {
        // Check if albumId is null or empty
        if (albumId.isNullOrEmpty()) {
            Log.e("getAlbum", "Album ID is null or empty, cannot fetch album.")
            callback(null)
            return
        }

        // Log the start of the method
        Log.d("getAlbum", "Fetching album with ID: $albumId")

        albumsRef.child(albumId).get()
            .addOnSuccessListener { dataSnapshot ->
                // Log the success of the database query
                Log.d("getAlbum", "Successfully fetched data for album ID: $albumId")

                // Check if the dataSnapshot is valid
                if (dataSnapshot.exists()) {
                    val album = dataSnapshot.getValue(Album::class.java)

                    // Log the album object obtained
                    Log.d("getAlbum", "Album: $album")

                    // Pass the album to the callback
                    callback(album)
                } else {
                    Log.e("getAlbum", "No album data found for ID: $albumId")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                // Log the failure
                Log.e("getAlbum", "Failed to fetch album with ID: $albumId", exception)

                // Call the callback with null in case of failure
                callback(null)
            }
    }


    fun getPlaylist(playlistId: String, callback: (Playlist?) -> Unit) {
        playlistsRef.child(playlistId).get()
            .addOnSuccessListener { dataSnapshot ->
                val playlist = dataSnapshot.getValue(Playlist::class.java)
                callback(playlist)
            }
            .addOnFailureListener {
                // Handle failure
                callback(null)
            }
    }

    fun getLikedSongs(userId: String, callback: (Map<String, Boolean>?) -> Unit) {
        likedSongsRef.child(userId).get()
            .addOnSuccessListener { dataSnapshot ->
                val likedSongs = dataSnapshot.value as? Map<String, Boolean>
                callback(likedSongs)
            }
            .addOnFailureListener {
                // Handle failure
                callback(null)
            }
    }

    fun saveArtists(userId: String, artists: List<Artist>) {
        val userArtistsRef = database.getReference("users").child(userId).child("artists")

        for (artist in artists) {
            // Log the artist ID and ensure it's not empty
            Log.d("FirebaseRepository", "Processing artist: ${artist.id} for user: $userId")

            if (artist.id.isNullOrEmpty()) {
                Log.e("FirebaseRepository", "Artist ID is null or empty, skipping artist: ${artist.name}")
                continue
            }

            // Fetch the existing artist data from the database
            userArtistsRef.child(artist.id).get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        // Artist exists, merge the data
                        val existingArtist = dataSnapshot.getValue(Artist::class.java)

                        if (existingArtist != null) {
                            // Merge the existing songs with the new songs, avoiding duplicates
                            val updatedSongs = mergeSongs(existingArtist.Songs, artist.Songs)

                            // Merge the artist data (including songs)
                            val updatedArtist = existingArtist.copy(
                                name = artist.name ?: existingArtist.name,
                                genres = artist.genres.ifEmpty { existingArtist.genres },
                                images = artist.images.ifEmpty { existingArtist.images },
                                popularity = artist.popularity.takeIf { it != 0 } ?: existingArtist.popularity,
                                externalUrls = artist.externalUrls.copy(),
                                followers = artist.followers.copy(),
                                Songs = updatedSongs
                            )

                            // Log the updated artist data
                            Log.d("FirebaseRepository", "Merging artist data: $updatedArtist")

                            // Save the updated artist data
                            userArtistsRef.child(artist.id).setValue(updatedArtist)
                                .addOnSuccessListener {
                                    Log.d(
                                        "FirebaseRepository",
                                        "Artist updated successfully for user $userId: ${artist.id}"
                                    )
                                }
                                .addOnFailureListener { e ->
                                    Log.e(
                                        "FirebaseRepository",
                                        "Failed to update artist for user $userId: ${artist.id}",
                                        e
                                    )
                                }
                        }
                    } else {
                        // Artist doesn't exist, add it as a new artist
                        userArtistsRef.child(artist.id).setValue(artist)
                            .addOnSuccessListener {
                                Log.d(
                                    "FirebaseRepository",
                                    "Artist added successfully for user $userId: ${artist.id}"
                                )
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "FirebaseRepository",
                                    "Failed to add artist for user $userId: ${artist.id}",
                                    e
                                )
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseRepository", "Error checking artist existence: ${artist.id}", e)
                }
        }
    }

    // Helper function to merge the songs list (avoids duplicates)
    fun mergeSongs(existingSongs: List<Song>, newSongs: List<Song>): List<Song> {
        val mergedSongs = mutableListOf<Song>()

        // Add existing songs if not already in the new list
        mergedSongs.addAll(existingSongs)

        // Add only the new songs that are not already in the list
        newSongs.forEach { newSong ->
            if (!existingSongs.any { it.id == newSong.id }) {
                mergedSongs.add(newSong)
            }
        }

        return mergedSongs
    }

    fun saveArtists(artists: List<Artist>) {

        for (artist in artists) {
            // Fetch the existing artist data from the database
            artistsRef.child(artist.id).get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        // Artist exists, merge the data
                        val existingArtist = dataSnapshot.getValue(Artist::class.java)

                        if (existingArtist != null) {
                            // Merge the existing songs with the new songs, avoiding duplicates
                            val updatedSongs = mergeSongs(existingArtist.Songs, artist.Songs)

                            // Merge the artist data (including songs)
                            val updatedArtist = existingArtist.copy(
                                name = artist.name ?: existingArtist.name,
                                genres = artist.genres.ifEmpty { existingArtist.genres },
                                images = artist.images.ifEmpty { existingArtist.images },
                                popularity = artist.popularity.takeIf { it != 0 } ?: existingArtist.popularity,
                                externalUrls = artist.externalUrls.copy(),
                                followers = artist.followers.copy(),
                                Songs = updatedSongs
                            )

                            // Save the updated artist data
                            artistsRef.child(artist.id).setValue(updatedArtist)
                                .addOnSuccessListener {
                                    Log.d("FirebaseRepository", "Artist updated successfully: ${artist.id}")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FirebaseRepository", "Failed to update artist: ${artist.id}", e)
                                }
                        }
                    } else {
                        // Artist doesn't exist, add it as a new artist
                        artistsRef.child(artist.id).setValue(artist)
                            .addOnSuccessListener {
                                Log.d("FirebaseRepository", "Artist added successfully: ${artist.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseRepository", "Failed to add artist: ${artist.id}", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseRepository", "Error checking artist existence: ${artist.id}", e)
                }
        }
    }

    fun getAllPlaylists(callback: PlaylistsCallback) {
        if (userId == null) {
            Log.e("FirebaseRepository", "User ID is null, cannot fetch playlists.")
            callback.onPlaylistsFetched(emptyList())
            return
        }

        val userPlaylistsRef = userRef.child("playlists")
        userPlaylistsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseRepository", "Fetched playlists snapshot: ${snapshot.value}")
                val playlists = mutableListOf<Playlist>()

                snapshot.children.forEach { playlistSnapshot ->
                    try {
                        val playlistId = playlistSnapshot.key ?: return@forEach
                        val playlistName = playlistSnapshot.child("name").getValue(String::class.java) ?: "Unnamed Playlist"
                        val description = playlistSnapshot.child("description").getValue(String::class.java) ?: ""
                        val collaborators = playlistSnapshot.child("collaborators")
                            .children.mapNotNull { it.getValue(User::class.java) }
                        val totalVotes = playlistSnapshot.child("totalVotes").getValue(Int::class.java) ?: 0
                        val lastModified = playlistSnapshot.child("lastModified").getValue(Long::class.java) ?: System.currentTimeMillis()

                        // Fetch the images
                        val images = mutableListOf<Image>()
                        playlistSnapshot.child("images").children.forEach { imageSnapshot ->
                            val url = imageSnapshot.child("url").getValue(String::class.java)
                            val height = imageSnapshot.child("height").getValue(Int::class.java) ?: 0
                            val width = imageSnapshot.child("width").getValue(Int::class.java) ?: 0

                            if (url != null) {
                                val image = Image( height,url, width)
                                images.add(image)
                            }
                        }

                        // Map track data
                        val tracks = mutableListOf<PlaylistTrack>()
                        playlistSnapshot.child("tracks/items").children.forEach { trackSnapshot ->
                            try {
                                val trackId = trackSnapshot.child("track/id").getValue(String::class.java) ?: return@forEach
                                val trackName = trackSnapshot.child("track/name").getValue(String::class.java) ?: "Untitled Track"
                                val artistName = trackSnapshot.child("track/artists/0/name").getValue(String::class.java) ?: "Unknown Artist"
                                val albumName = trackSnapshot.child("track/album/name").getValue(String::class.java) ?: "Unknown Album"
                                val voteCount = trackSnapshot.child("voteCount").getValue(Int::class.java) ?: 0
                                val lastUpdated = trackSnapshot.child("lastUpdated").getValue(Long::class.java) ?: System.currentTimeMillis()

                                val track = Track(
                                    id = trackId,
                                    name = trackName,
                                    artists = listOf(Artist(name = artistName)),
                                    album = Album(name = albumName)
                                )

                                val playlistTrack = PlaylistTrack(
                                    track = track,
                                    voteCount = voteCount,
                                    lastUpdated = lastUpdated,
                                    addedBy = User(),
                                    addedCount = 0,
                                    voters = mutableSetOf()

                                )
                                tracks.add(playlistTrack)
                            } catch (e: Exception) {
                                Log.e("FirebaseRepository", "Error mapping track data", e)
                            }
                        }

                        val playlist = Playlist(
                            id = playlistId,
                            name = playlistName,
                            description = description,
                            tracks = PlaylistTracks(tracks.size, tracks),
                            collaborators = collaborators,
                            totalVotes = totalVotes,
                            lastModified = lastModified,
                            images = images  // Added images field here
                        )
                        playlists.add(playlist)
                    } catch (e: Exception) {
                        Log.e("FirebaseRepository", "Error mapping playlist data", e)
                    }
                }

                Log.d("FirebaseRepository", "Playlists fetched: $playlists")
                callback.onPlaylistsFetched(playlists)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Error fetching playlists", error.toException())
                callback.onPlaylistsFetched(emptyList())
            }
        })
    }



    fun getAlbumTracks(albumId: String, callback: (List<Track>?) -> Unit) {
        val albumTracksRef = FirebaseDatabase.getInstance().getReference("albums/$albumId/tracks/tracks")

        albumTracksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Convert DataSnapshot to a list of Track objects
                val tracks = snapshot.children.mapNotNull { it.getValue(Track::class.java) }
                callback(tracks)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Error fetching album tracks", error.toException())
                callback(null)
            }
        })
    }

    // Fetch all tracks
    fun getAllTracks(callback: (List<SpotifyTrack>?) -> Unit) {
        tracksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tracks = snapshot.children.mapNotNull { it.getValue(SpotifyTrack::class.java) }
                callback(tracks)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Error fetching tracks", error.toException())
                callback(null)
            }
        })
    }

    // Fetch all albums
    fun getAllAlbums(callback: (List<Album>?) -> Unit) {
        albumsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val albums = snapshot.children.mapNotNull { it.getValue(Album::class.java) }
                Log.d("FirebaseRepositoryyy", "Received ${albums.size} albums")

                callback(albums)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Error fetching albums", error.toException())
                callback(null)
            }
        })
    }

    // Fetch all liked songs for a user
    fun getLikedSongsForUser(userId: String, callback: (Map<String, Boolean>?) -> Unit) {
        likedSongsRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likedSongs = snapshot.value as? Map<String, Boolean>
                callback(likedSongs)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "FirebaseRepository",
                    "Error fetching liked songs for user $userId",
                    error.toException()
                )
                callback(null)
            }
        })
    }

    // Fetch all artists
    fun getAllArtists(callback: (List<Artist>?) -> Unit) {
       val userId = SharedPreferencesManager.getUserId(context) // Get current user ID
        if (userId == null) {
            Log.e("FirebaseRepository", "User ID is null, cannot fetch artists.")
            callback(null)
            return
        }

        val userArtistsRef = database.getReference("users").child(userId).child("artists")
        userArtistsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val artists = snapshot.children.mapNotNull { it.getValue(Artist::class.java) }
                callback(artists)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Error fetching artists", error.toException())
                callback(null)
            }
        })
    }

    // Fetch specific user data
    fun getUser(userId: String, callback: (User?) -> Unit) {
        userRef.child(userId).get()
            .addOnSuccessListener { dataSnapshot ->
                val user = dataSnapshot.getValue(User::class.java)
                callback(user)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepository", "Error fetching user $userId", e)
                callback(null)
            }
    }

    fun getArtistDetails(artistName: String, callback: (Artist?) -> Unit) {
        val userId = SharedPreferencesManager.getUserId(context) // Get current user ID
        if (userId == null) {
            Log.e("FirebaseRepository", "User ID is null, cannot fetch artist details.")
            callback(null)
            return
        }

        // Navigate to the user's artists node (users -> userId -> artists -> artistId)
        val userArtistsRef = database.getReference("users").child(userId).child("artists")

        // Fetch all artists under the user
        userArtistsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var foundArtist: Artist? = null

                // Iterate over each artistId node
                for (artistSnapshot in snapshot.children) {
                    // Extract the artist's data
                    val artist = artistSnapshot.getValue(Artist::class.java)

                    // Check if the artist's name matches the input artistName
                    if (artist?.name == artistName) {
                        foundArtist = artist

                        // Fetch songs if available
                        val songsSnapshot = artistSnapshot.child("songs")
                        if (songsSnapshot.exists()) {
                            Log.d("FirebaseRepository", "Songs Snapshot: ${songsSnapshot.value}")
                            val songsList = mutableListOf<Song>()
                            for (songSnapshot in songsSnapshot.children) {
                                try {
                                    val song = songSnapshot.getValue(Song::class.java)
                                    song?.let { songsList.add(it) }
                                } catch (e: Exception) {
                                    Log.e("FirebaseRepository", "Error deserializing song", e)
                                }
                            }
                            artist.Songs = songsList // Assuming your Artist class has a `songs` property
                        } else {
                            Log.d("FirebaseRepository", "No songs found for artist $artistName")
                        }

                        // Callback with the found artist
                        break
                    }
                }

                if (foundArtist != null) {
                    callback(foundArtist)
                } else {
                    Log.e("FirebaseRepository", "No artist found with the name $artistName")
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Error fetching artist details", error.toException())
                callback(null)
            }
        })
    }

    fun saveArtistTopTracks(artistId: String, tracks: List<Track>) {
        val artistTracksRef =
            userId?.let { database.getReference("users").child(it).child("artists").child(artistId).child("songs") }

        // Fetch the existing tracks for the artist
        if (artistTracksRef != null) {
            artistTracksRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    val existingTracks = mutableListOf<Track>()

                    // If data exists, retrieve the existing tracks
                    if (dataSnapshot.exists()) {
                        val currentTracks = dataSnapshot.children.mapNotNull { it.getValue(Track::class.java) }
                        existingTracks.addAll(currentTracks)
                    }

                    // Merge the existing tracks with the new ones, avoiding duplicates based on track id
                    val mergedTracks = mergeTracks(existingTracks, tracks)

                    // Save the merged list of tracks
                    artistTracksRef.setValue(mergedTracks)
                        .addOnSuccessListener {
                            Log.d("FirebaseRepository", "Top tracks for artist $artistId saved successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseRepository", "Failed to save top tracks for artist $artistId", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseRepository", "Failed to fetch existing tracks for artist $artistId", e)
                }
        }
    }

    // Helper function to merge tracks, avoiding duplicates based on track id
    fun mergeTracks(existingTracks: List<Track>, newTracks: List<Track>): List<Track> {
        val mergedTracks = mutableListOf<Track>()

        // Add existing tracks if not already in the new list
        mergedTracks.addAll(existingTracks)

        // Add only the new tracks that are not already in the existing tracks list
        newTracks.forEach { newTrack ->
            if (!existingTracks.any { it.id == newTrack.id }) {
                mergedTracks.add(newTrack)
            }
        }

        return mergedTracks
    }

    fun saveUserData(userId: String, displayName: String?, email: String?) {
        // Create the user object with default values for images and followers
        val user = User(
            id = userId,
            displayName = displayName,
            email = email,
            images = emptyList(), // Default to an empty list of images
            // Default to 0 followers
        )

        // Save the user data to Firebase
        userRef.child(userId).setValue(user)
            .addOnSuccessListener {
                Log.d("FirebaseRepository", "User data saved successfully for user: $userId")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepository", "Error saving user data for user: $userId", e)
            }
    }

    fun getPlaylistById(playlistId: String, callback: (Playlist?) -> Unit) {
        playlistsRef.child(playlistId).get()
            .addOnSuccessListener { dataSnapshot ->
                // Log raw data from Firebase
                Log.d("FirebaseRepository", "DataSnapshot: ${dataSnapshot.value}")

                val playlist = dataSnapshot.getValue(Playlist::class.java)
                callback(playlist)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepository", "Error getting playlist by ID: $playlistId", e)
                callback(null)
            }
    }


    fun saveUserProfile(user: User) {
        userRef.child(user.id).setValue(user)
            .addOnSuccessListener {
                Log.d("FirebaseRepository", "User profile saved successfully: ${user.id}")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepository", "Error saving user profile: ${user.id}", e)
            }
    }




fun saveLikedSongs(userId: String, tracks: List<Track>?) {
    if (tracks == null) {
        Log.e("FirebaseRepository", "Tracks list is null, cannot save liked songs.")
        return
    }

    val userLikedSongsRef = database.getReference("users").child(userId).child("liked_songs")
    for (track in tracks) {
        userLikedSongsRef.child(track.id.toString())
            .setValue(true) // You might want to store the track object instead of just true
            .addOnSuccessListener {
                Log.d(
                    "FirebaseRepository",
                    "Liked song saved successfully for user $userId: ${track.id}"
                )
            }
            .addOnFailureListener { e ->
                Log.e(
                    "FirebaseRepository",
                    "Failed to save liked song for user $userId: ${track.id}",
                    e
                )
            }
    }
}


    fun addTrack(track: Track) {
        val userId = SharedPreferencesManager.getUserId(context) // Get current user ID
        if (userId == null) {
            Log.e("FirebaseRepository", "User ID is null, cannot add track.")
            return
        }

        val trackId = track.id // Assuming your Track class has an 'id' property
        val userTracksRef =
            trackId?.let { database.getReference("users").child(userId).child("tracks").child(it) }

        if (userTracksRef != null) {
            userTracksRef.setValue(track)
                .addOnSuccessListener {
                    Log.d(
                        "FirebaseRepository",
                        "Track ${track.name} added successfully for user $userId"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseRepository", "Error adding track ${track.name} for user $userId", e)
                }
        }
    }

    fun addArtist(artist: Artist) {
        val userId = SharedPreferencesManager.getUserId(context) // Get current user ID
        if (userId == null) {
            Log.e("FirebaseRepository", "User ID is null, cannot add artist.")
            return
        }

        val artistId = artist.id // Assuming your Artist class has an 'id' property
        val userArtistsRef =
            database.getReference("users").child(userId).child("artists").child(artistId)

        userArtistsRef.setValue(artist)
            .addOnSuccessListener {
                Log.d(
                    "FirebaseRepository",
                    "Artist ${artist.name} added successfully for user $userId"
                )
            }
            .addOnFailureListener { e ->
                Log.e(
                    "FirebaseRepository",
                    "Error adding artist ${artist.name} for user $userId",
                    e
                )
            }
    }



    fun updatePlaylistTracks(playlistId: String, tracks: List<PlaylistTrack>, callback: (Boolean) -> Unit) {
        var completedTasks = 0
        var hasErrorOccurred = false

        // Reference to the playlist in Firebase
        val playlistRef = playlistsRef.child(playlistId).child("tracks").child("items")

        // Loop through all tracks and check if they exist in Firebase
        tracks.forEachIndexed { index, track ->
            val trackRef = playlistRef.child(index.toString())

            trackRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val existingTrack = task.result?.getValue(PlaylistTrack::class.java)

                    if (existingTrack != null) {
                        // Track exists, update its vote count (incrementing the existing voteCount)
                        existingTrack.voteCount += track.voteCount  // Increment voteCount
                        trackRef.setValue(existingTrack)  // Update track with new vote count
                    } else {
                        // Track doesn't exist, create it with the initial vote count
                        trackRef.setValue(track)  // Add new track
                    }
                } else {
                    // Handle any errors in fetching the track
                    Log.e("updatePlaylistTracks", "Error fetching track: ${task.exception?.message}")
                    hasErrorOccurred = true
                }

                // Increment the completed task counter
                completedTasks++

                // If all tasks are completed, invoke the callback
                if (completedTasks == tracks.size) {
                    callback(!hasErrorOccurred)
                }
            }
        }
    }

    fun updateTrackVote(playlistId: String, track: PlaylistTrack, callback: (Boolean) -> Unit) {
        playlistsRef.child(playlistId).child("tracks").child("items").get()
            .addOnSuccessListener { snapshot ->
                var found = false
                for (child in snapshot.children) {
                    // Check the track ID
                    val currentTrackId = child.child("track").child("id").value.toString()
                    if (currentTrackId == track.track.id) {
                        // Retrieve current voteCount
                        val currentVoteCount = child.child("voteCount").getValue(Int::class.java) ?: 0
                        val newVoteCount = currentVoteCount + 1

                        // Update the voteCount
                        child.ref.child("voteCount").setValue(newVoteCount)
                            .addOnCompleteListener { task ->
                                callback(task.isSuccessful)
                            }
                        found = true
                        break
                    }
                }
                if (!found) {
                    Log.e("updateTrackVote", "Track with ID ${track.track.id} not found.")
                    callback(false)
                }
            }.addOnFailureListener {
                Log.e("updateTrackVote", "Error fetching tracks: ${it.message}")
                callback(false)
            }
    }


    fun getPlaylistTracks(playlistId: String, callback: (Playlist?) -> Unit) {
        val playlistRef = playlistsRef.child(playlistId)
        playlistRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val playlistTracks = snapshot.getValue(Playlist::class.java)
                callback(playlistTracks)
            } else {
                callback(null)
            }
        }.addOnFailureListener {
            callback(null)
        }
    }


    fun updateTrackVote(playlistId: String, track: PlaylistTrack, userId: String, callback: (Boolean) -> Unit) {
        playlistsRef.child(playlistId).child("tracks").child("items").get()
            .addOnSuccessListener { snapshot ->
                var found = false
                for (child in snapshot.children) {
                    val currentTrackId = child.child("track").child("id").value.toString()
                    if (currentTrackId == track.track.id) {
                        // Retrieve current voteCount
                        val currentVoteCount = child.child("voteCount").getValue(Int::class.java) ?: 0

                        // Use GenericTypeIndicator to safely cast the votedBy field to List<String>
                        val votedByIndicator = object : GenericTypeIndicator<List<String>>() {}
                        val votedBy = child.child("votedBy").getValue(votedByIndicator) ?: listOf<String>()

                        if (votedBy.contains(userId)) {
                            // User has already voted
                            Log.e("updateTrackVote", "User $userId has already voted for this track.")
                            callback(false)
                            return@addOnSuccessListener
                        }

                        // Add the user to the votedBy list and increment the vote count
                        val updatedVotedBy = votedBy + userId  // Immutably add the userId to the list
                        val newVoteCount = currentVoteCount + 1

                        // Update the Firebase entry with the new vote count and updated list of voters
                        val updates = mapOf(
                            "voteCount" to newVoteCount,
                            "votedBy" to updatedVotedBy  // Using the updated immutable list
                        )

                        child.ref.updateChildren(updates).addOnCompleteListener { task ->
                            callback(task.isSuccessful)
                        }

                        found = true
                        break
                    }
                }

                if (!found) {
                    Log.e("updateTrackVote", "Track with ID ${track.track.id} not found.")
                    callback(false)
                }
            }
            .addOnFailureListener {
                Log.e("updateTrackVote", "Error fetching tracks: ${it.message}")
                callback(false)
            }
    }

}