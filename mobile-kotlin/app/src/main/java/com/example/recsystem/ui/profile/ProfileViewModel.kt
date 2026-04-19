package com.example.recsystem.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recsystem.data.model.PersonalizationProfile
import com.example.recsystem.data.repository.PersonalizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: PersonalizationProfile? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveError: String? = null,
    // Toast-style one-shot message
    val successMessage: String? = null
)

class ProfileViewModel(
    private val repository: PersonalizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    // Current bearer token — set by MainActivity whenever auth state changes
    private var bearerToken: String? = null

    fun loadProfile(token: String) {
        bearerToken = token
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profile = repository.getProfile(token)
                _uiState.value = _uiState.value.copy(profile = profile, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load profile: ${e.message}"
                )
            }
        }
    }

    fun clearProfile() {
        bearerToken = null
        _uiState.value = ProfileUiState()
    }

    fun updatePreferences(
        displayName: String,
        age: Int?,
        favoriteGenres: List<String>,
        favoriteKeywords: List<String>,
        favoriteActors: List<String>,
        favoriteActresses: List<String>,
        favoriteDirectors: List<String>
    ) {
        val token = bearerToken ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            try {
                val updated = repository.updateProfile(
                    token, displayName, age,
                    favoriteGenres, favoriteKeywords,
                    favoriteActors, favoriteActresses, favoriteDirectors
                )
                _uiState.value = _uiState.value.copy(
                    profile        = updated,
                    isSaving       = false,
                    successMessage = "Preferences saved"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving   = false,
                    saveError  = e.message ?: "Save failed"
                )
            }
        }
    }

    fun saveMovie(
        movieId: Int,
        title: String,
        overview: String,
        posterPath: String,
        releaseDate: String,
        voteAverage: Double
    ) {
        val token = bearerToken ?: return
        viewModelScope.launch {
            try {
                val updated = repository.saveMovie(
                    token, movieId, title, overview, posterPath, releaseDate, voteAverage
                )
                _uiState.value = _uiState.value.copy(
                    profile        = updated,
                    successMessage = "Added to watchlist"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saveError = e.message ?: "Could not save movie"
                )
            }
        }
    }

    fun removeMovie(movieId: Int) {
        val token = bearerToken ?: return
        viewModelScope.launch {
            try {
                val updated = repository.removeMovie(token, movieId)
                _uiState.value = _uiState.value.copy(
                    profile        = updated,
                    successMessage = "Removed from watchlist"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saveError = e.message ?: "Could not remove movie"
                )
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null, saveError = null)
    }

    /** Returns true if the movie with [movieId] is already in the saved list. */
    fun isMovieSaved(movieId: Int): Boolean =
        _uiState.value.profile?.savedMovies?.any { it.id == movieId } == true
}
