package com.example.recsystem.data.model

import com.google.gson.annotations.SerializedName

data class CastMember(
    val name: String = "",
    val character: String = "",
    // Backend returns camelCase "profilePath" — matches field name directly
    val profilePath: String = ""
)

data class MovieDetail(
    val id: Int = 0,
    val title: String = "",
    val overview: String = "",
    // Backend returns snake_case for these fields
    @SerializedName("poster_path")   val poster_path: String = "",
    @SerializedName("backdrop_path") val backdrop_path: String = "",
    @SerializedName("release_date")  val release_date: String = "",
    @SerializedName("vote_average")  val vote_average: Double = 0.0,
    val runtime: Int = 0,
    val director: String = "",
    val cast: List<CastMember> = emptyList(),
    val trailerKey: String = "",
    val trailerUrl: String = "",
    val genres: List<String> = emptyList()
)
