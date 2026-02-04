package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetCurrentUserUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetCurrentUserUseCase.
 * 
 * Tests retrieval of current authenticated user.
 */
class GetCurrentUserUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)
    }

    @Test
    fun `invoke should return current user when user is logged in`() = runTest {
        // Given
        val currentUser = User(
            uid = "user123",
            email = "test@example.com",
            displayName = "Test User"
        )
        val userFlow = MutableStateFlow<User?>(currentUser)
        every { authRepository.currentUser } returns userFlow

        // When
        val result = getCurrentUserUseCase().first()

        // Then
        assertEquals(currentUser, result)
        verify(exactly = 1) { authRepository.currentUser }
    }

    @Test
    fun `invoke should return null when no user is logged in`() = runTest {
        // Given
        val userFlow = MutableStateFlow<User?>(null)
        every { authRepository.currentUser } returns userFlow

        // When
        val result = getCurrentUserUseCase().first()

        // Then
        assertNull(result)
    }

    @Test
    fun `invoke should reflect user changes in flow`() = runTest {
        // Given
        val user1 = User(uid = "user1", email = "user1@example.com", displayName = "User 1")
        val user2 = User(uid = "user2", email = "user2@example.com", displayName = "User 2")
        val userFlow = MutableStateFlow<User?>(user1)
        every { authRepository.currentUser } returns userFlow

        // When
        val result1 = getCurrentUserUseCase().first()
        userFlow.value = user2
        val result2 = getCurrentUserUseCase().first()

        // Then
        assertEquals(user1, result1)
        assertEquals(user2, result2)
    }
}
