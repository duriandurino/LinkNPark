package com.example.linknpark.data

import com.example.linknpark.model.User
import com.example.linknpark.model.UserRole

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String, role: UserRole): Result<User>
    suspend fun getCurrentUser(): User?
    suspend fun logout()
    suspend fun updateUserProfile(userId: String, name: String, password: String): Result<User>
}

