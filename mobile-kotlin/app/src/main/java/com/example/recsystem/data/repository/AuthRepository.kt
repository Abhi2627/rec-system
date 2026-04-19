package com.example.recsystem.data.repository

import com.example.recsystem.data.api.RecSystemApi
import com.example.recsystem.data.model.AuthResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import retrofit2.HttpException

class AuthRepository(private val api: RecSystemApi) {

    fun login(email: String, password: String): Flow<Result<AuthResponse>> = flow {
        try {
            val response = api.login(mapOf("email" to email, "password" to password))
            emit(Result.success(response))
        } catch (e: HttpException) {
            emit(Result.failure(Exception(parseHttpError(e) ?: "Login failed")))
        } catch (e: Exception) {
            emit(Result.failure(Exception(friendlyNetworkError(e))))
        }
    }

    fun register(name: String, email: String, password: String): Flow<Result<AuthResponse>> = flow {
        try {
            val response = api.register(
                mapOf("name" to name, "email" to email, "password" to password)
            )
            emit(Result.success(response))
        } catch (e: HttpException) {
            emit(Result.failure(Exception(parseHttpError(e) ?: "Registration failed")))
        } catch (e: Exception) {
            emit(Result.failure(Exception(friendlyNetworkError(e))))
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Reads the JSON error body from a Retrofit HttpException, e.g. {"error":"User already exists"} */
    private fun parseHttpError(e: HttpException): String? {
        return try {
            val body = e.response()?.errorBody()?.string() ?: return null
            JSONObject(body).optString("error").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /** Converts common network exceptions into human-readable messages. */
    private fun friendlyNetworkError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            "ECONNREFUSED" in msg || "refused" in msg.lowercase() ->
                "Cannot reach server. Make sure the backend is running."
            "timeout" in msg.lowercase() ->
                "Request timed out. Check your network connection."
            "Unable to resolve host" in msg ->
                "No network connection."
            else -> msg.ifBlank { "An unexpected error occurred." }
        }
    }
}
