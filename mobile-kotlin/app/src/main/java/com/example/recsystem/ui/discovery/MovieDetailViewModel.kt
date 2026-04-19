package com.example.recsystem.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recsystem.data.model.MovieDetail
import com.example.recsystem.data.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val detail: MovieDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentKey: String = ""
)

class MovieDetailViewModel(
    private val repository: DiscoveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState = _uiState.asStateFlow()

    // Survive back-navigation: cache keyed by "$type/$id"
    private val cache = mutableMapOf<String, MovieDetail>()

    fun loadDetails(type: String, id: String) {
        val key = "$type/$id"

        // Already showing this movie — nothing to do
        if (_uiState.value.currentKey == key && _uiState.value.detail != null) return

        // Serve from cache instantly if available
        cache[key]?.let {
            _uiState.value = MovieDetailUiState(detail = it, currentKey = key)
            return
        }

        // Clear stale content from a different movie immediately
        if (_uiState.value.currentKey != key) {
            _uiState.value = MovieDetailUiState(isLoading = true, currentKey = key)
        } else {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            try {
                val detail = repository.fetchMovieDetails(type, id)
                cache[key] = detail
                _uiState.value = MovieDetailUiState(detail = detail, currentKey = key)
            } catch (e: Exception) {
                _uiState.value = MovieDetailUiState(
                    error = "Failed to load. Tap retry.",
                    currentKey = key
                )
            }
        }
    }

    fun retry(type: String, id: String) {
        cache.remove("$type/$id")
        loadDetails(type, id)
    }
}
