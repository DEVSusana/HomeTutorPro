package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.entities.UpdateUserParams
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AuthRepositoryModule::class]
)
object TestAuthRepositoryModule {

    private val loggedInUser = User(
        uid = "test_user_id",
        email = "test@example.com",
        displayName = "Test User",
        workingStartTime = "08:00",
        workingEndTime = "23:00",
        notes = ""
    )

    private val _currentUser = MutableStateFlow<User?>(loggedInUser)

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = object : AuthRepository {
        override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

        override suspend fun login(email: String, password: String): Result<User, DomainError> {
            _currentUser.value = loggedInUser
            return Result.Success(loggedInUser)
        }

        override suspend fun register(email: String, password: String, name: String): Result<User, DomainError> {
            _currentUser.value = loggedInUser
            return Result.Success(loggedInUser)
        }

        override suspend fun logout() {
            _currentUser.value = null
        }

        override suspend fun updateProfile(params: UpdateUserParams): Result<Unit, DomainError> {
            return Result.Success(Unit)
        }

        override suspend fun updatePassword(newPassword: String): Result<Unit, DomainError> {
            return Result.Success(Unit)
        }
    }
}
