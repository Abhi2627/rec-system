package com.example.recsystem.data.api

import com.example.recsystem.data.model.*
import retrofit2.http.*

interface RecSystemApi {

    // ── Public discovery endpoints (no auth required) ─────────────────────

    @GET("api/discovery/trending")
    suspend fun getTrending(): List<Movie>

    @GET("api/discovery/trending/tv")
    suspend fun getTrendingTV(): List<Movie>

    @GET("api/discovery/category/{type}/{genreId}")
    suspend fun getByCategory(
        @Path("type") type: String,
        @Path("genreId") genreId: String
    ): List<Movie>

    @GET("api/discovery/details/{type}/{id}")
    suspend fun getMovieDetails(
        @Path("type") type: String,
        @Path("id") id: String
    ): MovieDetail

    // Smart search: TMDB results → AI re-ranked
    // Response shape: { query, source, results: [...] }
    @GET("api/discovery/smart-search")
    suspend fun smartSearch(@Query("q") query: String): SearchResponse

    // Pure AI recommendation from CSV embeddings
    // Response shape: { query, source, recommendations: [...] }
    @GET("api/discovery/ai-recommend")
    suspend fun aiRecommend(@Query("q") query: String): SearchResponse

    // ── Auth endpoints ────────────────────────────────────────────────────

    @POST("api/auth/register")
    suspend fun register(@Body body: Map<String, String>): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") bearerToken: String): MeResponse

    // ── Personalization endpoints (auth required) ─────────────────────────

    @GET("api/personalization/profile")
    suspend fun getProfile(@Header("Authorization") bearerToken: String): ProfileResponse

    @PUT("api/personalization/profile")
    suspend fun updateProfile(
        @Header("Authorization") bearerToken: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): ProfileResponse

    @POST("api/personalization/recommendations")
    suspend fun getPersonalizedRecommendations(
        @Header("Authorization") bearerToken: String,
        @Body body: Map<String, String>
    ): PersonalizedRecommendationsResponse

    @POST("api/personalization/saved-movies")
    suspend fun saveMovie(
        @Header("Authorization") bearerToken: String,
        @Body movie: Map<String, @JvmSuppressWildcards Any>
    ): ProfileResponse

    @DELETE("api/personalization/saved-movies/{movieId}")
    suspend fun removeMovie(
        @Header("Authorization") bearerToken: String,
        @Path("movieId") movieId: Int
    ): ProfileResponse

    @POST("api/personalization/search-history")
    suspend fun addSearchHistory(
        @Header("Authorization") bearerToken: String,
        @Body body: Map<String, String>
    ): ProfileResponse
}
