package com.example.recsystem.ui.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recsystem.data.model.UserAccount
import com.example.recsystem.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: UserAccount, val token: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    // Expose the bearer token separately so other ViewModels/screens can read it
    // without having to cast AuthState.
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    val isLoggedIn: Boolean get() = _authState.value is AuthState.Success

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.login(email, password).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        _token.value = response.token
                        _authState.value = AuthState.Success(response.user, response.token)
                    },
                    onFailure = { error ->
                        _authState.value = AuthState.Error(error.message ?: "Login failed")
                    }
                )
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.register(name, email, password).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        _token.value = response.token
                        _authState.value = AuthState.Success(response.user, response.token)
                    },
                    onFailure = { error ->
                        _authState.value = AuthState.Error(error.message ?: "Registration failed")
                    }
                )
            }
        }
    }

    fun logout() {
        _token.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        if (_authState.value !is AuthState.Success) {
            _authState.value = AuthState.Idle
        }
    }

    /** Returns "Bearer <token>" or null when not signed in. */
    fun bearerToken(): String? = _token.value?.let { "Bearer $it" }
}
