package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.AgentBalanceSummary
import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * Benchmark evaluation test suite for SUE conversational natural language understanding.
 *
 * Verifies that realistic, day-to-day Spanish expressions used by private tutors
 * are accurately routed to the expected actions and data queries.
 */
class SuePromptEvaluationTest {

    private lateinit var studentTools: StudentTools
    private lateinit var scheduleTools: ScheduleTools
    private lateinit var dateTimeProvider: DateTimeProvider
    private lateinit var authRepository: AuthRepository
    private lateinit var agent: SueAgentImpl

    // Fixed test timestamp: Tuesday, 16 September 2025 at 10:00 AM (Tuesday = ISO day 2)
    private val fixedNow = LocalDateTime.of(2025, 9, 16, 10, 0)

    // Standard mock students
    private val lucia = AgentStudentDetail(
        studentId = "stud_1",
        name = "Lucía Moreno García",
        subjects = "Matemáticas, Física",
        course = "4º ESO",
        pendingBalance = -30.0
    )

    private val carlos = AgentStudentDetail(
        studentId = "stud_2",
        name = "Carlos Hernández",
        subjects = "Química",
        course = "1º Bachillerato",
        pendingBalance = 0.0
    )

    private val maria = AgentStudentDetail(
        studentId = "stud_3",
        name = "María Santillana",
        subjects = "Lengua y Literatura",
        course = "2º Bachillerato",
        pendingBalance = -50.0
    )

    private val pedro = AgentStudentDetail(
        studentId = "stud_4",
        name = "Pedro Gómez",
        subjects = "Inglés",
        course = "3º ESO",
        pendingBalance = 20.0
    )

    private val sofia = AgentStudentDetail(
        studentId = "stud_5",
        name = "Sofía Martín",
        subjects = "Biología y Geología",
        course = "1º ESO",
        pendingBalance = 0.0
    )

    private val marcos = AgentStudentDetail(
        studentId = "stud_6",
        name = "Marcos Ruiz",
        subjects = "Economía",
        course = "2º Bachillerato",
        pendingBalance = -15.0
    )

    private fun normalize(str: String): String {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
    }

    @Before
    fun setUp() {
        studentTools = mockk(relaxed = true)
        scheduleTools = mockk(relaxed = true)
        dateTimeProvider = mockk {
            every { getNow() } returns fixedNow
            every { getLocale() } returns Locale.forLanguageTag("es-ES")
        }
        authRepository = mockk {
            val user = User(
                uid = "prof_123",
                email = "prof@test.com",
                displayName = "Profesor",
                workingStartTime = "08:00",
                workingEndTime = "22:00"
            )
            every { currentUser } returns MutableStateFlow(user)
        }

        // Default: queries without explicit student names return null
        coEvery { studentTools.extractRelevantStudent(any()) } returns null
        coEvery { scheduleTools.getSchedulesByStudentName(any()) } returns emptyList()

        // Accent-insensitive student resolution for names in user queries
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("lucia") }) } returns lucia
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("carlos") }) } returns carlos
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("maria") }) } returns maria
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("pedro") }) } returns pedro
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("sofia") }) } returns sofia
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("marcos") }) } returns marcos
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("pepe") }) } returns AgentStudentDetail(
            studentId = "stud_7",
            name = "Pepe",
            subjects = "Matemáticas",
            course = "4º ESO",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("ana") }) } returns AgentStudentDetail(
            studentId = "stud_8",
            name = "Ana",
            subjects = "General",
            course = "1º Bachillerato",
            pendingBalance = 0.0
        )
        val hugo = AgentStudentDetail(
            studentId = "stud_9",
            name = "Hugo",
            subjects = "Física",
            course = "2º Bachillerato",
            pendingBalance = 0.0
        )
        val oscar = AgentStudentDetail(
            studentId = "stud_10",
            name = "Óscar",
            subjects = "Matemáticas",
            course = "1º Bachillerato",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("hugo") }) } returns hugo
        coEvery { studentTools.extractRelevantStudent(match { normalize(it).contains("oscar") }) } returns oscar

        // Accent-insensitive student schedule lookups for cancel/reschedule
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("lucia") }) } returns listOf(
            AgentScheduleDetail("sch_1", lucia.studentId, lucia.name, 2, "16:00", "17:00") // Tuesday
        )
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("sofia") }) } returns listOf(
            AgentScheduleDetail("sch_5", sofia.studentId, sofia.name, 2, "17:00", "18:00") // Tuesday
        )
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("carlos") }) } returns listOf(
            AgentScheduleDetail("sch_2", carlos.studentId, carlos.name, 4, "16:00", "17:00") // Thursday
        )
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("maria") }) } returns listOf(
            AgentScheduleDetail("sch_3", maria.studentId, maria.name, 4, "18:00", "19:00") // Thursday
        )
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("pedro") }) } returns listOf(
            AgentScheduleDetail("sch_4", pedro.studentId, pedro.name, 3, "17:00", "18:00") // Wednesday
        )
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("marcos") }) } returns listOf(
            AgentScheduleDetail("sch_6", marcos.studentId, marcos.name, 2, "16:00", "17:00") // Tuesday
        )
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("pepe") }) } returns listOf(
            AgentScheduleDetail("sch_7", "stud_7", "Pepe", 1, "17:00", "18:00") // Monday
        )
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("ana") }) } returns listOf(
            AgentScheduleDetail("sch_8", "stud_8", "Ana", 2, "17:00", "18:00") // Tuesday
        )
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("hugo") }) } returns listOf(
            AgentScheduleDetail("sch_9", "stud_9", "Hugo", 2, "20:00", "21:00") // Tuesday
        )
        coEvery { scheduleTools.getSchedulesByStudentName(match { normalize(it).contains("oscar") }) } returns listOf(
            AgentScheduleDetail("sch_10", "stud_10", "Óscar", 2, "17:00", "18:00")
        )

        agent = SueAgentImpl(
            studentTools = studentTools,
            scheduleTools = scheduleTools,
            dateTimeProvider = dateTimeProvider,
            authRepository = authRepository
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category 1: Schedule Queries (Today, Tomorrow, Specific Day, Next Class)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `evaluation - query today schedule with various natural expressions`() = runTest {
        val todayPhrases = listOf(
            "¿Qué clases tengo hoy?",
            "Dime mis clases de hoy",
            "¿Qué alumnos tengo hoy?",
            "Horario de hoy",
            "¿Qué tengo para hoy?"
        )

        val todayResult = SueOperationResult.DaySchedule(
            dayOfWeek = 2,
            timeFilter = null,
            schedules = listOf(AgentScheduleSummary(lucia.name, 2, "16:00", "17:00"))
        )
        coEvery { scheduleTools.getScheduleForDay(2, any()) } returns todayResult
        coEvery { scheduleTools.getScheduleForDay(2, null) } returns todayResult
        coEvery { scheduleTools.getScheduleForDay(2) } returns todayResult

        for (phrase in todayPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertEquals("Phrase '$phrase' should trigger today's schedule", todayResult, result)
        }
        coVerify(atLeast = todayPhrases.size) { scheduleTools.getScheduleForDay(2, any()) }
    }

    @Test
    fun `evaluation - query tomorrow schedule`() = runTest {
        val tomorrowPhrases = listOf(
            "¿Qué clases tengo mañana?",
            "Dime el horario de mañana",
            "¿Tengo clase mañana?"
        )

        val tomorrowResult = SueOperationResult.DaySchedule(
            dayOfWeek = 3,
            timeFilter = null,
            schedules = listOf(AgentScheduleSummary(pedro.name, 3, "17:00", "18:00"))
        )
        coEvery { scheduleTools.getScheduleForDay(3, any()) } returns tomorrowResult
        coEvery { scheduleTools.getScheduleForDay(3, null) } returns tomorrowResult
        coEvery { scheduleTools.getScheduleForDay(3) } returns tomorrowResult

        for (phrase in tomorrowPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertEquals("Phrase '$phrase' should trigger tomorrow's schedule", tomorrowResult, result)
        }
    }

    @Test
    fun `evaluation - query specific day schedule`() = runTest {
        val thursdayResult = SueOperationResult.DaySchedule(
            dayOfWeek = 4,
            timeFilter = null,
            schedules = listOf(AgentScheduleSummary(maria.name, 4, "18:00", "19:00"))
        )
        coEvery { scheduleTools.getScheduleForDay(4, any()) } returns thursdayResult
        coEvery { scheduleTools.getScheduleForDay(4, null) } returns thursdayResult
        coEvery { scheduleTools.getScheduleForDay(4) } returns thursdayResult

        val thursdayPhrases = listOf(
            "Dime el horario del jueves",
            "¿Qué clases tengo los jueves?",
            "¿Qué tengo el jueves?"
        )

        for (phrase in thursdayPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertEquals("Phrase '$phrase' should trigger Thursday schedule", thursdayResult, result)
        }
    }

    @Test
    fun `evaluation - query next class`() = runTest {
        val nextClassResult = SueOperationResult.NextClass(
            schedule = AgentScheduleSummary(lucia.name, 2, "16:00", "17:00"),
            occurrenceDate = LocalDate.of(2025, 9, 16)
        )
        coEvery { scheduleTools.getNextClass(any()) } returns nextClassResult
        coEvery { scheduleTools.getNextClass(null) } returns nextClassResult
        coEvery { scheduleTools.getNextClass() } returns nextClassResult

        val nextClassPhrases = listOf(
            "¿Cuál es mi próxima clase?",
            "¿Qué clase es la siguiente?",
            "Siguiente clase"
        )

        for (phrase in nextClassPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertEquals("Phrase '$phrase' should trigger next class", nextClassResult, result)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category 2: Free Slot Queries
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `evaluation - query free slots with natural tutor phrases`() = runTest {
        val freeSlotsToday = SueOperationResult.FreeSlotsDetailed(
            freeDays = emptyList(),
            gapLines = listOf("Martes: hueco libre de 17:00 a 19:00")
        )
        coEvery { scheduleTools.getFreeSlots("08:00", "22:00", 2) } returns freeSlotsToday

        val freeTodayPhrases = listOf(
            "¿Tengo algún hueco libre hoy?",
            "¿Qué huecos libres tengo hoy?",
            "¿Tengo tiempo libre esta tarde?"
        )

        for (phrase in freeTodayPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertEquals("Phrase '$phrase' should trigger free slots for today", freeSlotsToday, result)
        }

        val fridaySlots = SueOperationResult.FreeSlotsDetailed(
            freeDays = listOf(5),
            gapLines = emptyList()
        )
        coEvery { scheduleTools.getFreeSlots("08:00", "22:00", 5) } returns fridaySlots

        val fridayPhrase = "¿Qué huecos libres tengo el viernes?"
        agent.resetConversationContext()
        val fridayResult = agent.detectActionIntent(fridayPhrase)
        assertEquals(fridaySlots, fridayResult)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category 3: Class Cancellations
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `evaluation - cancel class natural phrasing`() = runTest {
        val cancelAction = SuePendingAction.CancelClass(
            studentName = lucia.name,
            studentId = lucia.studentId,
            scheduleId = "sch_1",
            date = 1758000000000L,
            startTime = "16:00",
            endTime = "17:00"
        )
        coEvery { scheduleTools.prepareCancelAction(any(), any(), any()) } returns
                SueOperationResult.Prepare.Success(cancelAction)
        coEvery { scheduleTools.prepareCancelAction(any(), any(), null) } returns
                SueOperationResult.Prepare.Success(cancelAction)
        coEvery { scheduleTools.prepareCancelAction(any(), any()) } returns
                SueOperationResult.Prepare.Success(cancelAction)

        val cancelPhrases = listOf(
            "Cancela la clase de Lucía de hoy",
            "Cancela la clase con Lucía esta tarde",
            "No voy a poder dar clase con Lucía hoy"
        )

        for (phrase in cancelPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertTrue("Phrase '$phrase' should prepare cancel action", result is SueOperationResult.Prepare.Success)
            val action = (result as SueOperationResult.Prepare.Success).action
            assertTrue(action is SuePendingAction.CancelClass)
            assertEquals(lucia.studentId, (action as SuePendingAction.CancelClass).studentId)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category 4: Class Rescheduling
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `evaluation - reschedule class natural phrasing`() = runTest {
        val rescheduleAction = SuePendingAction.RescheduleClass(
            studentName = sofia.name,
            studentId = sofia.studentId,
            scheduleId = "sch_5",
            originalDate = 1758000000000L,
            originalStartTime = "17:00",
            newDayOfWeek = DayOfWeek.FRIDAY,
            newDate = 1758240000000L,
            newStartTime = "18:00",
            newEndTime = "19:00"
        )
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), any(), any(), any()) } returns
                SueOperationResult.Prepare.Success(rescheduleAction)
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), any(), any(), null) } returns
                SueOperationResult.Prepare.Success(rescheduleAction)
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), any(), any()) } returns
                SueOperationResult.Prepare.Success(rescheduleAction)

        val reschedulePhrases = listOf(
            "Pasa la clase de Sofía al viernes a las 18:00",
            "Mueve la clase de Sofía al viernes a las 18:00",
            "Cambia la clase de Sofía al viernes a las 18:00"
        )

        for (phrase in reschedulePhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertTrue("Phrase '$phrase' should prepare reschedule action", result is SueOperationResult.Prepare.Success)
            val action = (result as SueOperationResult.Prepare.Success).action
            assertTrue(action is SuePendingAction.RescheduleClass)
            assertEquals(sofia.studentId, (action as SuePendingAction.RescheduleClass).studentId)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category 5: Extra Classes
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `evaluation - add extra class natural phrasing`() = runTest {
        val extraClassAction = SuePendingAction.AddExtraClass(
            studentName = carlos.name,
            studentId = carlos.studentId,
            date = 1758240000000L,
            startTime = "16:00",
            endTime = "17:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "16:00", "17:00") } returns
                SueOperationResult.Prepare.Success(extraClassAction)

        val extraPhrases = listOf(
            "Añade una clase extra a Carlos el viernes a las cuatro de la tarde",
            "Ponle una clase extra a Carlos el viernes a las 16:00",
            "Añade una clase de refuerzo para Carlos el viernes a las 16:00"
        )

        for (phrase in extraPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            val extraAction = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
            assertEquals(carlos.studentId, extraAction.studentId)
            assertEquals("16:00", extraAction.startTime)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category 6: Financial Registrations & Debt Inquiries
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `evaluation - register payment cash and bizum phrasing`() = runTest {
        val cashPayment = SuePendingAction.RegisterPayment(
            studentName = lucia.name,
            studentId = lucia.studentId,
            amount = 30.0,
            paymentType = PaymentType.EFFECTIVE
        )
        coEvery { studentTools.prepareRegisterPayment(any(), 30.0, PaymentType.EFFECTIVE) } returns
                SueOperationResult.Prepare.Success(cashPayment)

        agent.resetConversationContext()
        val cashResult = agent.detectActionIntent("Registra un pago de 30 euros de Lucía en efectivo")
        assertTrue(cashResult is SueOperationResult.Prepare.Success)
        assertEquals(cashPayment, (cashResult as SueOperationResult.Prepare.Success).action)

        val bizumPayment = SuePendingAction.RegisterPayment(
            studentName = carlos.name,
            studentId = carlos.studentId,
            amount = 20.0,
            paymentType = PaymentType.BIZUM
        )
        coEvery { studentTools.prepareRegisterPayment(any(), 20.0, PaymentType.BIZUM) } returns
                SueOperationResult.Prepare.Success(bizumPayment)

        agent.resetConversationContext()
        val bizumResult = agent.detectActionIntent("Carlos me ha pagado 20 euros por bizum")
        assertTrue(bizumResult is SueOperationResult.Prepare.Success)
        assertEquals(bizumPayment, (bizumResult as SueOperationResult.Prepare.Success).action)
    }

    @Test
    fun `evaluation - add balance and debts inquiry`() = runTest {
        val addBalance = SuePendingAction.AddBalance(
            studentName = marcos.name,
            studentId = marcos.studentId,
            amount = 15.0
        )
        coEvery { studentTools.prepareAddBalance(any(), 15.0) } returns
                SueOperationResult.Prepare.Success(addBalance)

        agent.resetConversationContext()
        val addBalanceResult = agent.detectActionIntent("Suma 15 euros a la deuda de Marcos")
        assertTrue(addBalanceResult is SueOperationResult.Prepare.Success)
        assertEquals(addBalance, (addBalanceResult as SueOperationResult.Prepare.Success).action)

        val debtorResult = SueOperationResult.StudentsWithBalance(
            listOf(
                AgentBalanceSummary(lucia.name, -30.0),
                AgentBalanceSummary(maria.name, -50.0),
                AgentBalanceSummary(marcos.name, -15.0)
            )
        )
        coEvery { studentTools.getStudentsWithBalance() } returns debtorResult

        val debtorPhrases = listOf(
            "¿Quién me debe dinero?",
            "¿Quién tiene deuda pendiente?",
            "Alumnos con saldo pendiente"
        )

        for (phrase in debtorPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertEquals("Phrase '$phrase' should query debtors", debtorResult, result)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category 7: Colloquial Time Expressions & School Subject Aliases
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `evaluation - colloquial time idioms despues de comer, primera hora de la tarde, media tarde`() = runTest {
        // "después de comer" -> 15:00
        val afterLunchAction = SuePendingAction.AddExtraClass(
            studentName = lucia.name,
            studentId = lucia.studentId,
            date = 1758240000000L,
            startTime = "15:00",
            endTime = "16:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "15:00", "16:00") } returns
                SueOperationResult.Prepare.Success(afterLunchAction)

        agent.resetConversationContext()
        val afterLunchResult = agent.detectActionIntent("Añade clase extra a Lucía el viernes después de comer")
        assertTrue(afterLunchResult is SueOperationResult.Prepare.Success)
        val action1 = (afterLunchResult as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("15:00", action1.startTime)

        // "a primera hora de la tarde" -> 16:00
        val earlyAfternoonAction = SuePendingAction.AddExtraClass(
            studentName = carlos.name,
            studentId = carlos.studentId,
            date = 1758240000000L,
            startTime = "16:00",
            endTime = "17:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "16:00", "17:00") } returns
                SueOperationResult.Prepare.Success(earlyAfternoonAction)

        agent.resetConversationContext()
        val earlyAfternoonResult = agent.detectActionIntent("Pon una clase a Carlos el viernes a primera hora de la tarde")
        assertTrue(earlyAfternoonResult is SueOperationResult.Prepare.Success)
        val action2 = (earlyAfternoonResult as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("16:00", action2.startTime)

        // "a media tarde" -> 17:30
        val midAfternoonAction = SuePendingAction.RescheduleClass(
            studentName = maria.name,
            studentId = maria.studentId,
            scheduleId = "sch_3",
            originalDate = 1758000000000L,
            originalStartTime = "16:00",
            newDayOfWeek = DayOfWeek.FRIDAY,
            newDate = 1758240000000L,
            newStartTime = "17:30",
            newEndTime = "18:30"
        )
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), any(), "17:30", any()) } returns
                SueOperationResult.Prepare.Success(midAfternoonAction)
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), any(), "17:30", null) } returns
                SueOperationResult.Prepare.Success(midAfternoonAction)
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), any(), "17:30") } returns
                SueOperationResult.Prepare.Success(midAfternoonAction)

        agent.resetConversationContext()
        val midAfternoonResult = agent.detectActionIntent("Pasa la clase de María al viernes a media tarde")
        assertTrue(midAfternoonResult is SueOperationResult.Prepare.Success)
        val action3 = (midAfternoonResult as SueOperationResult.Prepare.Success).action as SuePendingAction.RescheduleClass
        assertEquals("17:30", action3.newStartTime)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category 8: Multi-turn Slot Filling (Incomplete Intents)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `evaluation - multi-turn slot filling when student name is initially missing`() = runTest {
        // Turn 1: User says "Cancela una clase de hoy" without student name
        agent.resetConversationContext()
        val turn1 = agent.detectActionIntent("Cancela una clase de hoy")
        assertTrue("Turn 1 should ask for student name", turn1 is SueOperationResult.Prepare.Error)
        assertTrue("Turn 1 details should prompt for student", (turn1 as SueOperationResult.Prepare.Error).details?.contains("alumno") == true)

        // Turn 2: User answers "De Lucía"
        val cancelAction = SuePendingAction.CancelClass(
            studentName = lucia.name,
            studentId = lucia.studentId,
            scheduleId = "sch_1",
            date = 1758000000000L,
            startTime = "16:00",
            endTime = "17:00"
        )
        coEvery { scheduleTools.prepareCancelAction(any(), any(), any()) } returns
                SueOperationResult.Prepare.Success(cancelAction)
        coEvery { scheduleTools.prepareCancelAction(any(), any(), null) } returns
                SueOperationResult.Prepare.Success(cancelAction)
        coEvery { scheduleTools.prepareCancelAction(any(), any()) } returns
                SueOperationResult.Prepare.Success(cancelAction)

        val turn2 = agent.detectActionIntent("De Lucía")
        assertTrue("Turn 2 should successfully prepare cancellation", turn2 is SueOperationResult.Prepare.Success)
        val action = (turn2 as SueOperationResult.Prepare.Success).action as SuePendingAction.CancelClass
        assertEquals(lucia.studentId, action.studentId)
    }

    @Test
    fun `evaluation - multi-turn slot filling when time is initially missing`() = runTest {
        // Turn 1: User says "Añade una clase extra para Pedro el viernes" without time
        coEvery { studentTools.extractRelevantStudent("añade una clase extra para pedro el viernes") } returns pedro
        coEvery { studentTools.extractRelevantStudent("a las 5 de la tarde") } returns null

        val pendingAction = SuePendingAction.AddExtraClass(
            studentName = pedro.name,
            studentId = pedro.studentId,
            date = 1758240000000L,
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "17:00", "18:00") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        agent.resetConversationContext()
        val turn1 = agent.detectActionIntent("Añade una clase extra para Pedro el viernes")
        assertTrue("Turn 1 should ask for time", turn1 is SueOperationResult.Prepare.Error)
        assertTrue("Turn 1 details asks for hour", (turn1 as SueOperationResult.Prepare.Error).details?.contains("hora") == true)

        // Turn 2: User responds "A las 5 de la tarde"
        val turn2 = agent.detectActionIntent("A las 5 de la tarde")
        assertTrue("Turn 2 should successfully prepare extra class", turn2 is SueOperationResult.Prepare.Success)
        val action = (turn2 as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("17:00", action.startTime)
        assertEquals("18:00", action.endTime)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category 9: Colloquial Imperative Verbs (Pon, Mueve, Quita) & Clitics
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `evaluation - pon verb colloquial variations`() = runTest {
        val extraAction = SuePendingAction.AddExtraClass(
            studentName = marcos.name,
            studentId = marcos.studentId,
            date = 1758240000000L,
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "17:00", "18:00") } returns
                SueOperationResult.Prepare.Success(extraAction)

        val ponPhrases = listOf(
            "Pon una clase a Marcos el viernes a las 17:00",
            "Ponle una clase a Marcos el viernes a las 5",
            "Pon a Marcos una clase el viernes a las 5",
            "Pon una clase de Marcos el viernes a las 5",
            "Pon una clase con Marcos el viernes a las 5",
            "Pon clase a Marcos el viernes a las 5"
        )

        for (phrase in ponPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertTrue("Phrase '$phrase' should prepare extra class", result is SueOperationResult.Prepare.Success)
            val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
            assertEquals(marcos.name, action.studentName)
            assertEquals("17:00", action.startTime)
        }
    }

    @Test
    fun `evaluation - mueve verb colloquial variations`() = runTest {
        val rescheduleAction = SuePendingAction.RescheduleClass(
            studentName = marcos.name,
            studentId = marcos.studentId,
            scheduleId = "sch_6",
            originalDate = 1758000000000L,
            originalStartTime = "16:00",
            newDayOfWeek = DayOfWeek.FRIDAY,
            newDate = 1758240000000L,
            newStartTime = "16:00",
            newEndTime = "17:00"
        )
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), 5, "16:00", any()) } returns
                SueOperationResult.Prepare.Success(rescheduleAction)
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), 5, "16:00") } returns
                SueOperationResult.Prepare.Success(rescheduleAction)

        val mueveDayPhrases = listOf(
            "Mueve la clase de Marcos al viernes",
            "Muévele la clase a Marcos al viernes",
            "Mueve a Marcos al viernes",
            "Mueve la de Marcos al viernes",
            "Muévele la de Marcos al viernes"
        )

        for (phrase in mueveDayPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertTrue("Phrase '$phrase' should prepare reschedule", result is SueOperationResult.Prepare.Success)
            val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.RescheduleClass
            assertEquals(marcos.name, action.studentName)
            assertEquals(DayOfWeek.FRIDAY, action.newDayOfWeek)
        }

        // Rescheduling time on same day (e.g. to 17:00)
        val sameDayReschedule = SuePendingAction.RescheduleClass(
            studentName = marcos.name,
            studentId = marcos.studentId,
            scheduleId = "sch_6",
            originalDate = 1758000000000L,
            originalStartTime = "16:00",
            newDayOfWeek = null,
            newDate = 1758000000000L,
            newStartTime = "17:00",
            newEndTime = "18:00"
        )
        coEvery { scheduleTools.prepareRescheduleAction(any(), 2, 2, "17:00", any()) } returns
                SueOperationResult.Prepare.Success(sameDayReschedule)
        coEvery { scheduleTools.prepareRescheduleAction(any(), 2, 2, "17:00") } returns
                SueOperationResult.Prepare.Success(sameDayReschedule)

        val mueveTimePhrases = listOf(
            "Mueve la clase de Marcos a las 5",
            "Muévele la clase a Marcos a las 5",
            "Mueve a Marcos a las 5"
        )

        for (phrase in mueveTimePhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertTrue("Phrase '$phrase' should reschedule time", result is SueOperationResult.Prepare.Success)
            val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.RescheduleClass
            assertEquals("17:00", action.newStartTime)
        }
    }

    @Test
    fun `evaluation - quita verb colloquial variations`() = runTest {
        val cancelAction = SuePendingAction.CancelClass(
            studentName = marcos.name,
            studentId = marcos.studentId,
            scheduleId = "sch_6",
            date = 1758000000000L,
            startTime = "16:00",
            endTime = "17:00"
        )
        coEvery { scheduleTools.prepareCancelAction(any(), 2, any()) } returns
                SueOperationResult.Prepare.Success(cancelAction)
        coEvery { scheduleTools.prepareCancelAction(any(), 2, null) } returns
                SueOperationResult.Prepare.Success(cancelAction)
        coEvery { scheduleTools.prepareCancelAction(any(), 2) } returns
                SueOperationResult.Prepare.Success(cancelAction)

        val quitaPhrases = listOf(
            "Quita la clase de Marcos del martes",
            "Quítale la clase a Marcos del martes",
            "Quita a Marcos la clase del martes",
            "Quítale a Marcos la clase del martes",
            "Quita la de Marcos del martes",
            "Quítale la de Marcos del martes",
            "Quita la clase de Marcos",
            "Quítale la clase a Marcos"
        )

        for (phrase in quitaPhrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertTrue("Phrase '$phrase' should prepare cancel class", result is SueOperationResult.Prepare.Success)
            val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.CancelClass
            assertEquals(marcos.name, action.studentName)
        }
    }

    @Test
    fun `evaluation - tutorial command phrases compatibility`() = runTest {
        // 1. "Añade un estudiante llamado Ana de bachillerato a 15 la hora"
        val createStudentAction = SuePendingAction.CreateStudent(
            name = "Ana",
            course = "bachillerato",
            subjects = "General",
            pricePerHour = 15.0
        )
        coEvery { studentTools.prepareCreateStudent("Ana", "bachillerato", any(), 15.0) } returns
                SueOperationResult.Prepare.Success(createStudentAction)

        agent.resetConversationContext()
        val createResult = agent.detectActionIntent("Añade un estudiante llamado Ana de bachillerato a 15 la hora")
        assertTrue("Tutorial student creation should succeed", createResult is SueOperationResult.Prepare.Success)
        val studentAction = (createResult as SueOperationResult.Prepare.Success).action as SuePendingAction.CreateStudent
        assertEquals("Ana", studentAction.name)
        assertEquals(15.0, studentAction.pricePerHour, 0.01)

        // 2. "Inicia una clase con Pepe de 60 minutos"
        val startClassAction = SuePendingAction.StartClass(
            studentName = "Pepe",
            studentId = "stud_7",
            durationMinutes = 60
        )
        coEvery { studentTools.prepareStartClass("Pepe", 60) } returns
                SueOperationResult.Prepare.Success(startClassAction)

        agent.resetConversationContext()
        val startResult = agent.detectActionIntent("Inicia una clase con Pepe de 60 minutos")
        assertTrue("Tutorial start class with 'con' should succeed", startResult is SueOperationResult.Prepare.Success)
        val startAct = (startResult as SueOperationResult.Prepare.Success).action as SuePendingAction.StartClass
        assertEquals("Pepe", startAct.studentName)
        assertEquals(60, startAct.durationMinutes)
    }

    @Test
    fun `evaluation - query past completed classes for student with natural phrasing`() = runTest {
        coEvery { studentTools.getClassLogs(match { normalize(it).contains("oscar") }) } returns listOf(
            com.devsusana.hometutorpro.domain.entities.AgentClassLog(
                studentId = "stud_10",
                scheduleId = "sch_10",
                date = 1758000000000L,
                startTime = "17:00",
                endTime = "18:00",
                isExtra = false
            ),
            com.devsusana.hometutorpro.domain.entities.AgentClassLog(
                studentId = "stud_10",
                scheduleId = "sch_extra",
                date = 1758050000000L,
                startTime = "18:00",
                endTime = "19:00",
                isExtra = true
            )
        )
        coEvery { scheduleTools.getWeeklyClassesForStudent(match { normalize(it).contains("oscar") }) } returns
                SueOperationResult.WeeklyClassesForStudent("Óscar", listOf(AgentScheduleSummary("Óscar", 2, "17:00", "18:00")))

        val phrases = listOf(
            "¿Cuántas clases le he dado a un alumno llamado Óscar?",
            "cuantas clases le he dado a un alumno llamado Oscar",
            "cuántas clases le he dado a Óscar",
            "cuantas clases le he dado a Oscar",
            "How many classes have I given to Oscar?",
            "How many classes did I teach to Oscar?"
        )

        for (phrase in phrases) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertTrue("Phrase '$phrase' should return ReadSuccess", result is SueOperationResult.ReadSuccess)
            val msg = (result as SueOperationResult.ReadSuccess).message
            assertTrue("Should contain class count: $msg", msg.contains("2 clases") || msg.contains("2"))
            assertTrue("Should mention Óscar: $msg", msg.contains("Óscar") || msg.contains("Oscar"))
        }

        agent.resetConversationContext()
        val weeklyResult = agent.detectActionIntent("how many classes for Oscar")
        assertTrue("Weekly inquiry should return WeeklyClassesForStudent", weeklyResult is SueOperationResult.WeeklyClassesForStudent)
    }

    @Test
    fun `evaluation - reschedule student within same day to another hour`() = runTest {
        val rescheduleMarcos = SuePendingAction.RescheduleClass(
            studentName = marcos.name,
            studentId = marcos.studentId,
            scheduleId = "sch_6",
            originalDate = 1758000000000L,
            originalStartTime = "16:00",
            newDayOfWeek = null,
            newDate = 1758000000000L,
            newStartTime = "18:00",
            newEndTime = "19:00"
        )
        coEvery { scheduleTools.prepareRescheduleAction(match { normalize(it).contains("marcos") }, any(), any(), "18:00", any()) } returns
                SueOperationResult.Prepare.Success(rescheduleMarcos)

        for (phrase in listOf("mueve la clase de Marcos a las 18", "move class for Marcos to 18")) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertTrue("Same day reschedule should succeed for '$phrase'", result is SueOperationResult.Prepare.Success)
            val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.RescheduleClass
            assertEquals(marcos.name, action.studentName)
            assertEquals("18:00", action.newStartTime)
        }
    }

    @Test
    fun `evaluation - cancel extra class for student`() = runTest {
        val cancelExtraLucia = SuePendingAction.CancelClass(
            studentName = lucia.name,
            studentId = lucia.studentId,
            scheduleId = "EXTRA",
            date = 1758000000000L,
            startTime = "18:30",
            endTime = "19:30",
            isExtraException = true,
            exceptionId = "exc_extra_1"
        )
        coEvery { scheduleTools.prepareCancelAction(match { normalize(it).contains("lucia") }, any(), any(), preferExtra = true) } returns
                SueOperationResult.Prepare.Success(cancelExtraLucia)

        for (phrase in listOf("cancela la clase extra de Lucía", "cancel extra class for Lucia")) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(phrase)
            assertTrue("Cancelling extra class should succeed for '$phrase'", result is SueOperationResult.Prepare.Success)
            val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.CancelClass
            assertEquals(lucia.name, action.studentName)
            assertTrue("Should be marked as extra exception", action.isExtraException)
            assertEquals("exc_extra_1", action.exceptionId)
        }
    }

    @Test
    fun `evaluation - compound action cancelling extra class and advancing another student`() = runTest {
        val cancelExtraLucia = SuePendingAction.CancelClass(
            studentName = lucia.name,
            studentId = lucia.studentId,
            scheduleId = "EXTRA",
            date = 1758000000000L,
            startTime = "18:30",
            endTime = "19:30",
            isExtraException = true,
            exceptionId = "exc_extra_1"
        )
        coEvery { scheduleTools.prepareCancelAction(match { normalize(it).contains("lucia") }, any(), any(), preferExtra = true) } returns
                SueOperationResult.Prepare.Success(cancelExtraLucia)

        val rescheduleHugo = SuePendingAction.RescheduleClass(
            studentName = "Hugo",
            studentId = "stud_9",
            scheduleId = "sch_9",
            originalDate = 1758000000000L,
            originalStartTime = "20:00",
            newDayOfWeek = null,
            newDate = 1758000000000L,
            newStartTime = "18:30",
            newEndTime = "19:30"
        )
        coEvery { scheduleTools.prepareRescheduleAction(match { normalize(it).contains("hugo") }, any(), any(), "18:30", any()) } returns
                SueOperationResult.Prepare.Success(rescheduleHugo)

        val compoundQueries = listOf(
            "cancela la clase extra de Lucía y adelanta la clase de Hugo que es a las 20:00 a las 18:30",
            "cancel the extra class of Lucia and move class for Hugo to 18:30"
        )
        for (compoundQuery in compoundQueries) {
            agent.resetConversationContext()
            val result = agent.detectActionIntent(compoundQuery)

            assertTrue("Compound query '$compoundQuery' should return MultipleSuccess", result is SueOperationResult.Prepare.MultipleSuccess)
            val multiple = result as SueOperationResult.Prepare.MultipleSuccess
            assertEquals("Should contain exactly 2 actions", 2, multiple.actions.size)

            val firstAction = multiple.actions[0] as SuePendingAction.CancelClass
            assertEquals(lucia.name, firstAction.studentName)
            assertTrue(firstAction.isExtraException)

            val secondAction = multiple.actions[1] as SuePendingAction.RescheduleClass
            assertEquals("Hugo", secondAction.studentName)
            assertEquals("18:30", secondAction.newStartTime)
        }
    }
}
