package com.example.recsystem.data.model

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    val query: String = "",
    val source: String = "",
    // smart-search returns "results", ai-recommend returns "recommendations"
    val results: List<Movie> = emptyList(),
    val recommendations: List<Movie> = emptyList()
) {
    /** Returns whichever list is populated. */
    fun movies(): List<Movie> = results.ifEmpty { recommendations }
}
