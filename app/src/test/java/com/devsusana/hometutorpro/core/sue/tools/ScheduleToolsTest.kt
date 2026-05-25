package com.devsusana.hometutorpro.core.sue.tools

import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.usecases.IManageScheduleForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IQuerySchedulesForAgentUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ScheduleTools].
 *
 * All dependencies ([IQuerySchedulesForAgentUseCase], [IManageScheduleForAgentUseCase],
 * [SecureAuthManager]) are mocked so tests run in complete isolation.
 */
class ScheduleToolsTest {

    private lateinit var querySchedulesUseCase: IQuerySchedulesForAgentUseCase
    private lateinit var manageScheduleUseCase: IManageScheduleForAgentUseCase
    private lateinit var secureAuthManager: SecureAuthManager
    private lateinit var scheduleTools: ScheduleTools

    // AgentScheduleSummary — used by getAllSchedules() (no IDs needed)
    private val mondaySchedule = AgentScheduleSummary(
        studentName = "María",
        dayOfWeek = 1, // Monday
        startTime = "10:00",
        endTime = "11:00"
    )

    private val wednesdaySchedule = AgentScheduleSummary(
        studentName = "Juan",
        dayOfWeek = 3, // Wednesday
        startTime = "16:00",
        endTime = "17:00"
    )

    // AgentScheduleDetail — used by getSchedulesByStudentName() (includes IDs for actions)
    private val mondayScheduleDetail = AgentScheduleDetail(
        scheduleId = "sched-1",
        studentId = "stu-1",
        studentName = "María",
        dayOfWeek = 1,
        startTime = "10:00",
        endTime = "11:00"
    )

    @Before
    fun setup() {
        querySchedulesUseCase = mockk()
        manageScheduleUseCase = mockk()
        secureAuthManager = mockk()

        scheduleTools = ScheduleTools(
            querySchedulesUseCase = querySchedulesUseCase,
            manageScheduleUseCase = manageScheduleUseCase,
            secureAuthManager = secureAuthManager
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getWeeklySchedule
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `getWeeklySchedule returns WeeklySchedule result with all schedules`() = runTest {
        coEvery { querySchedulesUseCase.getAllSchedules() } returns listOf(mondaySchedule, wednesdaySchedule)

        val result = scheduleTools.getWeeklySchedule()

        assertTrue(result is SueOperationResult.WeeklySchedule)
        val weekly = result as SueOperationResult.WeeklySchedule
        assertEquals(2, weekly.schedules.size)
    }

    @Test
    fun `getWeeklySchedule returns empty WeeklySchedule when no schedules exist`() = runTest {
        coEvery { querySchedulesUseCase.getAllSchedules() } returns emptyList()

        val result = scheduleTools.getWeeklySchedule()

        assertTrue(result is SueOperationResult.WeeklySchedule)
        assertTrue((result as SueOperationResult.WeeklySchedule).schedules.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getScheduleForDay
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `getScheduleForDay filters by day and returns DaySchedule`() = runTest {
        coEvery { querySchedulesUseCase.getAllSchedules() } returns listOf(mondaySchedule, wednesdaySchedule)

        val result = scheduleTools.getScheduleForDay(dayOfWeek = 1, timeFilter = null)

        assertTrue(result is SueOperationResult.DaySchedule)
        val dayResult = result as SueOperationResult.DaySchedule
        assertEquals(1, dayResult.schedules.size)
        assertEquals("María", dayResult.schedules.first().studentName)
    }

    @Test
    fun `getScheduleForDay with morning filter excludes afternoon schedules`() = runTest {
        coEvery { querySchedulesUseCase.getAllSchedules() } returns listOf(mondaySchedule, wednesdaySchedule)

        // Wednesday schedule is at 16:00 (afternoon) — morning filter should exclude it
        val result = scheduleTools.getScheduleForDay(dayOfWeek = 3, timeFilter = "morning")

        assertTrue(result is SueOperationResult.DaySchedule)
        assertTrue((result as SueOperationResult.DaySchedule).schedules.isEmpty())
    }

    @Test
    fun `getScheduleForDay with afternoon filter includes afternoon schedules`() = runTest {
        coEvery { querySchedulesUseCase.getAllSchedules() } returns listOf(mondaySchedule, wednesdaySchedule)

        val result = scheduleTools.getScheduleForDay(dayOfWeek = 3, timeFilter = "afternoon")

        assertTrue(result is SueOperationResult.DaySchedule)
        assertEquals(1, (result as SueOperationResult.DaySchedule).schedules.size)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getFreeSlots
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `getFreeSlots returns days 1-5 that have no schedule`() = runTest {
        // Only Monday (1) and Wednesday (3) are scheduled
        coEvery { querySchedulesUseCase.getAllSchedules() } returns listOf(mondaySchedule, wednesdaySchedule)

        val result = scheduleTools.getFreeSlots()

        assertTrue(result is SueOperationResult.FreeSlots)
        val free = (result as SueOperationResult.FreeSlots).freeDays
        // Tuesday (2), Thursday (4), Friday (5) should be free
        assertEquals(listOf(2, 4, 5), free)
    }

    @Test
    fun `getFreeSlots returns empty list when every weekday is scheduled`() = runTest {
        val allDays = (1..5).map { day ->
            AgentScheduleSummary(studentName = "Student$day", dayOfWeek = day, startTime = "09:00", endTime = "10:00")
        }
        coEvery { querySchedulesUseCase.getAllSchedules() } returns allDays

        val result = scheduleTools.getFreeSlots()

        assertTrue(result is SueOperationResult.FreeSlots)
        assertTrue((result as SueOperationResult.FreeSlots).freeDays.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareCancelAction
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareCancelAction returns Prepare Error when student schedule not found`() = runTest {
        coEvery { querySchedulesUseCase.getSchedulesByStudentName("Unknown") } returns emptyList()

        val result = scheduleTools.prepareCancelAction("Unknown", dayOfWeek = 1)

        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.CLASS_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `prepareCancelAction returns Prepare Success with pre-resolved action`() = runTest {
        coEvery { querySchedulesUseCase.getSchedulesByStudentName("María") } returns listOf(mondayScheduleDetail)

        val result = scheduleTools.prepareCancelAction("María", dayOfWeek = 1)

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action
        assertTrue(action is SuePendingAction.CancelClass)
        assertEquals("María", (action as SuePendingAction.CancelClass).studentName)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // executeCancelAction
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `executeCancelAction returns AuthError when no session found`() = runTest {
        every { secureAuthManager.getUserId() } returns null

        val action = SuePendingAction.CancelClass(
            studentName = "María", studentId = "stu-1", scheduleId = "sched-1",
            date = System.currentTimeMillis(), startTime = "10:00", endTime = "11:00"
        )

        val result = scheduleTools.executeCancelAction(action)

        assertEquals(SueOperationResult.Execute.AuthError, result)
    }

    @Test
    fun `executeCancelAction returns Execute Success on successful cancellation`() = runTest {
        every { secureAuthManager.getUserId() } returns "prof-1"
        coEvery {
            manageScheduleUseCase.cancelClass(any(), any(), any(), any())
        } returns Result.Success(Unit)

        val action = SuePendingAction.CancelClass(
            studentName = "María", studentId = "stu-1", scheduleId = "sched-1",
            date = System.currentTimeMillis(), startTime = "10:00", endTime = "11:00"
        )

        val result = scheduleTools.executeCancelAction(action)

        assertTrue(result is SueOperationResult.Execute.Success)
        assertEquals(action, (result as SueOperationResult.Execute.Success).action)
    }

    @Test
    fun `executeCancelAction returns Execute Error on domain error`() = runTest {
        every { secureAuthManager.getUserId() } returns "prof-1"
        coEvery {
            manageScheduleUseCase.cancelClass(any(), any(), any(), any())
        } returns Result.Error(DomainError.Unknown)

        val action = SuePendingAction.CancelClass(
            studentName = "María", studentId = "stu-1", scheduleId = "sched-1",
            date = System.currentTimeMillis(), startTime = "10:00", endTime = "11:00"
        )

        val result = scheduleTools.executeCancelAction(action)

        assertTrue(result is SueOperationResult.Execute.Error)
        assertEquals(DomainError.Unknown, (result as SueOperationResult.Execute.Error).domainError)
    }
}
