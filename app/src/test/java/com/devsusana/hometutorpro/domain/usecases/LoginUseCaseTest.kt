package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.LoginUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val loginUseCase = LoginUseCase(repository)

    @Test
    fun `invoke returns Success when repository login succeeds`() = runTest {
        val email = "test@example.com"
        val password = "password"
        val user = User("123", email, "Test User")
        coEvery { repository.login(email, password) } returns Result.Success(user)

        val result = loginUseCase(email, password)

        assert(result is Result.Success)
        assertEquals(user, (result as Result.Success).data)
    }

    @Test
    fun `invoke returns Error when repository login fails`() = runTest {
        val email = "test@example.com"
        val password = "wrong"
        val error = DomainError.UnknownError
        coEvery { repository.login(email, password) } returns Result.Error(error)

        val result = loginUseCase(email, password)

        assert(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
    }
}
