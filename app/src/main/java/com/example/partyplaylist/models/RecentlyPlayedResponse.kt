import com.example.partyplaylist.models.Track
import com.google.gson.annotations.SerializedName

data class RecentlyPlayedResponse(
    @SerializedName("items") val items: List<PlayedTrack>
)

data class PlayedTrack(
    @SerializedName("track") val track: Track,
    @SerializedName("played_at") val playedAt: String
)
