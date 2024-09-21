package com.example.partyplaylist.repositories

import android.util.Log
import com.example.partyplaylist.data.Song
import com.example.partyplaylist.data.User
import com.example.partyplaylist.models.Album
import com.example.partyplaylist.models.Artist
import com.example.partyplaylist.models.Playlist
import com.example.partyplaylist.models.SpotifyTrack
import com.example.partyplaylist.models.Track
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class FirebaseRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val userRef: DatabaseReference = database.getReference("users")
    private val playlistsRef: DatabaseReference = database.getReference("playlists")
    private val tracksRef: DatabaseReference = database.getReference("tracks")
    private val albumsRef: DatabaseReference = database.getReference("albums")
    private val likedSongsRef: DatabaseReference = database.getReference("liked_songs")
    private val artistsRef: DatabaseReference = database.getReference("artists")

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
    fun saveTracks(tracks: List<Track>) {
        val tracksRef = database.reference.child("tracks")
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
            albumsRef.child(album.id).setValue(album)
                .addOnSuccessListener {
                    Log.d("FirebaseRepository", "Album saved successfully: ${album.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseRepository", "Failed to save album: ${album.id}", e)
                }
        }
    }
//    fun saveAlbums2(albums: List<com.example.partyplaylist.trail.Album>) {
//        for (album in albums) {
//            albumsRef.child(album.id).setValue(album)
//                .addOnSuccessListener {
//                    Log.d("FirebaseRepository", "Album saved successfully: ${album.id}")
//                }
//                .addOnFailureListener { e ->
//                    Log.e("FirebaseRepository", "Failed to save album: ${album.id}", e)
//                }
//        }
//    }
    fun savePlaylists(playlists: List<Playlist>) {
        for (playlist in playlists) {
            if (playlist.id.isNullOrEmpty()) {
                Log.e("FirebaseRepository", "Playlist ID is null or empty for playlist: ${playlist.name}")
                continue // Skip saving this playlist
            }

            playlistsRef.child(playlist.id).setValue(playlist)
                .addOnSuccessListener {
                    // Handle success
                    Log.d("FirebaseRepository", "Playlist ${playlist.name} saved successfully")
                }
                .addOnFailureListener { e ->
                    // Handle failure
                    Log.e("FirebaseRepository", "Error saving playlist ${playlist.name}", e)
                }
        }
    }

    fun savePlaylist(playlist: Playlist) {
        if (playlist.id.isNullOrEmpty()) {
            Log.e("FirebaseRepository", "Playlist ID is null or empty for playlist: ${playlist.name}")
            return // Skip saving this playlist
        }

        playlistsRef.child(playlist.id).setValue(playlist)
            .addOnSuccessListener {
                // Handle success
                Log.d("FirebaseRepository", "Playlist ${playlist.name} saved successfully")
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e("FirebaseRepository", "Error saving playlist ${playlist.name}", e)
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
        likedSongsRef.child(userId).child(trackId).setValue(true)
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
    fun saveArtists(artists: List<Artist>) {
        val artistsRef = database.reference.child("artists")
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

    // Additional methods for other Firebase operations

//    fun getAllPlaylists(callback: (List<Playlist>?) -> Unit) {
//        playlistsRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val playlists = snapshot.children.mapNotNull { it.getValue(Playlist::class.java) }
//                callback(playlists)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("FirebaseRepository", "Error fetching playlists", error.toException())
//                callback(null)
//            }
//        })
//    }
open fun getAllPlaylists(callback: PlaylistsCallback) {
    playlistsRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d("FirebaseRepository", "Raw data: " + snapshot.value)
            val playlists: MutableList<Playlist?> = ArrayList()
            for (ds in snapshot.children) {
                try {
                    val playlist = ds.getValue(Playlist::class.java)
                    if (playlist != null) {
                        playlists.add(playlist)
                    }
                } catch (e: java.lang.Exception) {
                    Log.e("FirebaseRepository", "Error parsing playlist: " + ds.value, e)
                }
            }
            callback.onPlaylistsFetched(playlists)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("FirebaseRepository", "Error fetching playlists", error.toException())
            callback.onPlaylistsFetched(null)
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
                Log.e("FirebaseRepository", "Error fetching liked songs for user $userId", error.toException())
                callback(null)
            }
        })
    }

    // Fetch all artists
    fun getAllArtists(callback: (List<Artist>?) -> Unit) {
        artistsRef.addListenerForSingleValueEvent(object : ValueEventListener {
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
        // Query to fetch artist details based on artistName
        artistsRef.orderByChild("name").equalTo(artistName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Fetch the artist details
                    val artistSnapshot = snapshot.children.firstOrNull()

                    val artist = artistSnapshot?.getValue(Artist::class.java)

                    // Fetch the songs if the artist is found
                    artistSnapshot?.child("songs")?.let { songsSnapshot ->
                        Log.d("FirebaseRepository", "Songs Snapshot: ${songsSnapshot.value}")
                        for (songSnapshot in songsSnapshot.children) {
                            try {
                                val song = songSnapshot.getValue(Song::class.java)
                                song?.let { Log.d("FirebaseRepository", "Song: $it") }
                            } catch (e: Exception) {
                                Log.e("FirebaseRepository", "Error deserializing song", e)
                            }
                        }
                    }

                    callback(artist)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                    Log.e("FirebaseRepository", "Error fetching artist details", error.toException())
                    callback(null)
                }
            })
    }

    fun saveArtistTopTracks(artistId: String, tracks: List<Track>) {
        val artistTracksRef = database.getReference("artists").child(artistId).child("songs")
        artistTracksRef.setValue(tracks).addOnSuccessListener {
            Log.d("FirebaseRepository", "Top tracks for artist $artistId saved successfully")
        }.addOnFailureListener { e ->
            Log.e("FirebaseRepository", "Failed to save top tracks for artist $artistId", e)
        }
    }


}
