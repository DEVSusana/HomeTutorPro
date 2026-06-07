package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.IManageScheduleForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IQuerySchedulesForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IQueryStudentsForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleExceptionUseCase
import java.time.LocalDateTime
import java.util.Locale
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ScheduleTools].
 *
 * All dependencies are mocked so tests run in complete isolation.
 */
class ScheduleToolsTest {

    private lateinit var querySchedulesUseCase: IQuerySchedulesForAgentUseCase
    private lateinit var manageScheduleUseCase: IManageScheduleForAgentUseCase
    private lateinit var queryStudentsUseCase: IQueryStudentsForAgentUseCase
    private lateinit var saveScheduleUseCase: ISaveScheduleUseCase
    private lateinit var deleteScheduleUseCase: IDeleteScheduleUseCase
    private lateinit var saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var dateTimeProvider: DateTimeProvider
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

    private val mockUser = com.devsusana.hometutorpro.domain.entities.User(
        uid = "prof-1",
        email = "prof@example.com",
        displayName = "Professor",
        workingStartTime = "08:00",
        workingEndTime = "23:00",
        notes = ""
    )

    private val mariaStudentDetail = AgentStudentDetail(
        studentId = "stu-1",
        name = "María",
        subjects = "Mates",
        course = "ESO",
        pendingBalance = 0.0
    )

    @Before
    fun setup() {
        querySchedulesUseCase = mockk()
        manageScheduleUseCase = mockk()
        queryStudentsUseCase = mockk()
        saveScheduleUseCase = mockk()
        deleteScheduleUseCase = mockk()
        saveScheduleExceptionUseCase = mockk()
        authRepository = mockk()
        dateTimeProvider = mockk(relaxed = true)

        every { dateTimeProvider.getNow() } returns LocalDateTime.of(2026, 5, 27, 8, 35) // Wednesday
        every { dateTimeProvider.getLocale() } returns Locale.US

        scheduleTools = ScheduleTools(
            querySchedulesUseCase = querySchedulesUseCase,
            manageScheduleUseCase = manageScheduleUseCase,
            queryStudentsUseCase = queryStudentsUseCase,
            saveScheduleUseCase = saveScheduleUseCase,
            deleteScheduleUseCase = deleteScheduleUseCase,
            saveScheduleExceptionUseCase = saveScheduleExceptionUseCase,
            authRepository = authRepository,
            dateTimeProvider = dateTimeProvider
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

    // ──────────────────────────────────────────────────────────────────────────
    // getFreeSlots
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `getFreeSlots returns days 1-5 that have no schedule`() = runTest {
        coEvery { querySchedulesUseCase.getAllSchedules() } returns listOf(mondaySchedule, wednesdaySchedule)

        val result = scheduleTools.getFreeSlots()

        assertTrue(result is SueOperationResult.FreeSlots)
        val free = (result as SueOperationResult.FreeSlots).freeDays
        assertEquals(listOf(2, 4, 5), free)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareCancelAction with time filter
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareCancelAction returns Prepare Success matching exact time`() = runTest {
        val s1 = AgentScheduleDetail("sched-1", "stu-1", "María", 1, "10:00", "11:00")
        val s2 = AgentScheduleDetail("sched-2", "stu-1", "María", 1, "17:00", "18:00")
        coEvery { querySchedulesUseCase.getSchedulesByStudentName("María") } returns listOf(s1, s2)

        val result = scheduleTools.prepareCancelAction("María", dayOfWeek = 1, time = "17:00")

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.CancelClass
        assertEquals("sched-2", action.scheduleId)
        assertEquals("17:00", action.startTime)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // executeCancelAction
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `executeCancelAction returns Execute Success on successful cancellation`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        coEvery {
            manageScheduleUseCase.cancelClass(any(), any(), any(), any())
        } returns Result.Success(Unit)

        val action = SuePendingAction.CancelClass(
            studentName = "María", studentId = "stu-1", scheduleId = "sched-1",
            date = System.currentTimeMillis(), startTime = "10:00", endTime = "11:00"
        )

        val result = scheduleTools.executeCancelAction(action)

        assertTrue(result is SueOperationResult.Execute.Success)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareCreateSchedule & executeCreateSchedule
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareCreateSchedule returns Success when student exists`() = runTest {
        coEvery { queryStudentsUseCase.searchByName("María") } returns listOf(mariaStudentDetail)

        val result = scheduleTools.prepareCreateSchedule("María", 1, "17:00", "18:00")

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.CreateSchedule
        assertEquals("stu-1", action.studentId)
        assertEquals(1, action.dayOfWeek)
        assertEquals("17:00", action.startTime)
    }

    @Test
    fun `executeCreateSchedule saves a permanent schedule`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        coEvery { saveScheduleUseCase("prof-1", "stu-1", any()) } returns Result.Success(Unit)

        val action = SuePendingAction.CreateSchedule("María", "stu-1", 1, "17:00", "18:00")

        val result = scheduleTools.executeCreateSchedule(action)

        assertTrue(result is SueOperationResult.Execute.Success)
        coVerify { saveScheduleUseCase("prof-1", "stu-1", withArg {
            assertEquals("stu-1", it.studentId)
            assertEquals(java.time.DayOfWeek.MONDAY, it.dayOfWeek)
            assertEquals("17:00", it.startTime)
            assertEquals("18:00", it.endTime)
        })}
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareDeleteSchedule & executeDeleteSchedule
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareDeleteSchedule returns Success when class exists`() = runTest {
        coEvery { querySchedulesUseCase.getSchedulesByStudentName("María") } returns listOf(mondayScheduleDetail)

        val result = scheduleTools.prepareDeleteSchedule("María", 1, "10:00")

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.DeleteSchedule
        assertEquals("sched-1", action.scheduleId)
    }

    @Test
    fun `executeDeleteSchedule invokes deleteScheduleUseCase`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        coEvery { deleteScheduleUseCase("prof-1", "stu-1", "sched-1") } returns Result.Success(Unit)

        val action = SuePendingAction.DeleteSchedule("María", "stu-1", "sched-1", 1, "10:00")

        val result = scheduleTools.executeDeleteSchedule(action)

        assertTrue(result is SueOperationResult.Execute.Success)
        coVerify { deleteScheduleUseCase("prof-1", "stu-1", "sched-1") }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareAddExtraClass & executeAddExtraClass
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareAddExtraClass returns Success when student exists`() = runTest {
        coEvery { queryStudentsUseCase.searchByName("María") } returns listOf(mariaStudentDetail)

        val result = scheduleTools.prepareAddExtraClass("María", 123456L, "17:00", "18:00")

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("stu-1", action.studentId)
        assertEquals(123456L, action.date)
    }

    @Test
    fun `executeAddExtraClass saves exception of type EXTRA`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        coEvery { saveScheduleExceptionUseCase("prof-1", "stu-1", any()) } returns Result.Success(Unit)

        val action = SuePendingAction.AddExtraClass("María", "stu-1", 123456789L, "17:00", "18:00")

        val result = scheduleTools.executeAddExtraClass(action)

        assertTrue(result is SueOperationResult.Execute.Success)
        coVerify { saveScheduleExceptionUseCase("prof-1", "stu-1", withArg {
            assertEquals("stu-1", it.studentId)
            assertEquals("EXTRA", it.originalScheduleId)
            assertEquals(ExceptionType.EXTRA, it.type)
            assertEquals(123456789L, it.date)
            assertEquals("17:00", it.newStartTime)
            assertEquals("18:00", it.newEndTime)
        })}
    }
}

