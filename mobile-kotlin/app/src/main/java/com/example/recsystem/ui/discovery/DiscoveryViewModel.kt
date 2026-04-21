package com.example.recsystem.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recsystem.data.model.Movie
import com.example.recsystem.data.repository.DiscoveryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val GENRE_ACTION   = "28"
private const val GENRE_COMEDY   = "35"
private const val GENRE_THRILLER = "53"
private const val GENRE_SCIFI    = "878"

data class DiscoveryUiState(
    val trendingMovies:   List<Movie> = emptyList(),
    val trendingTV:       List<Movie> = emptyList(),
    val actionMovies:     List<Movie> = emptyList(),
    val comedyMovies:     List<Movie> = emptyList(),
    val thrillerMovies:   List<Movie> = emptyList(),
    val scifiMovies:      List<Movie> = emptyList(),
    val personalizedMovies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DiscoveryViewModel(
    private val repository: DiscoveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState = _uiState.asStateFlow()

    init { loadAllContent() }

    fun loadAllContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // All 6 requests fire in parallel — total time = slowest single request
                val trendingMoviesD  = async { runCatching { repository.fetchTrendingMovies() }.getOrDefault(emptyList()) }
                val trendingTVD      = async { runCatching { repository.fetchTrendingTV() }.getOrDefault(emptyList()) }
                val actionD          = async { runCatching { repository.fetchByCategory("movie", GENRE_ACTION) }.getOrDefault(emptyList()) }
                val comedyD          = async { runCatching { repository.fetchByCategory("movie", GENRE_COMEDY) }.getOrDefault(emptyList()) }
                val thrillerD        = async { runCatching { repository.fetchByCategory("movie", GENRE_THRILLER) }.getOrDefault(emptyList()) }
                val scifiD           = async { runCatching { repository.fetchByCategory("movie", GENRE_SCIFI) }.getOrDefault(emptyList()) }

                val trending  = trendingMoviesD.await()
                val tv        = trendingTVD.await()
                val action    = actionD.await()
                val comedy    = comedyD.await()
                val thriller  = thrillerD.await()
                val scifi     = scifiD.await()

                if (trending.isEmpty() && tv.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Could not connect to server. Make sure the backend is running."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        trendingMovies  = trending,
                        trendingTV      = tv,
                        actionMovies    = action,
                        comedyMovies    = comedy,
                        thrillerMovies  = thriller,
                        scifiMovies     = scifi,
                        isLoading       = false,
                        error           = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not connect to server. Make sure the backend is running."
                )
            }
        }
    }

    /** True when content has never loaded successfully. */
    val isEmpty: Boolean
        get() = _uiState.value.trendingMovies.isEmpty() && !_uiState.value.isLoading

    fun loadPersonalizedRow(bearerToken: String?, query: String = "movies I will enjoy") {
        if (bearerToken == null) {
            _uiState.value = _uiState.value.copy(personalizedMovies = emptyList())
            return
        }
        viewModelScope.launch {
            try {
                val movies = repository.fetchPersonalizedRecommendations(bearerToken, query)
                _uiState.value = _uiState.value.copy(personalizedMovies = movies)
            } catch (_: Exception) {
                // Personalised row is optional — skip silently
            }
        }
    }
}
