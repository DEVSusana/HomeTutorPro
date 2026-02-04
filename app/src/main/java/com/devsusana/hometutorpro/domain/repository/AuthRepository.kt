package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, password: String): Result<User, DomainError>
    suspend fun register(email: String, password: String, name: String): Result<User, DomainError>
    suspend fun logout()
    suspend fun updateProfile(
        name: String, 
        email: String, 
        workingStartTime: String, 
        workingEndTime: String
    ): Result<Unit, DomainError>
    suspend fun updatePassword(newPassword: String): Result<Unit, DomainError>
}
