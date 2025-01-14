package com.example.partyplaylist.repositories

import android.content.Context
import android.util.Log
import com.example.partyplaylist.data.Song
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.AddedBy
import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.models.ExternalUrls
import com.example.partyplaylist.models.Image
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.PlaylistTrack
import com.example.partyplaylist.models.PlaylistTracks
import com.example.partyplaylist.models.TopTracksResponse
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.utils.SharedPreferencesManager
import com.example.partyplaylist.utils.SharedPreferencesManager.getUserId
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
fun sanitizeUserId(userId: String): String {
    return userId.replace(".", "_")
}
fun savePlaylists(userId: String, playlists: List<Playlist>?) {
    if (playlists == null) return

    val userPlaylistsRef = database.getReference("users").child(userId).child("playlists")

    userPlaylistsRef.get().addOnSuccessListener { dataSnapshot ->
        val existingPlaylists = dataSnapshot.children.associateBy { it.key }

        for (playlist in playlists) {
            if (playlist.id.isNullOrEmpty()) continue

            val playlistRef = userPlaylistsRef.child(playlist.id)

            if (existingPlaylists.containsKey(playlist.id)) {
                val existingPlaylistTracks = existingPlaylists[playlist.id]?.child("tracks")?.child("items")?.children?.mapNotNull {
                    it.getValue(PlaylistTrack::class.java)
                }?.toMutableList() ?: mutableListOf()

                val newPlaylistTracks = playlist.tracks.items?.map { it } ?: emptyList()

                if (newPlaylistTracks.isNotEmpty()) {
                    val updatedPlaylistTracks = newPlaylistTracks.map { newTrack ->
                        val existingTrack = existingPlaylistTracks.find { it.track.id == newTrack.track.id }

                        if (existingTrack != null) {
                            existingTrack.copy(
                                voteCount = existingTrack.voteCount ?: 0,
                                added_by = existingTrack.added_by ?: AddedBy(),
                                addedCount = existingTrack.addedCount ?: 0,
                                lastUpdated = existingTrack.lastUpdated ?: System.currentTimeMillis(),
                                voters = existingTrack.voters.takeIf { it.isNotEmpty() } ?: mutableListOf()
                            )
                        } else {
                            PlaylistTrack(
                                track = newTrack.track,
                                voteCount = 0,
                                added_by = newTrack.added_by,
                                addedCount = 0,
                                lastUpdated = System.currentTimeMillis(),
                                voters = mutableListOf()
                            )
                        }
                    }

                    val tracksToAdd = newPlaylistTracks.filter { newTrack ->
                        existingPlaylistTracks.none { it.track.id == newTrack.track.id }
                    }

                    val finalUpdatedTracks = updatedPlaylistTracks + tracksToAdd

                    // Check if the user is the owner or a collaborator
                    val ownerId = playlist.owner.id
                    val collaborators = playlist.collaborators ?: emptyList()

                    if (ownerId == userId) {
                        // User is the owner, update their own playlist with full data
                        playlistRef.child("tracks").child("items").setValue(finalUpdatedTracks)
                    } else if (collaborators.any { it.id == userId }) {
                        // User is a collaborator, update only the track data, not other metadata
                        val collaboratorTrackEntries = finalUpdatedTracks.map { track ->
                            PlaylistTrack(
                                track = track.track,
                                voteCount = 0, // Reset metadata for the collaborator
                                added_by = AddedBy(),
                                addedCount = 0,
                                lastUpdated = System.currentTimeMillis(),
                                voters = mutableListOf()
                            )
                        }

                        // Save only the track data for the collaborator, without metadata
                        playlistRef.child("tracks").child("items").setValue(collaboratorTrackEntries)

                        // Save only metadata in the collaborator's playlist section (playlist ID, owner ID, and collaborators)
                        val collaboratorMetadata = playlist.copy(
                            description = "", // Empty description
                            externalUrls = ExternalUrls(), // Empty externalUrls
                            href = "", // Empty href
                            images = playlist.images, // Empty images
                            name = playlist.name, // Empty name or keep default
                            public = null, // Empty public
                            snapshotId = "", // Empty snapshotId
                            tracks = PlaylistTracks(), // Empty tracks
                            type = "", // Empty type
                            uri = "", // Empty uri
                            totalVotes = 0, // Empty votes
                            lastModified = System.currentTimeMillis(), // Empty lastModified
                            owner = playlist.owner, // Include owner
                            collaborators = playlist.collaborators, // Include collaborators
                            id = playlist.id // Include playlist ID
                        )

                        playlistRef.setValue(collaboratorMetadata)

                        // Save the same for the owner's playlist (keeping the full metadata intact)
                        val ownerPlaylistRef = database.getReference("users").child(ownerId).child("playlists").child(playlist.id)
                        ownerPlaylistRef.child("tracks").child("items").setValue(finalUpdatedTracks)
                    }

                } else {
                    Log.d("FbRepo_SavePlaylists", "No new tracks to add, keeping existing tracks for playlist ${playlist.name}")
                }
            } else {
                // Playlist doesn't exist, save it as a new playlist
                playlistRef.setValue(playlist).addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e("FbRepo_SavePlaylists", "Failed to save new playlist ${playlist.name}")
                    }
                }

                // Save empty track data in the current user's playlist section
                val emptyTrackEntries = playlist.tracks.items?.map {
                    PlaylistTrack(
                        track = it.track,
                        voteCount = 0,
                        added_by = AddedBy(),
                        addedCount = 0,
                        lastUpdated = System.currentTimeMillis(),
                        voters = mutableListOf()
                    )
                } ?: emptyList()

                playlistRef.child("tracks").child("items").setValue(emptyTrackEntries)

                // Save the full playlist (including tracks) in the owner's section
                val ownerId = playlist.owner.id
                Log.d("ownerid path", ownerId)

                val ownerPlaylistRef = database.getReference("users").child(ownerId).child("playlists").child(playlist.id)

                ownerPlaylistRef.setValue(playlist)

                // Save the playlist track data for each collaborator
                val collaborators = playlist.collaborators ?: emptyList()
                for (collaborator in collaborators) {
                    if(userId == collaborator.id) continue
                    val collaboratorPlaylistRef = database.getReference("users").child(collaborator.id).child("playlists").child(playlist.id)
                    collaboratorPlaylistRef.child("tracks").child("items").setValue(emptyTrackEntries)
                }
            }
        }
    }.addOnFailureListener { exception ->
        Log.e("FbRepo_SavePlaylists", "Error fetching existing playlists: ", exception)
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

        val userId = getUserId(context) // Get current user ID
        if (userId == null) {
            Log.e("FirebaseRepository", "User ID is null, cannot fetch album details.")
            callback(null)
            return
        }

        val userAlbumsRef = database.getReference("users").child(userId).child("albums")

        // Log the start of the method
        Log.d("getAlbum", "Fetching album with ID: $albumId")

        userAlbumsRef.child(albumId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
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

            override fun onCancelled(error: DatabaseError) {
                // Log the failure
                Log.e("getAlbum", "Failed to fetch album with ID: $albumId", error.toException())

                // Call the callback with null in case of failure
                callback(null)
            }
        })
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
                        val ownerSnapshot = playlistSnapshot.child("owner")
                        val owner = ownerSnapshot.getValue(User::class.java) ?: User()

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
                                val externalUrls = trackSnapshot.child("added_by/externalUrls").getValue(ExternalUrls::class.java) ?: ExternalUrls()
                                val added_by_track = trackSnapshot.child("track/added_by").getValue(User::class.java) ?: User()

                                val track = Track(
                                    id = trackId,
                                    name = trackName,
                                    added_by = added_by_track,
                                    artists = listOf(Artist(name = artistName)),
                                    album = Album(name = albumName)
                                )
                                val added_by = AddedBy(externalUrls = externalUrls)

                                val playlistTrack = PlaylistTrack(
                                    track = track,
                                    voteCount = voteCount,
                                    lastUpdated = lastUpdated,
                                    added_by = added_by,
                                    addedCount = 0,
                                    voters = mutableListOf()

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
                            images = images,  // Added images field here
                            owner = owner
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
        // Reference to the album's tracks in the Firebase Database
        albumsRef.child(albumId).child("tracks").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Convert the DataSnapshot to TrackResponse
                val trackResponse = snapshot.getValue(TopTracksResponse::class.java)

                // Check if TrackResponse is valid
                if (trackResponse != null) {
                    // Extract the list of tracks from TrackResponse
                    val tracks = trackResponse.items
                    // Return the list of tracks through the callback
                    callback(tracks)
                } else {
                    Log.e("FirebaseRepository", "TrackResponse is null for album ID: $albumId")
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Error fetching album tracks", error.toException())
                callback(null)
            }
        })
    }  // Fetch all tracks

    // Fetch all tracks
    fun getAllTracks(callback: (List<Track>?) -> Unit) {
        tracksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tracks = snapshot.children.mapNotNull { it.getValue(Track::class.java) }
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
            Log.d("snapshotx",snapshot.toString())

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

    fun getPlaylistTracksOwner(playlistId: String,ownerId : String, callback: (Playlist?) -> Unit) {
        Log.d("FirebaseDebug", "Fetching path: /$ownerId/playlists/$playlistId")

        val playlistRef = database.getReference("users").child(ownerId).child("playlists").child(playlistId)
        playlistRef.get().addOnSuccessListener { snapshot ->
            Log.d("snapshotx",snapshot.toString())
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


//    fun updateTrackVote(playlistId: String, track: PlaylistTrack, userId: String, callback: (Boolean) -> Unit) {
//
//        val playlistRef = playlistsRef.child(playlistId)
//
//        // Fetch the owner ID from the playlist reference
//        playlistRef.child("owner").child("id").get().addOnSuccessListener { ownerSnapshot ->
//            val ownerId = ownerSnapshot.value.toString()
//
//            database.getReference("users").child(ownerId).child(playlistId).child("tracks")
//                .child("items").get()
//                .addOnSuccessListener { snapshot ->
//                    var found = false
//                    for (child in snapshot.children) {
//                        val currentTrackId = child.child("track").child("id").value.toString()
//                        if (currentTrackId == track.track.id) {
//                            // Retrieve current voteCount
//                            val currentVoteCount =
//                                child.child("voteCount").getValue(Int::class.java) ?: 0
//
//                            // Use GenericTypeIndicator to safely cast the votedBy field to List<String>
//                            val votedByIndicator = object : GenericTypeIndicator<List<String>>() {}
//                            val votedBy =
//                                child.child("voters").getValue(votedByIndicator) ?: listOf<String>()
//
//                            if (votedBy.contains(userId)) {
//                                // User has already voted
//                                Log.e(
//                                    "updateTrackVote",
//                                    "User $userId has already voted for this track."
//                                )
//                                callback(false)
//                                return@addOnSuccessListener
//                            }
//
//                            // Add the user to the votedBy list and increment the vote count
//                            val updatedVotedBy =
//                                votedBy + userId  // Immutably add the userId to the list
//                            val newVoteCount = currentVoteCount + 1
//
//                            // Update the Firebase entry with the new vote count and updated list of voters
//                            val updates = mapOf(
//                                "voteCount" to newVoteCount,
//                                "voters" to updatedVotedBy  // Using the updated immutable list
//                            )
//
//                            child.ref.updateChildren(updates).addOnCompleteListener { task ->
//                                callback(task.isSuccessful)
//                            }
//
//                            found = true
//                            break
//                        }
//                    }
//
//                    if (!found) {
//                        Log.e("updateTrackVote", "Track with ID ${track.track.id} not found.")
//                        callback(false)
//                    }
//                }
//                .addOnFailureListener {
//                    Log.e("updateTrackVote", "Error fetching tracks: ${it.message}")
//                    callback(false)
//                }
//        }
//    }
fun updateTrackVote(playlistId: String, track: PlaylistTrack, userId: String, callback: (Boolean) -> Unit) {
    val playlistRef = playlistsRef.child(playlistId)

    // Fetch the owner ID from the playlist reference
    playlistRef.child("owner").child("id").get().addOnSuccessListener { ownerSnapshot ->
        val ownerId = ownerSnapshot.value.toString()

        // Query for the tracks inside the owner's playlist (not the user's)
        val tracksRef = database.getReference("users")
            .child(ownerId) // Referring to the playlist owner
            .child("playlists") // The parent of playlists
            .child(playlistId)  // The specific playlist
            .child("tracks")
            .child("items")

        // Query for the track with the given track ID
        tracksRef.orderByChild("track/id").equalTo(track.track.id).limitToFirst(1).get()
            .addOnSuccessListener { snapshot ->
                val trackSnapshot = snapshot.children.firstOrNull()
                if (trackSnapshot != null) {
                    val currentVoteCount = trackSnapshot.child("voteCount").getValue(Int::class.java) ?: 0

                    // Use GenericTypeIndicator to safely cast the voters field to List<String>
                    val votedByIndicator = object : GenericTypeIndicator<List<String>>() {}
                    val votedBy = trackSnapshot.child("voters").getValue(votedByIndicator) ?: listOf()

                    if (votedBy.contains(userId)) {
                        // User has already voted
                        Log.e("updateTrackVote", "User $userId has already voted for this track.")
                        callback(false)
                    } else {
                        // Add the user to the votedBy list and increment the vote count
                        val updatedVotedBy = votedBy + userId
                        val newVoteCount = currentVoteCount + 1

                        // Create the update map for the vote count and voters
                        val updates = mapOf(
                            "voteCount" to newVoteCount,
                            "voters" to updatedVotedBy
                        )

                        // Perform the update
                        trackSnapshot.ref.updateChildren(updates)
                            .addOnCompleteListener { task ->
                                callback(task.isSuccessful)
                            }
                    }
                } else {
                    Log.e("updateTrackVote", "Track with ID ${track.track.id} not found.")
                    callback(false)
                }
            }.addOnFailureListener {
                Log.e("updateTrackVote", "Error fetching tracks: ${it.message}")
                callback(false)
            }
    }
}

        fun getTracks(callback: (List<Track>) -> Unit) {
            tracksRef.get()
                .addOnSuccessListener { snapshot ->
                    val tracks = snapshot.children.mapNotNull { it.getValue(Track::class.java) }
                    callback(tracks)
                }
                .addOnFailureListener {
                    callback(emptyList())
                }

        }

    fun getCollaborativePlaylist(playlist: Playlist, callback: (Playlist?) -> Unit) {
        val ownerId = playlist.owner.id

        val userPlaylistRef = database.getReference("users")
            .child(ownerId)
            .child("playlists")
            .child(playlist.id)

        userPlaylistRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseRepository", "Fetched playlist snapshot: ${snapshot.value}")

                if (snapshot.exists()) {
                    try {
                        val playlistId = snapshot.key ?: return
                        val playlistName = snapshot.child("name").getValue(String::class.java) ?: "Unnamed Playlist"
                        val description = snapshot.child("description").getValue(String::class.java) ?: ""
                        val collaborators = snapshot.child("collaborators")
                            .children.mapNotNull { it.getValue(User::class.java) }
                        val totalVotes = snapshot.child("totalVotes").getValue(Int::class.java) ?: 0
                        val lastModified = snapshot.child("lastModified").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val ownerSnapshot = snapshot.child("owner")
                        val owner = ownerSnapshot.getValue(User::class.java) ?: User()

                        val images = mutableListOf<Image>()
                        snapshot.child("images").children.forEach { imageSnapshot ->
                            val url = imageSnapshot.child("url").getValue(String::class.java)
                            val height = imageSnapshot.child("height").getValue(Int::class.java) ?: 0
                            val width = imageSnapshot.child("width").getValue(Int::class.java) ?: 0

                            if (url != null) {
                                val image = Image(height, url, width)
                                images.add(image)
                            }
                        }

                        val tracks = mutableListOf<PlaylistTrack>()
                        snapshot.child("tracks/items").children.forEach { trackSnapshot ->
                            try {
                                val trackId = trackSnapshot.child("track/id").getValue(String::class.java) ?: return@forEach
                                val trackName = trackSnapshot.child("track/name").getValue(String::class.java) ?: "Untitled Track"
                                val artistName = trackSnapshot.child("track/artists/0/name").getValue(String::class.java) ?: "Unknown Artist"
                                val albumName = trackSnapshot.child("track/album/name").getValue(String::class.java) ?: "Unknown Album"
                                val voteCount = trackSnapshot.child("voteCount").getValue(Int::class.java) ?: 0
                                val lastUpdated = trackSnapshot.child("lastUpdated").getValue(Long::class.java) ?: System.currentTimeMillis()
                                val externalUrls = trackSnapshot.child("added_by/externalUrls").getValue(ExternalUrls::class.java) ?: ExternalUrls()
                                val addedByTrack = trackSnapshot.child("track/added_by").getValue(User::class.java) ?: User()

                                val track = Track(
                                    id = trackId,
                                    name = trackName,
                                    added_by = addedByTrack,
                                    artists = listOf(Artist(name = artistName)),
                                    album = Album(name = albumName)
                                )
                                val addedBy = AddedBy(externalUrls = externalUrls)

                                val playlistTrack = PlaylistTrack(
                                    track = track,
                                    voteCount = voteCount,
                                    lastUpdated = lastUpdated,
                                    added_by = addedBy,
                                    addedCount = 0,
                                    voters = mutableListOf()
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
                            images = images,
                            owner = owner
                        )

                        callback(playlist) // Return the playlist directly
                    } catch (e: Exception) {
                        Log.e("FirebaseRepository", "Error mapping playlist data", e)
                        callback(null) // Return null if error occurs
                    }
                } else {
                    Log.e("FirebaseRepository", "Playlist not found.")
                    callback(null) // Return null if playlist does not exist
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Error fetching playlist", error.toException())
                callback(null) // Return null in case of cancellation
            }
        })
    }

    fun getUserIds(callback: (List<String>) -> Unit) {
        val userIds = mutableListOf<String>()
        val userRef = FirebaseDatabase.getInstance().getReference("users")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    userSnapshot.key?.let { userIds.add(it) }
                }
                callback(userIds)  // Passing the result to the callback after data is fetched
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching user IDs: ${error.message}")
            }
        })
    }



    fun getUsers(userIds: List<String>, callback: (MutableList<User>) -> Unit) {
        val users = mutableListOf<User>()
        val remainingRequests = userIds.size

        // Create a helper function to call the callback once all users are fetched
        var completedRequests = 0
        userIds.forEach { userId ->
            database.getReference("users").child(userId).get().addOnSuccessListener {
                val user = it.getValue(User::class.java)
                if (user != null) {
                    users.add(user)
                }
                completedRequests++
                if (completedRequests == remainingRequests) {
                    callback(users)  // Call the callback once all users are fetched
                }
            }.addOnFailureListener { exception ->
                Log.e("CollaborativePlaylist", "Error fetching user: ${exception.message}")
                completedRequests++
                if (completedRequests == remainingRequests) {
                    callback(users)  // Call the callback even if some requests fail
                }
            }
        }
    }

}

