package com.example.recsystem.data.repository

import android.content.Context
import com.example.recsystem.data.api.RecSystemApi
import com.example.recsystem.data.model.Movie
import com.example.recsystem.data.model.MovieDetail

class DiscoveryRepository(
    private val api: RecSystemApi,
    context: Context
) {
    // SharedPreferences kept for future local caching
    @Suppress("unused")
    private val prefs = context.getSharedPreferences("rec_system_prefs", Context.MODE_PRIVATE)

    suspend fun fetchTrendingMovies(): List<Movie> = api.getTrending()

    suspend fun fetchTrendingTV(): List<Movie> = api.getTrendingTV()

    suspend fun fetchByCategory(type: String, genreId: String): List<Movie> =
        api.getByCategory(type, genreId)

    suspend fun fetchMovieDetails(type: String, id: String): MovieDetail =
        api.getMovieDetails(type, id)

    /**
     * Smart search: TMDB results re-ranked by the AI service.
     * The backend handles AI downtime and returns TMDB results as a fallback.
     */
    suspend fun search(query: String): List<Movie> =
        api.smartSearch(query).movies()

    /**
     * Pure AI recommendation from the local embedding model.
     */
    suspend fun aiRecommend(query: String): List<Movie> =
        api.aiRecommend(query).movies()

    /**
     * Personalized recommendations based on the authenticated user's profile.
     * Requires a valid bearer token.
     */
    suspend fun fetchPersonalizedRecommendations(
        bearerToken: String,
        query: String
    ): List<Movie> =
        api.getPersonalizedRecommendations(bearerToken, mapOf("query" to query)).recommendations
}
