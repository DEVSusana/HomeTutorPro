package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository contract for Auth data operations.
 */
interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, password: String): Result<User, DomainError>
    suspend fun register(email: String, password: String, name: String): Result<User, DomainError>
    suspend fun logout()
    /** Updates user profile data in both Firebase and local persistent storage. */
    suspend fun updateProfile(params: com.devsusana.hometutorpro.domain.entities.UpdateUserParams): Result<Unit, DomainError>
    suspend fun updatePassword(newPassword: String): Result<Unit, DomainError>
}
