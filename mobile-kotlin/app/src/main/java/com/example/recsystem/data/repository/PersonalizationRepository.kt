package com.example.recsystem.data.repository

import com.example.recsystem.data.api.RecSystemApi
import com.example.recsystem.data.model.PersonalizationProfile

class PersonalizationRepository(private val api: RecSystemApi) {

    suspend fun getProfile(bearerToken: String): PersonalizationProfile =
        api.getProfile(bearerToken).profile

    suspend fun updateProfile(
        bearerToken: String,
        displayName: String,
        age: Int?,
        favoriteGenres: List<String>,
        favoriteKeywords: List<String>,
        favoriteActors: List<String>,
        favoriteActresses: List<String>,
        favoriteDirectors: List<String>
    ): PersonalizationProfile {
        val body = buildMap<String, Any> {
            put("displayName", displayName)
            if (age != null) put("age", age)
            put("favoriteGenres", favoriteGenres)
            put("favoriteKeywords", favoriteKeywords)
            put("favoriteActors", favoriteActors)
            put("favoriteActresses", favoriteActresses)
            put("favoriteDirectors", favoriteDirectors)
        }
        return api.updateProfile(bearerToken, body).profile
    }

    suspend fun saveMovie(
        bearerToken: String,
        movieId: Int,
        title: String,
        overview: String,
        posterPath: String,
        releaseDate: String,
        voteAverage: Double
    ): PersonalizationProfile {
        val body = buildMap<String, Any> {
            put("id", movieId)
            put("title", title)
            put("overview", overview)
            put("poster_path", posterPath)
            put("release_date", releaseDate)
            put("vote_average", voteAverage)
        }
        return api.saveMovie(bearerToken, body).profile
    }

    suspend fun removeMovie(
        bearerToken: String,
        movieId: Int
    ): PersonalizationProfile =
        api.removeMovie(bearerToken, movieId).profile

    suspend fun addSearchHistory(
        bearerToken: String,
        query: String
    ): PersonalizationProfile =
        api.addSearchHistory(bearerToken, mapOf("query" to query)).profile
}
