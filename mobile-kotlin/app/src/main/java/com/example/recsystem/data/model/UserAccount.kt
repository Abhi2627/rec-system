package com.example.recsystem.data.model

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: String
)

data class AuthResponse(
    val user: UserAccount,
    val token: String
)
