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
    val query: String = "",
    val results: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

class SearchViewModel(private val repository: DiscoveryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /** Called on every keystroke. Debounces 500 ms before firing. */
    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, error = null)

        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.value = _uiState.value.copy(
                results = emptyList(),
                isLoading = false,
                hasSearched = false
            )
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query.trim())
        }
    }

    /** Called when the user taps the search button / keyboard action. Fires immediately. */
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
                error = "Search failed: ${e.message}"
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = SearchUiState()
    }
}
