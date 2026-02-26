package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.RescueOrphanedDataUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RescueOrphanedDataUseCaseTest {

    private val studentRepository: StudentRepository = mockk()
    private val getCurrentUserUseCase: IGetCurrentUserUseCase = mockk()
    private val useCase = RescueOrphanedDataUseCase(studentRepository, getCurrentUserUseCase)

    @Test
    fun `invoke should rescue data when user is available`() = runTest {
        val user = User(uid = "prof1", email = "test@example.com", displayName = "Test")
        val userFlow = MutableStateFlow<User?>(user)

        every { getCurrentUserUseCase.invoke() } returns userFlow
        coEvery { studentRepository.rescueOrphanedData("prof1") } returns com.devsusana.hometutorpro.domain.core.Result.Success(Unit)

        useCase()

        coVerify(exactly = 1) { studentRepository.rescueOrphanedData("prof1") }
    }

    @Test
    fun `invoke should do nothing when user is missing`() = runTest {
        val userFlow = MutableStateFlow<User?>(null)

        every { getCurrentUserUseCase.invoke() } returns userFlow

        useCase()

        coVerify(exactly = 0) { studentRepository.rescueOrphanedData(any()) }
    }
}
