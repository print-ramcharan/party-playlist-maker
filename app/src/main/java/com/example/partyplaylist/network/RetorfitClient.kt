package com.example.partyplaylist.network

import com.example.partyplaylist.models.data.SearchResponse
import com.google.android.gms.common.api.Response
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

//interface SpotifyApiService {
//    @GET("v1/search")
//    suspend fun searchSpotify(
//        @Query("q") query: String,
//        @Query("type") type: String = "track,album,artist",
//        @Query("limit") limit: Int = 10
//    ): Response<SearchResponse>
//}
object RetrofitClient {

    private const val BASE_URL = "https://api.spotify.com/"

    private val retrofit: Retrofit by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getClient(): Retrofit {
        return retrofit
    }
    fun getSpotifyApiService(): SpotifyService {
        return getClient().create(SpotifyService::class.java)
    }
}
