package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.AgentBalanceSummary
import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class SueScheduleIntentHandlerTest {

    private lateinit var scheduleTools: ScheduleTools
    private lateinit var studentTools: StudentTools
    private lateinit var authRepository: AuthRepository
    private lateinit var dateTimeProvider: DateTimeProvider
    private lateinit var entityExtractor: SueEntityExtractor
    private lateinit var context: SueConversationContext
    private lateinit var handler: SueScheduleIntentHandler

    @Before
    fun setUp() {
        scheduleTools = mockk(relaxed = true)
        studentTools = mockk(relaxed = true)
        authRepository = mockk {
            every { currentUser } returns MutableStateFlow(
                User(uid = "1", email = "p@t.com", displayName = "Prof", workingStartTime = "09:00", workingEndTime = "21:00")
            )
        }
        dateTimeProvider = mockk {
            every { getNow() } returns LocalDateTime.of(2025, 9, 16, 10, 0) // Tuesday (2)
            every { getLocale() } returns Locale.forLanguageTag("es-ES")
        }
        entityExtractor = SueEntityExtractor(dateTimeProvider, authRepository)
        context = SueConversationContext()
        handler = SueScheduleIntentHandler(
            scheduleTools,
            studentTools,
            authRepository,
            dateTimeProvider,
            entityExtractor,
            context
        )
    }

    @Test
    fun `handleStartClass handles missing slots and prepares class start`() = runTest {
        assertTrue(handler.handleStartClass() is SueOperationResult.Prepare.Error)
        context.lastMentionedStudentName = "Ana"
        assertTrue(handler.handleStartClass() is SueOperationResult.Prepare.Error)
        context.lastMentionedDuration = 60
        coEvery { studentTools.prepareStartClass("Ana", 60) } returns SueOperationResult.Prepare.Success(
            SuePendingAction.StartClass("1", "Ana", 60)
        )
        assertTrue(handler.handleStartClass() is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `handleAddExtraClass and handleCreateSchedule cover missing and valid slots`() = runTest {
        assertTrue(handler.handleAddExtraClass("clase extra") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleCreateSchedule("crea horario") is SueOperationResult.Prepare.Error)

        context.lastMentionedStudentName = "Ana"
        assertTrue(handler.handleAddExtraClass("clase extra para ana") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleCreateSchedule("crea horario para ana") is SueOperationResult.Prepare.Error)

        context.lastMentionedDayOfWeek = 3
        assertTrue(handler.handleAddExtraClass("clase extra el miercoles") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleCreateSchedule("crea horario el miercoles") is SueOperationResult.Prepare.Error)

        context.lastMentionedTime = "17:00"
        coEvery { scheduleTools.prepareAddExtraClass("Ana", any(), "17:00", "18:00") } returns SueOperationResult.Prepare.Success(
            SuePendingAction.AddExtraClass("Ana", "1", 1000L, "17:00", "18:00")
        )
        coEvery { scheduleTools.prepareCreateSchedule("Ana", 3, "17:00", "18:00") } returns SueOperationResult.Prepare.Success(
            SuePendingAction.CreateSchedule("Ana", "1", 3, "17:00", "18:00")
        )

        assertTrue(handler.handleAddExtraClass("a las 17:00") is SueOperationResult.Prepare.Success)
        assertTrue(handler.handleCreateSchedule("a las 17:00") is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `handleDeleteSchedule and handleCancelClass resolve single vs multiple schedules`() = runTest {
        assertTrue(handler.handleDeleteSchedule() is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleCancelClass("cancela") is SueOperationResult.Prepare.Error)

        context.lastMentionedStudentName = "Ana"
        assertTrue(handler.handleDeleteSchedule() is SueOperationResult.Prepare.Error)

        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns listOf(
            AgentScheduleDetail("s1", "1", "Ana", 1, "17:00", "18:00"),
            AgentScheduleDetail("s2", "1", "Ana", 4, "18:00", "19:00")
        )
        val ambiguousCancel = handler.handleCancelClass("cancela la clase de ana") as SueOperationResult.Prepare.Error
        assertEquals(SueOperationResult.ErrorType.CLASS_NOT_FOUND, ambiguousCancel.errorType)

        context.lastMentionedDayOfWeek = 1
        coEvery { scheduleTools.prepareDeleteSchedule("Ana", 1, null) } returns SueOperationResult.Prepare.Success(
            SuePendingAction.DeleteSchedule("Ana", "1", "s1", 1, "17:00")
        )
        coEvery { scheduleTools.prepareCancelAction("Ana", 1, null, false) } returns SueOperationResult.Prepare.Success(
            SuePendingAction.CancelClass("Ana", "1", "s1", 1000L, "17:00", "18:00")
        )
        assertTrue(handler.handleDeleteSchedule() is SueOperationResult.Prepare.Success)
        assertTrue(handler.handleCancelClass("cancela la del lunes") is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `handleRescheduleClass handles no schedules and two-day rescheduling`() = runTest {
        assertTrue(handler.handleRescheduleClass("mueve clase") is SueOperationResult.Prepare.Error)

        context.lastMentionedStudentName = "Ana"
        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns emptyList()
        val noClass = handler.handleRescheduleClass("mueve la clase de ana") as SueOperationResult.Prepare.Error
        assertEquals(SueOperationResult.ErrorType.CLASS_NOT_FOUND, noClass.errorType)

        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns listOf(
            AgentScheduleDetail("s1", "1", "Ana", 2, "17:00", "18:00")
        )
        coEvery { scheduleTools.prepareRescheduleAction("Ana", 2, 4, "19:00", "17:00") } returns SueOperationResult.Prepare.Success(
            SuePendingAction.RescheduleClass("Ana", "1", "s1", 1000L, "17:00", DayOfWeek.THURSDAY, 2000L, "19:00", "20:00")
        )
        val ok = handler.handleRescheduleClass("pasa la clase del martes a las 17:00 al jueves a las 19:00")
        assertTrue(ok is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `query handlers cover next class, free slots, day schedule, today summary with debt alert, and cancelled classes`() = runTest {
        val summary = AgentScheduleSummary("Ana", 2, "17:00", "18:00")
        coEvery { scheduleTools.getNextClass(any()) } returns SueOperationResult.NextClass(summary, LocalDate.of(2025, 9, 16))
        val next = handler.handleQueryNextClass()
        assertTrue(next is SueOperationResult.NextClass)
        assertEquals("Ana", context.lastMentionedStudentName)

        coEvery { scheduleTools.getFreeSlots("09:00", "21:00", 2) } returns SueOperationResult.FreeSlots(listOf(3, 5))
        assertTrue(handler.handleQueryFreeSlots() is SueOperationResult.FreeSlots)

        coEvery { scheduleTools.getScheduleForDay(2, null) } returns SueOperationResult.DaySchedule(2, null, listOf(summary))
        assertTrue(handler.handleQueryDaySchedule("que tengo hoy") is SueOperationResult.DaySchedule)

        coEvery { studentTools.getStudentsWithBalance() } returns SueOperationResult.StudentsWithBalance(
            listOf(AgentBalanceSummary("Ana", 30.0))
        )
        val today = handler.handleQueryTodaySummary() as SueOperationResult.ReadSuccess
        assertTrue(today.message.contains("saldo pendiente y clase hoy"))

        coEvery { scheduleTools.getCancelledClassesDescription(2, "Ana", ExceptionType.CANCELLED) } returns ""
        val noCancelled = handler.handleQueryCancelledClasses("clases canceladas de ana hoy", "Ana", 2, null) as SueOperationResult.ReadSuccess
        assertTrue(noCancelled.message.contains("clases canceladas registradas"))
        coVerify { scheduleTools.getCancelledClassesDescription(2, "Ana", ExceptionType.CANCELLED) }
    }
}
