package com.example.partyplaylist.repositories

import android.content.Context
import android.util.Log
import com.example.partyplaylist.data.Followers
import com.example.partyplaylist.data.Song
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.AlbumsResponse
import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.models.ExternalIds
import com.example.partyplaylist.models.ExternalUrls
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.PlaylistTrack
import com.example.partyplaylist.models.SpotifyTrack
import com.example.partyplaylist.models.TopTracksResponse
import com.example.partyplaylist.models.Track
import com.example.partyplaylist.utils.SharedPreferencesManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener

class FirebaseRepository(private val context: Context) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val userId: String? = getUserId() // Assuming this method gets the current user's ID

    // Reference for the current user's node in the database
    private val userRef: DatabaseReference = database.getReference("users").child(userId ?: "")

    // All other references are now children of the userRef
    private val playlistsRef: DatabaseReference = userRef.child("playlists")
    private val tracksRef: DatabaseReference = userRef.child("tracks")
    private val albumsRef: DatabaseReference = database.getReference("users").child(userId?:"").child("albums")
    private val likedSongsRef: DatabaseReference = userRef.child("liked_songs")
    private val artistsRef: DatabaseReference = userRef.child("artists")

    private fun getUserId(): String? {
        val sharedPreferences = context.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("user_id", null) // Adjust key as needed
    }

    interface PlaylistsCallback {
        fun onPlaylistsFetched(playlists: List<Playlist?>?)
    }

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


    fun saveTracks(userId: String, tracks: List<Track>?) { // Allow tracks to be null
        if (tracks == null) {
            Log.e("FirebaseRepository", "Tracks list is null, cannot save.")
            return // Do not proceed if tracks is null
        }

        val userTracksRef = database.getReference("users").child(userId).child("tracks")
        for (track in tracks) { // Now we're sure tracks is not null
            userTracksRef.child(track.id.toString()).setValue(track)
                .addOnSuccessListener {
                    Log.d(
                        "FirebaseRepository",
                        "Track saved successfully for user $userId: ${track.id}"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "FirebaseRepository",
                        "Failed to save track for user $userId: ${track.id}",
                        e
                    )
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
                    // Album exists, retrieve existing tracks
                    val existingAlbum = dataSnapshot.getValue(Album::class.java)
                    val existingTracks = existingAlbum?.tracks?.items?.map { it.id } ?: emptyList()

                    // Prepare new tracks list without duplicates
                    val newTracks = album.tracks.items.filter { track ->
                        !existingTracks.contains(track.id)
                    }

                    // Update album with new tracks only
                    val updatedAlbum = album.copy(tracks = TopTracksResponse(newTracks))

                    albumRef.setValue(updatedAlbum).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FirebaseRepository", "Album updated successfully for user $userId: ${album.id}")
                        } else {
                            Log.e("FirebaseRepository", "Failed to update album for user $userId: ${album.id}", task.exception)
                        }
                    }
                } else {
                    // If album doesn't exist, simply save it
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
            Log.e("FbRepo_SavePlaylists1", "Playlists list is null, cannot save.")
            return
        }

        val userPlaylistsRef = database.getReference("users").child(userId).child("playlists") // Reference to user's playlists
        for (playlist in playlists) {
            if (playlist.id.isNullOrEmpty()) {
                Log.e(
                    "FbRepo_SavePlaylists1",
                    "Playlist ID is null or empty for playlist: ${playlist.name}"
                )
                continue
            }

            // Log the playlist snapshot and track details
            Log.d("FbRepo_SavePlaylists1", "Saving playlist: ${playlist.name}, ID: ${playlist.id}")
            Log.d("FbRepo_SavePlaylists1", "Playlist data snapshot: $playlist")

            // Log track details in the playlist
            playlist.tracks.items.forEachIndexed { index, track ->
                Log.d("FbRepo_SavePlaylists1", "Track $index: ${track.track.name}, Preview URL: ${track.track.previewUrl}")
            }

            userPlaylistsRef.child(playlist.id)
                .setValue(playlist) // Save under user's playlists node
                .addOnSuccessListener {
                    Log.d(
                        "FbRepo_SavePlaylists1",
                        "Playlist ${playlist.name} saved successfully for user $userId"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "FbRepo_SavePlaylists1",
                        "Error saving playlist ${playlist.name} for user $userId",
                        e
                    )
                }
        }
    }



    fun savePlaylists(playlists: List<Playlist>) {
        Log.d("SavePlaylists", "Saving playlists, count: ${playlists.size}")

        for (playlist in playlists) {
            Log.d("SavePlaylists", "Processing playlist: ${playlist.name}, ID: ${playlist.id}")

            if (playlist.id.isNullOrEmpty()) {
                Log.e(
                    "SavePlaylists",
                    "Playlist ID is null or empty for playlist: ${playlist.name}"
                )
                continue // Skip saving this playlist
            }

            // Log the playlist details and snapshot data to see the content
            Log.d("SavePlaylists", "Playlist data snapshot: $playlist")
            Log.d("SavePlaylists", "Tracks in playlist: ${playlist.tracks.items.size} tracks")

            // Log the tracks for debugging
            playlist.tracks.items.forEachIndexed { index, track ->
                Log.d("SavePlaylists", "Track $index: ${track.track.name}, Preview URL: ${track.track.previewUrl}")
            }

            playlistsRef.child(playlist.id).setValue(playlist)
                .addOnSuccessListener {
                    // Handle success
                    Log.d("SavePlaylists", "Playlist ${playlist.name} saved successfully")
                }
                .addOnFailureListener { e ->
                    // Handle failure
                    Log.e("SavePlaylists", "Error saving playlist ${playlist.name}", e)
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
        playlist.tracks.items.forEachIndexed { index, track ->
            Log.d("SavePlaylist", "Track $index: ${track.track.name}, Preview URL: ${track.track.previewUrl}")
        }

        val userPlaylistsRef = database.getReference("users").child(userId).child("playlists")
        userPlaylistsRef.child(playlist.id).setValue(playlist)
            .addOnSuccessListener {
                Log.d(
                    "SavePlaylist",
                    "Playlist ${playlist.name} saved successfully for user $userId"
                )
            }
            .addOnFailureListener { e ->
                Log.e(
                    "SavePlaylist",
                    "Error saving playlist ${playlist.name} for user $userId",
                    e
                )
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

        likedSongsRef.child(trackId).setValue(true)
            .addOnSuccessListener {
                // Handle success
            }
            .addOnFailureListener {
                // Handle failure
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
    // Log the start of the method
    Log.d("getAlbum", "Fetching album with ID: $albumId")

    albumsRef.child(albumId).get()
        .addOnSuccessListener { dataSnapshot ->
            // Log the success of the database query
            Log.d("getAlbum", "Successfully fetched data for album ID: $albumId")

            // Log the dataSnapshot received
            Log.d("getAlbum", "DataSnapshot: $dataSnapshot")

            val album = dataSnapshot.getValue(Album::class.java)

            // Log the album object obtained
            Log.d("getAlbum", "Album: $album")

            callback(album)
        }
        .addOnFailureListener { exception ->
            // Log the failure
            Log.e("getAlbum", "Failed to fetch album with ID: $albumId", exception)

            // Handle failure and log the error
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
            userArtistsRef.child(artist.id).setValue(artist)
                .addOnSuccessListener {
                    Log.d(
                        "FirebaseRepository",
                        "Artist saved successfully for user $userId: ${artist.id}"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "FirebaseRepository",
                        "Failed to save artist for user $userId: ${artist.id}",
                        e
                    )
                }
        }
    }

    fun saveArtists(artists: List<Artist>) {
//        val artistsRef = database.reference.child("artists")
        for (artist in artists) {
            artistsRef.child(artist.id).setValue(artist)
                .addOnSuccessListener {
                    // Handle success
                }
                .addOnFailureListener {
                    // Handle failure
                }
        }
    }

     fun getAllPlaylists(callback: PlaylistsCallback) {
//        val userId = SharedPreferencesManager.getUserId(context) // Get current user ID
        if (userId == null) {
            Log.e("FirebaseRepository", "User ID is null, cannot fetch playlists.")
            callback.onPlaylistsFetched(emptyList())
            return
        }

        val userPlaylistsRef = userRef.child("playlists")
        userPlaylistsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val playlists = snapshot.children.mapNotNull { it.getValue(Playlist::class.java) }
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
        val artistTracksRef = database.getReference("users").child(userId.toString()).child("artists").child(artistId).child("songs")
        artistTracksRef.setValue(tracks).addOnSuccessListener {
            Log.d("FirebaseRepository", "Top tracks for artist $artistId saved successfully")
        }.addOnFailureListener { e ->
            Log.e("FirebaseRepository", "Failed to save top tracks for artist $artistId", e)
        }
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
                        // Track exists, update its details (e.g., vote count)
                        existingTrack.voteCount = track.voteCount
                        trackRef.setValue(existingTrack)  // Update track
                    } else {
                        // Track doesn't exist, create it
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
        val trackRef = playlistsRef.child(playlistId).child("tracks").child(track.track.id.toString())

        trackRef.child("voteCount").setValue(track.voteCount).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true)
            } else {
                callback(false)
            }
        }
    }
    fun getPlaylistTracks(playlistId: String, callback: (List<PlaylistTrack>?) -> Unit) {
        playlistsRef.child(playlistId).child("tracks").child("items").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val tracks = task.result?.children?.mapNotNull {
                    it.getValue(PlaylistTrack::class.java)
                }
                callback(tracks)
            } else {
                callback(null)
            }
        }
    }


}