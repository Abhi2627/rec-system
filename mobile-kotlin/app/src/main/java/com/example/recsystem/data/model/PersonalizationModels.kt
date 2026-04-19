package com.example.recsystem.data.model

/** Wrapper returned by GET /api/auth/me */
data class MeResponse(val user: UserAccount)

/** Wrapper returned by personalization profile endpoints */
data class PersonalizationProfile(
    val userId: String = "",
    val displayName: String = "",
    val age: Int? = null,
    val favoriteGenres: List<String> = emptyList(),
    val favoriteKeywords: List<String> = emptyList(),
    val favoriteActors: List<String> = emptyList(),
    val favoriteActresses: List<String> = emptyList(),
    val favoriteDirectors: List<String> = emptyList(),
    val savedMovies: List<Movie> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val updatedAt: String = ""
)

data class ProfileResponse(val profile: PersonalizationProfile)

data class PersonalizedRecommendationsResponse(
    val query: String = "",
    val prompt: String = "",
    val profile: PersonalizationProfile = PersonalizationProfile(),
    val recommendations: List<Movie> = emptyList()
)
