import com.example.partyplaylist.models.Image
import com.example.partyplaylist.data.Song
import com.example.partyplaylist.models.Track
import com.google.gson.annotations.SerializedName

data class AlbumResponse(

    @SerializedName("items") val albums: List<Album>
)

data class Album(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("images") val images: List<Image>?,
    @SerializedName("tracks") var tracks: AlbumTracks
)

data class AlbumTracks(
    @SerializedName("items") var tracks: List<Track>
)
