package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SueAgentImpl] intent-detection logic.
 *
 * Covers the keyword-based routing in [SueAgentImpl.extractDayOfWeek]
 * and [SueAgentImpl.detectActionIntent]. Schedule and student tools are stubbed
 * to return [SueOperationResult.Prepare.Error] to simulate "no match" scenarios cleanly.
 */
class SueAgentIntentDetectionTest {

    private lateinit var scheduleTools: ScheduleTools
    private lateinit var studentTools: StudentTools
    private lateinit var dateTimeProvider: DateTimeProvider
    private lateinit var authRepository: com.devsusana.hometutorpro.domain.repository.AuthRepository
    private lateinit var sueAgent: SueAgentImpl

    @Before
    fun setup() {
        scheduleTools = mockk(relaxed = true)
        studentTools = mockk(relaxed = true)
        dateTimeProvider = mockk(relaxed = true)
        authRepository = mockk(relaxed = true) {
            every { currentUser } returns kotlinx.coroutines.flow.MutableStateFlow(
                com.devsusana.hometutorpro.domain.entities.User(
                    uid = "prof-1",
                    email = "prof@test.com",
                    displayName = "Test Professor",
                    workingStartTime = "08:00",
                    workingEndTime = "23:00"
                )
            )
        }

        every { dateTimeProvider.getNow() } returns LocalDateTime.of(2026, 5, 27, 8, 35) // Wednesday
        every { dateTimeProvider.getLocale() } returns Locale.US

        // Stub tools to return Prepare.Error for missing entities
        coEvery { scheduleTools.prepareCancelAction(any(), any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.CLASS_NOT_FOUND)
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), any(), any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.CLASS_NOT_FOUND)
        coEvery { studentTools.prepareRegisterPayment(any(), any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        coEvery { studentTools.prepareAddBalance(any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)

        // Stub new operations
        coEvery { studentTools.prepareStartClass(any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        coEvery { studentTools.prepareCreateStudent(any(), any(), any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.UNKNOWN)
        coEvery { studentTools.prepareDeleteStudent(any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        coEvery { scheduleTools.prepareCreateSchedule(any(), any(), any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        coEvery { scheduleTools.prepareDeleteSchedule(any(), any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.CLASS_NOT_FOUND)
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        coEvery { studentTools.extractRelevantStudent(any()) } returns null

        sueAgent = SueAgentImpl(
            studentTools = studentTools,
            scheduleTools = scheduleTools,
            dateTimeProvider = dateTimeProvider,
            authRepository = authRepository
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // extractDayOfWeek
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `extractDayOfWeek should return 1 for lunes`() {
        assertEquals(1, sueAgent.extractDayOfWeek("cancela la clase de María el lunes"))
    }

    @Test
    fun `extractDayOfWeek should return 3 for miercoles without accent`() {
        assertEquals(3, sueAgent.extractDayOfWeek("clase el miercoles"))
    }

    @Test
    fun `extractDayOfWeek should return 3 for miércoles with accent`() {
        assertEquals(3, sueAgent.extractDayOfWeek("el miércoles tengo clase"))
    }

    @Test
    fun `extractDayOfWeek should return 5 for viernes`() {
        assertEquals(5, sueAgent.extractDayOfWeek("mueve la clase al viernes"))
    }

    @Test
    fun `extractDayOfWeek should return null when no day is mentioned`() {
        assertNull(sueAgent.extractDayOfWeek("cuántos alumnos tengo"))
    }

    @Test
    fun `extractDayOfWeek should return 1 for monday`() {
        assertEquals(1, sueAgent.extractDayOfWeek("schedule for monday"))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // detectActionIntent — returns null for non-management queries
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `detectActionIntent should return null for general student query`() = runTest {
        val result = sueAgent.detectActionIntent("cuántos alumnos tengo activos")
        assertNull(result)
    }

    @Test
    fun `detectActionIntent should return null for schedule read query`() = runTest {
        val result = sueAgent.detectActionIntent("cuál es mi horario del lunes")
        assertNull(result)
    }

    @Test
    fun `detectActionIntent should return Error when cancel intent has no day`() = runTest {
        val result = sueAgent.detectActionIntent("cancela la clase de María")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.CLASS_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should return non-null Prepare result for valid cancel intent`() = runTest {
        val result = sueAgent.detectActionIntent("cancela la clase de María el lunes")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.CLASS_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should detect payment and return Prepare result`() = runTest {
        val result = sueAgent.detectActionIntent("registra un pago de 20 euros de María")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should detect add balance and return Prepare result`() = runTest {
        val result = sueAgent.detectActionIntent("súmale 15.5 euros a la deuda de Juan")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should return Error when reschedule has no time`() = runTest {
        val scheduleDetail = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch123",
            studentId = "stud2",
            studentName = "Ana",
            dayOfWeek = 1, // Monday
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns listOf(scheduleDetail)

        val result = sueAgent.detectActionIntent("mueve la clase de Ana del miércoles al martes")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.UNKNOWN,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should resolve reschedule with relative days and 12-hour PM times`() = runTest {
        val scheduleDetail = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch-1",
            studentId = "stu-1",
            studentName = "Ana",
            dayOfWeek = 3,
            startTime = "16:30",
            endTime = "17:30"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns listOf(scheduleDetail)
        coEvery { scheduleTools.prepareRescheduleAction("Ana", 3, 4, "17:30", "16:30") } returns
                SueOperationResult.Prepare.Success(mockk())

        val result = sueAgent.detectActionIntent("mueve la clase de Ana de esta tarde a las 4:30 a 5:30 para mañana")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Success)
        io.mockk.coVerify { scheduleTools.prepareRescheduleAction("Ana", 3, 4, "17:30", "16:30") }
    }

    @Test
    fun `detectActionIntent should detect cancel intent with relative day hoy`() = runTest {
        val result = sueAgent.detectActionIntent("cancela la clase de María de hoy")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.CLASS_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should detect start class intent`() = runTest {
        val result = sueAgent.detectActionIntent("Inicia una clase para Alma de 45 minutos")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should detect create student intent`() = runTest {
        val result = sueAgent.detectActionIntent("Crea al alumno Carlos Pérez de física a 18 euros la hora")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.UNKNOWN,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should detect delete student intent`() = runTest {
        val result = sueAgent.detectActionIntent("elimina al alumno Carlos")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should detect create schedule intent`() = runTest {
        val result = sueAgent.detectActionIntent("añade un horario para Carlos los lunes a las 17:00")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should detect delete schedule intent`() = runTest {
        val result = sueAgent.detectActionIntent("elimina el horario de Carlos los lunes a las 17:00")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.CLASS_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should detect add extra class intent`() = runTest {
        val result = sueAgent.detectActionIntent("añade una clase extra para María hoy a las 5")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should persist context variables and fallback on subsequent calls`() = runTest {
        // Mock schedule tools to return a single schedule detail for Alma
        val scheduleDetail = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch123",
            studentId = "stud1",
            studentName = "Alma",
            dayOfWeek = 1, // Monday
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Alma") } returns listOf(scheduleDetail)
        coEvery { scheduleTools.prepareCancelAction("Alma", 1, "17:00") } returns
            SueOperationResult.Prepare.Success(com.devsusana.hometutorpro.domain.entities.SuePendingAction.CancelClass("Alma", "stud1", "sch123", 0L, "17:00", "18:00"))

        // First call sets lastMentionedStudentName to Alma
        sueAgent.detectActionIntent("Inicia una clase para Alma de 45 minutos")

        // Second call uses lastMentionedStudentName (Alma) and resolves day from schedule matching 17:00
        val result = sueAgent.detectActionIntent("cancela la clase a las 17:00")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as com.devsusana.hometutorpro.domain.entities.SuePendingAction.CancelClass
        assertEquals("Alma", action.studentName)
        assertEquals("17:00", action.startTime)
    }

    @Test
    fun `gatherRelevantContext should resolve targetDay when day is missing but time matches student schedule`() = runTest {
        val scheduleDetail = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch123",
            studentId = "stud1",
            studentName = "Alma",
            dayOfWeek = 1, // Monday
            startTime = "17:00",
            endTime = "18:00"
        )
        val scheduleSummary = com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary(
            studentName = "Alma",
            dayOfWeek = 1, // Monday
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Alma") } returns listOf(scheduleDetail)
        coEvery { scheduleTools.getScheduleForDay(1, "17:00") } returns
            SueOperationResult.DaySchedule(1, "17:00", listOf(scheduleSummary))

        // Set lastMentionedStudentName
        sueAgent.detectActionIntent("Inicia una clase para Alma de 45 minutos")

        // Call buildPromptWithContext (which invokes gatherRelevantContext)
        val prompt = sueAgent.buildPromptWithContext("dame la clase a las 17:00")
        assertTrue(prompt.contains("Available Data") || prompt.contains("Available data") || prompt.contains("Datos disponibles") || prompt.contains("DATOS DISPONIBLES") || prompt.contains("17:00"))
    }

    @Test
    fun `resetConversationContext should clear all persisted memory variables`() = runTest {
        // Set context
        sueAgent.detectActionIntent("Inicia una clase para Alma de 45 minutos")
        
        // Reset
        sueAgent.resetConversationContext()
        
        // Next cancel should fail with STUDENT_NOT_FOUND error
        val result = sueAgent.detectActionIntent("cancela la clase a las 17:00")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND, (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should persist intent type when fields are missing and succeed on next turn when missing fields are supplied`() = runTest {
        val scheduleDetail = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch123",
            studentId = "stud1",
            studentName = "Alma",
            dayOfWeek = 1, // Monday
            startTime = "17:00",
            endTime = "18:00"
        )
        val mockStudent = com.devsusana.hometutorpro.domain.entities.AgentStudentDetail(
            studentId = "stud1",
            name = "Alma",
            subjects = "Math",
            course = "Math",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent("alma") } returns mockStudent
        coEvery { studentTools.extractRelevantStudent("Alma") } returns mockStudent

        coEvery { scheduleTools.getSchedulesByStudentName("Alma") } returns listOf(scheduleDetail)
        coEvery { scheduleTools.prepareCancelAction("Alma", 1, "17:00") } returns
            SueOperationResult.Prepare.Success(com.devsusana.hometutorpro.domain.entities.SuePendingAction.CancelClass("Alma", "stud1", "sch123", 0L, "17:00", "18:00"))

        // First turn: "cancela la clase de esta tarde a las 5" (missing student name!)
        val result1 = sueAgent.detectActionIntent("cancela la clase de esta tarde a las 5")
        assertNotNull(result1)
        assertTrue(result1 is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND, (result1 as SueOperationResult.Prepare.Error).errorType)

        // Stub getSchedulesByStudentName for the correct day (3) to return a match
        val wednesdaySchedule = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch123",
            studentId = "stud1",
            studentName = "Alma",
            dayOfWeek = 3, // Wednesday
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Alma") } returns listOf(wednesdaySchedule)
        coEvery { scheduleTools.prepareCancelAction("Alma", 3, "17:00") } returns
            SueOperationResult.Prepare.Success(com.devsusana.hometutorpro.domain.entities.SuePendingAction.CancelClass("Alma", "stud1", "sch123", 0L, "17:00", "18:00"))

        // Second turn: user just says the student name: "Alma"
        val result2 = sueAgent.detectActionIntent("Alma")
        assertNotNull(result2)
        assertTrue(result2 is SueOperationResult.Prepare.Success)
        val action = (result2 as SueOperationResult.Prepare.Success).action as com.devsusana.hometutorpro.domain.entities.SuePendingAction.CancelClass
        assertEquals("Alma", action.studentName)
        assertEquals("17:00", action.startTime)
    }

    @Test
    fun `detectActionIntent should clear pending intent if user asks a general question`() = runTest {
        // First turn: "cancela la clase de esta tarde a las 5" (missing student name!)
        val result1 = sueAgent.detectActionIntent("cancela la clase de esta tarde a las 5")
        assertNotNull(result1)
        assertTrue(result1 is SueOperationResult.Prepare.Error)

        // Second turn: user asks a completely different question
        val result2 = sueAgent.detectActionIntent("¿cuál es mi horario de mañana?")
        assertNull(result2) // should bypass and return null to let LLM handle it
    }

    @Test
    fun `detectActionIntent should clear pending intent if user says abort words`() = runTest {
        // First turn: "cancela la clase de esta tarde a las 5" (missing student name!)
        val result1 = sueAgent.detectActionIntent("cancela la clase de esta tarde a las 5")
        assertNotNull(result1)
        assertTrue(result1 is SueOperationResult.Prepare.Error)

        // Second turn: user says "olvídalo"
        val result2 = sueAgent.detectActionIntent("olvídalo")
        assertNull(result2) // should bypass and return null
    }

    @Test
    fun `detectActionIntent should ask for duration when starting class without duration`() = runTest {
        // First turn: start class but missing duration
        val result = sueAgent.detectActionIntent("inicia clase para Alma")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals("¿De cuántos minutos será la clase con Alma?", (result as SueOperationResult.Prepare.Error).details)
    }

    @Test
    fun `detectActionIntent should resolve duration when provided in next turn`() = runTest {
        val mockStudent = com.devsusana.hometutorpro.domain.entities.AgentStudentDetail(
            studentId = "stu-1", name = "Alma", subjects = "Math", course = "ESO", pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent("alma") } returns mockStudent
        coEvery { studentTools.prepareStartClass("Alma", 45) } returns SueOperationResult.Prepare.Success(SuePendingAction.StartClass("Alma", "stu-1", 45))

        // Turn 1: Starts class but missing duration
        sueAgent.detectActionIntent("inicia clase para Alma")

        // Turn 2: User specifies duration
        val result = sueAgent.detectActionIntent("de 45 minutos")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.StartClass
        assertEquals("Alma", action.studentName)
        assertEquals(45, action.durationMinutes)
    }

    @Test
    fun `detectActionIntent should ask for name when creating student without name`() = runTest {
        val result = sueAgent.detectActionIntent("crea un alumno")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals("¿Cómo se llama el nuevo alumno?", (result as SueOperationResult.Prepare.Error).details)
    }

    @Test
    fun `detectActionIntent should ask for price when creating student without price`() = runTest {
        val result = sueAgent.detectActionIntent("crea al alumno Carlos")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals("¿Cuál será el precio por hora de Carlos?", (result as SueOperationResult.Prepare.Error).details)
    }

    @Test
    fun `detectActionIntent should resolve name and price in subsequent turns`() = runTest {
        coEvery { studentTools.prepareCreateStudent("Carlos", "Other", "General", 18.0) } returns
                SueOperationResult.Prepare.Success(SuePendingAction.CreateStudent("Carlos", "Other", "General", 18.0))

        // Turn 1: create student but missing price
        sueAgent.detectActionIntent("crea al alumno Carlos")

        // Turn 2: specify price
        val result = sueAgent.detectActionIntent("a 18 euros")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.CreateStudent
        assertEquals("Carlos", action.name)
        assertEquals(18.0, action.pricePerHour, 0.001)
    }

    @Test
    fun `detectActionIntent should reschedule by day only using same original time`() = runTest {
        val scheduleDetail = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch-1",
            studentId = "stu-1",
            studentName = "Ana",
            dayOfWeek = 3,
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns listOf(scheduleDetail)
        coEvery { scheduleTools.prepareRescheduleAction("Ana", 3, 4, "17:00", null) } returns
                SueOperationResult.Prepare.Success(mockk())

        val result = sueAgent.detectActionIntent("mueve la clase de Ana de esta tarde para mañana")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Success)
        io.mockk.coVerify { scheduleTools.prepareRescheduleAction("Ana", 3, 4, "17:00", null) }
    }

    @Test
    fun `detectActionIntent should resolve reschedule for student with multiple classes by matching time`() = runTest {
        val class1 = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch-1", studentId = "stu-1", studentName = "Ana",
            dayOfWeek = 3, startTime = "09:00", endTime = "10:00"
        )
        val class2 = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch-2", studentId = "stu-1", studentName = "Ana",
            dayOfWeek = 3, startTime = "17:00", endTime = "18:00"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns listOf(class1, class2)
        coEvery { scheduleTools.prepareRescheduleAction("Ana", 3, 4, "18:00", "17:00") } returns
                SueOperationResult.Prepare.Success(mockk())

        val result = sueAgent.detectActionIntent("mueve la clase de Ana de esta tarde a las 5 a las 6 para mañana")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Success)
        io.mockk.coVerify { scheduleTools.prepareRescheduleAction("Ana", 3, 4, "18:00", "17:00") }
    }

    @Test
    fun `detectActionIntent should resolve AM PM based on working hours range`() = runTest {
        val classDetail = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch-1", studentId = "stu-1", studentName = "Ana",
            dayOfWeek = 3, startTime = "16:30", endTime = "17:30"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns listOf(classDetail)
        coEvery { scheduleTools.prepareRescheduleAction("Ana", 3, 3, "16:00", "16:30") } returns
                SueOperationResult.Prepare.Success(mockk())

        val result = sueAgent.detectActionIntent("mueve la clase de Ana de esta tarde de las 4:30 a las 4")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Success)
        io.mockk.coVerify { scheduleTools.prepareRescheduleAction("Ana", 3, 3, "16:00", "16:30") }
     }

    @Test
    fun `detectActionIntent should return Error when reschedule is ambiguous due to multiple classes`() = runTest {
        val class1 = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch-1", studentId = "stu-1", studentName = "Ana",
            dayOfWeek = 1, startTime = "09:00", endTime = "10:00"
        )
        val class2 = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch-2", studentId = "stu-1", studentName = "Ana",
            dayOfWeek = 1, startTime = "17:00", endTime = "18:00"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns listOf(class1, class2)

        val result = sueAgent.detectActionIntent("mueve la clase de Ana del lunes al martes")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.UNKNOWN, (result as SueOperationResult.Prepare.Error).errorType)
        val details = (result as SueOperationResult.Prepare.Error).details
        assertTrue(details != null && (details.contains("varias") || details.contains("hora")))
    }

    @Test
    fun `detectActionIntent should resolve reschedule with single target day using target prepositions`() = runTest {
        val scheduleDetail = com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
            scheduleId = "sch-1", studentId = "stu-1", studentName = "Ana",
            dayOfWeek = 3, startTime = "16:30", endTime = "17:30"
        )
        coEvery { scheduleTools.getSchedulesByStudentName("Ana") } returns listOf(scheduleDetail)
        coEvery { scheduleTools.prepareRescheduleAction("Ana", 3, 1, "16:30", null) } returns
                SueOperationResult.Prepare.Success(mockk())

        val result = sueAgent.detectActionIntent("reprograma la clase de Ana al lunes")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Success)
        io.mockk.coVerify { scheduleTools.prepareRescheduleAction("Ana", 3, 1, "16:30", null) }
    }
}

