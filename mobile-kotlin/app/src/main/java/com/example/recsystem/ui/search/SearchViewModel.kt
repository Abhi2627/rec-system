package com.example.recsystem.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recsystem.data.model.Movie
import com.example.recsystem.data.repository.DiscoveryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query:      String       = "",
    val results:    List<Movie>  = emptyList(),
    val trending:   List<Movie>  = emptyList(),   // shown in poster grid before any search
    val isLoading:  Boolean      = false,
    val error:      String?      = null,
    val hasSearched: Boolean     = false
)

class SearchViewModel(private val repository: DiscoveryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init { loadTrending() }

    private fun loadTrending() {
        viewModelScope.launch {
            runCatching {
                val movies = repository.fetchTrendingMovies()
                _uiState.value = _uiState.value.copy(trending = movies)
            }
        }
    }

    /** Called on every keystroke — 400 ms debounce */
    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, error = null)

        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.value = _uiState.value.copy(
                results     = emptyList(),
                isLoading   = false,
                hasSearched = false
            )
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            performSearch(query.trim())
        }
    }

    /** Called when user taps the keyboard Search action — fires immediately */
    fun search(query: String = _uiState.value.query) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(query.trim()) }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) return
        _uiState.value = _uiState.value.copy(isLoading = true, hasSearched = true, error = null)
        try {
            val results = repository.search(query)
            _uiState.value = _uiState.value.copy(results = results, isLoading = false)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Search failed. Check your connection."
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            query       = "",
            results     = emptyList(),
            isLoading   = false,
            hasSearched = false,
            error       = null
        )
    }
}
