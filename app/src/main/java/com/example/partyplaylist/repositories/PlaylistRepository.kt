package com.example.partyplaylist.repositories

import com.example.partyplaylist.models.Playlist
import com.google.firebase.database.FirebaseDatabase

class PlaylistRepository {

    private val database = FirebaseDatabase.getInstance()
    private val playlistRef = database.getReference("playlists")

    fun addPlaylist(playlist: Playlist, onComplete: (Boolean, Exception?) -> Unit) {
        val newPlaylistRef = playlistRef.push()
        newPlaylistRef.setValue(playlist)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception)
            }
    }

    fun getPlaylist(playlistId: String, onComplete: (Playlist?, Exception?) -> Unit) {
        playlistRef.child(playlistId).get()
            .addOnSuccessListener { dataSnapshot ->
                val playlist = dataSnapshot.getValue(Playlist::class.java)
                onComplete(playlist, null)
            }
            .addOnFailureListener { exception ->
                onComplete(null, exception)
            }
    }

    fun deletePlaylist(playlistId: String, onComplete: (Boolean, Exception?) -> Unit) {
        playlistRef.child(playlistId).removeValue()
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception)
            }
    }

    fun updatePlaylist(playlistId: String, updates: Map<String, Any>, onComplete: (Boolean, Exception?) -> Unit) {
        playlistRef.child(playlistId).updateChildren(updates)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception)
            }
    }
}
