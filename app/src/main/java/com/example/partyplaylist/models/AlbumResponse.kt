import com.example.partyplaylist.models.Image
import com.example.partyplaylist.models.Track
import com.google.gson.annotations.SerializedName

// Response containing a list of albums
data class AlbumResponse(
    @SerializedName("items") val albums: List<Album> = emptyList() // Ensures the list is never null
)

// Album data class, representing an album with its properties and tracks
data class Album(
    @SerializedName("id") var id: String = "", // Default empty string value for id
    @SerializedName("name") var name: String = "", // Default empty string value for name
    @SerializedName("images") var images: List<Image>? = null, // Nullable list for images
    @SerializedName("tracks") var tracks: AlbumTracks = AlbumTracks() // Default empty AlbumTracks
) {
    // Getters and Setters for better compatibility with Firebase or Gson
//    fun getId(): String {
//        return id
//    }
//
//    fun setId(id: String) {
//        this.id = id
//    }
//
//    fun getName(): String {
//        return name
//    }
//
//    fun setName(name: String) {
//        this.name = name
//    }
//
//    fun getImages(): List<Image>? {
//        return images
//    }
//
//    fun setImages(images: List<Image>?) {
//        this.images = images
//    }
//
//    fun getTracks(): AlbumTracks {
//        return tracks
//    }
//
//    fun setTracks(tracks: AlbumTracks) {
//        this.tracks = tracks
//    }
}

// AlbumTracks data class to hold the list of tracks within an album
data class AlbumTracks(
    @SerializedName("items") var tracks: List<Track> = emptyList() // Default empty list for tracks
) {
    // Getters and Setters for better compatibility with Firebase or Gson
//    fun getTracks(): List<Track> {
//        return tracks
//    }
//
//    fun setTracks(tracks: List<Track>) {
//        this.tracks = tracks
//    }
}
