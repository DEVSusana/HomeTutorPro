package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
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
    private lateinit var exceptionRepository: com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
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
        exceptionRepository = mockk()
        authRepository = mockk()
        dateTimeProvider = mockk(relaxed = true)

        every { dateTimeProvider.getNow() } returns LocalDateTime.of(2026, 5, 27, 8, 35) // Wednesday
        every { dateTimeProvider.getLocale() } returns Locale.US
        coEvery { queryStudentsUseCase.searchByName(any()) } returns listOf(mariaStudentDetail)

        scheduleTools = ScheduleTools(
            querySchedulesUseCase = querySchedulesUseCase,
            manageScheduleUseCase = manageScheduleUseCase,
            queryStudentsUseCase = queryStudentsUseCase,
            saveScheduleUseCase = saveScheduleUseCase,
            deleteScheduleUseCase = deleteScheduleUseCase,
            saveScheduleExceptionUseCase = saveScheduleExceptionUseCase,
            exceptionRepository = exceptionRepository,
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
    fun `getFreeSlots returns days 1-5 that have no schedule as fully free`() = runTest {
        coEvery { querySchedulesUseCase.getScheduleDetails() } returns listOf(
            mondayScheduleDetail,
            AgentScheduleDetail("s-wed", "stu-2", "Juan", 3, "16:00", "17:00")
        )
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        coEvery { exceptionRepository.getAllExceptions("prof-1") } returns emptyList()

        val result = scheduleTools.getFreeSlots(workingStart = "08:00", workingEnd = "11:00")

        assertTrue(result is SueOperationResult.FreeSlotsDetailed)
        val detailed = result as SueOperationResult.FreeSlotsDetailed
        // Tuesday (2), Thursday (4) and Friday (5) have no classes at all
        assertTrue(detailed.freeDays.containsAll(listOf(2, 4, 5)))
    }

    @Test
    fun `getFreeSlots detects intra-day gap between two classes`() = runTest {
        // Monday has class 08:00-09:00 and 10:30-11:00 → gap 09:00-10:30 (90 min)
        val cls1 = AgentScheduleDetail("s1", "stu-1", "Alice", 1, "08:00", "09:00")
        val cls2 = AgentScheduleDetail("s2", "stu-2", "Bob",   1, "10:30", "11:00")
        coEvery { querySchedulesUseCase.getScheduleDetails() } returns listOf(cls1, cls2)
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        coEvery { exceptionRepository.getAllExceptions("prof-1") } returns emptyList()

        val result = scheduleTools.getFreeSlots(workingStart = "08:00", workingEnd = "11:00", dayOfWeek = 1)

        assertTrue(result is SueOperationResult.FreeSlotsDetailed)
        val gaps = (result as SueOperationResult.FreeSlotsDetailed).gapLines
        assertTrue(gaps.any { it.contains("09:00") && it.contains("10:30") })
    }

    @Test
    fun `getFreeSlots treats cancelled classes as free time`() = runTest {
        // Thursday has class 10:00-11:00 but it is CANCELLED -> whole 08:00-11:00 is free
        val cls = AgentScheduleDetail("s1", "stu-1", "Alice", 4, "10:00", "11:00")
        coEvery { querySchedulesUseCase.getScheduleDetails() } returns listOf(cls)
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        // Next Thursday millis
        val targetThursdayMillis = java.time.LocalDateTime.of(2026, 5, 28, 10, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        coEvery { exceptionRepository.getAllExceptions("prof-1") } returns listOf(
            ScheduleException(
                id = "exc-1",
                studentId = "stu-1",
                professorId = "prof-1",
                date = targetThursdayMillis,
                type = ExceptionType.CANCELLED
            )
        )

        val result = scheduleTools.getFreeSlots(workingStart = "08:00", workingEnd = "11:00", dayOfWeek = 4)

        assertTrue(result is SueOperationResult.FreeSlotsDetailed)
        val detailed = result as SueOperationResult.FreeSlotsDetailed
        assertTrue(detailed.freeDays.contains(4))
    }

    @Test
    fun `getFreeSlots includes reasons for cancelled classes in gap lines`() = runTest {
        // Monday has class 08:00-09:00 (active), 09:30-10:30 (cancelled Alice), 11:00-12:00 (active)
        val cls1 = AgentScheduleDetail("s1", "stu-1", "Alice", 1, "09:30", "10:30")
        val cls2 = AgentScheduleDetail("s2", "stu-2", "Bob",   1, "08:00", "09:00")
        val cls3 = AgentScheduleDetail("s3", "stu-3", "Charlie", 1, "11:00", "12:00")
        coEvery { querySchedulesUseCase.getScheduleDetails() } returns listOf(cls1, cls2, cls3)
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        val targetMondayMillis = java.time.LocalDateTime.of(2026, 6, 1, 9, 30)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        coEvery { exceptionRepository.getAllExceptions("prof-1") } returns listOf(
            ScheduleException(
                id = "exc-1",
                studentId = "stu-1",
                professorId = "prof-1",
                date = targetMondayMillis,
                type = ExceptionType.CANCELLED
            )
        )

        val result = scheduleTools.getFreeSlots(workingStart = "08:00", workingEnd = "12:00", dayOfWeek = 1)

        assertTrue(result is SueOperationResult.FreeSlotsDetailed)
        val gaps = (result as SueOperationResult.FreeSlotsDetailed).gapLines
        assertTrue(gaps.any { it.contains("09:00") && it.contains("11:00") && it.contains("libre por cancelación de Alice") })
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

    // ──────────────────────────────────────────────────────────────────────────
    // getCancelledClassesDescription
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `getCancelledClassesDescription returns formatted cancellations`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        coEvery { exceptionRepository.getAllExceptions("prof-1") } returns listOf(
            ScheduleException(
                id = "exc-1",
                studentId = "stu-1",
                professorId = "prof-1",
                date = 1787700000000L,
                type = ExceptionType.CANCELLED
            )
        )
        coEvery { querySchedulesUseCase.getScheduleDetails() } returns listOf(mondayScheduleDetail)

        val result = scheduleTools.getCancelledClassesDescription()

        assertTrue(result.contains("Clases canceladas"))
        assertTrue(result.contains("María"))
        assertTrue(result.contains("cancelada"))
    }

    @Test
    fun `getCancelledClassesDescription with dayOfWeek filters by upcoming date of that weekday`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        // dateTimeProvider is Wednesday 2026-05-27. Next Thursday (dayOfWeek=4) is 2026-05-28.
        val targetThursdayMillis = java.time.LocalDateTime.of(2026, 5, 28, 10, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pastThursdayMillis = java.time.LocalDateTime.of(2026, 2, 26, 10, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        coEvery { exceptionRepository.getAllExceptions("prof-1") } returns listOf(
            ScheduleException(
                id = "exc-past",
                studentId = "stu-1",
                professorId = "prof-1",
                date = pastThursdayMillis,
                type = ExceptionType.CANCELLED
            ),
            ScheduleException(
                id = "exc-target",
                studentId = "stu-1",
                professorId = "prof-1",
                date = targetThursdayMillis,
                type = ExceptionType.CANCELLED
            )
        )
        coEvery { querySchedulesUseCase.getScheduleDetails() } returns listOf(mondayScheduleDetail)

        // Request specifically for Thursday (day 4)
        val result = scheduleTools.getCancelledClassesDescription(dayOfWeek = 4)

        assertTrue(result.contains("28/05/2026"))
        assertTrue(!result.contains("26/02/2026"))
    }

    @Test
    fun `getCancelledClassesDescription with studentNameFilter filters by student name`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        val juanStudentDetail = AgentStudentDetail(
            studentId = "stu-2",
            name = "Juan",
            subjects = "Inglés",
            course = "1º",
            pendingBalance = 0.0
        )
        coEvery { queryStudentsUseCase.searchByName("") } returns listOf(mariaStudentDetail, juanStudentDetail)

        val targetThursdayMillis = java.time.LocalDateTime.of(2026, 5, 28, 10, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        coEvery { exceptionRepository.getAllExceptions("prof-1") } returns listOf(
            ScheduleException(
                id = "exc-maria",
                studentId = "stu-1",
                professorId = "prof-1",
                date = targetThursdayMillis,
                type = ExceptionType.CANCELLED
            ),
            ScheduleException(
                id = "exc-juan",
                studentId = "stu-2",
                professorId = "prof-1",
                date = targetThursdayMillis,
                type = ExceptionType.CANCELLED
            )
        )
        coEvery { querySchedulesUseCase.getScheduleDetails() } returns listOf(mondayScheduleDetail)

        val result = scheduleTools.getCancelledClassesDescription(studentNameFilter = "Juan")

        assertTrue(result.contains("Juan"))
        assertTrue(!result.contains("María"))
    }

    @Test
    fun `getCancelledClassesDescription returns empty string when no exceptions`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(mockUser)
        coEvery { exceptionRepository.getAllExceptions("prof-1") } returns emptyList()

        val result = scheduleTools.getCancelledClassesDescription()

        assertEquals("", result)
    }
}

