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
                val trendingMoviesD  = async { repository.fetchTrendingMovies() }
                val trendingTVD      = async { repository.fetchTrendingTV() }
                val actionD          = async { repository.fetchByCategory("movie", GENRE_ACTION) }
                val comedyD          = async { repository.fetchByCategory("movie", GENRE_COMEDY) }
                val thrillerD        = async { repository.fetchByCategory("movie", GENRE_THRILLER) }
                val scifiD           = async { repository.fetchByCategory("movie", GENRE_SCIFI) }

                _uiState.value = _uiState.value.copy(
                    trendingMovies  = trendingMoviesD.await(),
                    trendingTV      = trendingTVD.await(),
                    actionMovies    = actionD.await(),
                    comedyMovies    = comedyD.await(),
                    thrillerMovies  = thrillerD.await(),
                    scifiMovies     = scifiD.await(),
                    isLoading       = false,
                    error           = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not load content. Check your connection and retry."
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
