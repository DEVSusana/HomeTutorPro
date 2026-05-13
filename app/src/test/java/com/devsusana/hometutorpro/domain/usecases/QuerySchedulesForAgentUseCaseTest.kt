package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.data.models.AgentScheduleDetail
import com.devsusana.hometutorpro.data.models.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.usecases.implementations.QuerySchedulesForAgentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [QuerySchedulesForAgentUseCase].
 *
 * Verifies that each method correctly delegates to [AgentContextRepository]
 * and returns the repository's response unmodified.
 */
class QuerySchedulesForAgentUseCaseTest {

    private val repository: com.devsusana.hometutorpro.domain.repository.AgentContextRepository = mockk()
    private val useCase = QuerySchedulesForAgentUseCase(repository)

    @Test
    fun `getAllSchedules should delegate to repository and return its result`() = runTest {
        // Given
        val expected = listOf(AgentScheduleSummary("María", 1, "09:00", "10:00"))
        coEvery { repository.getAllSchedules() } returns expected

        // When
        val result = useCase.getAllSchedules()

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.getAllSchedules() }
    }

    @Test
    fun `getScheduleDetails should delegate to repository and return its result`() = runTest {
        // Given
        val expected = listOf(
            AgentScheduleDetail("sched1", "student1", "María", 1, "09:00", "10:00")
        )
        coEvery { repository.getScheduleDetails() } returns expected

        // When
        val result = useCase.getScheduleDetails()

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.getScheduleDetails() }
    }

    @Test
    fun `getSchedulesByStudentName should pass name to repository and return its result`() = runTest {
        // Given
        val studentName = "María"
        val expected = listOf(
            AgentScheduleDetail("sched1", "student1", "María García", 1, "09:00", "10:00")
        )
        coEvery { repository.getSchedulesByStudentName(studentName) } returns expected

        // When
        val result = useCase.getSchedulesByStudentName(studentName)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.getSchedulesByStudentName(studentName) }
    }

    @Test
    fun `getSchedulesByStudentName should return empty list when no match found`() = runTest {
        // Given
        coEvery { repository.getSchedulesByStudentName(any()) } returns emptyList()

        // When
        val result = useCase.getSchedulesByStudentName("NonExistent")

        // Then
        assertEquals(emptyList<AgentScheduleDetail>(), result)
    }
}
