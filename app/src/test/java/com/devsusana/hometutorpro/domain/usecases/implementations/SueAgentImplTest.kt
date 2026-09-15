package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * Comprehensive unit tests for [SueAgentImpl] intent parsing, multi-turn flows,
 * Spanish time extraction, and mock data integrity.
 */
class SueAgentImplTest {

    private lateinit var studentTools: StudentTools
    private lateinit var scheduleTools: ScheduleTools
    private lateinit var dateTimeProvider: DateTimeProvider
    private lateinit var authRepository: AuthRepository
    private lateinit var agent: SueAgentImpl

    private val fixedNow = LocalDateTime.of(2025, 9, 16, 10, 0) // Tuesday 16 Sept 2025

    @Before
    fun setUp() {
        studentTools = mockk(relaxed = true)
        scheduleTools = mockk(relaxed = true)
        dateTimeProvider = mockk {
            every { getNow() } returns fixedNow
            every { getLocale() } returns Locale("es", "ES")
        }
        authRepository = mockk {
            val user = User(
                uid = "prof_123",
                email = "prof@test.com",
                displayName = "Profesor",
                workingStartTime = "08:00",
                workingEndTime = "23:00"
            )
            every { currentUser } returns MutableStateFlow(user)
        }

        agent = SueAgentImpl(
            studentTools = studentTools,
            scheduleTools = scheduleTools,
            dateTimeProvider = dateTimeProvider,
            authRepository = authRepository
        )
    }

    @Test
    fun `add extra class in 2 turns when user replies with conversational time a las 5`() = runTest {
        val student = AgentStudentDetail(
            studentId = "1",
            name = "Lucía Moreno García",
            subjects = "Matemáticas",
            course = "4º ESO",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent("anade una clase extra para lucia el viernes") } returns student
        coEvery { studentTools.extractRelevantStudent("a las 5") } returns null

        val pendingAction = SuePendingAction.AddExtraClass(
            studentName = "Lucía Moreno García",
            studentId = "1",
            date = 1758240000000L,
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "17:00", "18:00") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        // Turn 1: User asks without specifying time
        val turn1 = agent.detectActionIntent("añade una clase extra para Lucia el viernes")
        assertTrue("Turn 1 should ask for the time", turn1 is SueOperationResult.Prepare.Error)
        assertTrue("Turn 1 message asks for hour", (turn1 as SueOperationResult.Prepare.Error).details?.contains("hora") == true)

        // Turn 2: User responds with "a las 5"
        val turn2 = agent.detectActionIntent("a las 5")
        assertTrue("Turn 2 should successfully prepare action", turn2 is SueOperationResult.Prepare.Success)
        val action = (turn2 as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("17:00", action.startTime)
        assertEquals("18:00", action.endTime)
    }

    @Test
    fun `add extra class in 2 turns when user replies with standalone number 17`() = runTest {
        val student = AgentStudentDetail(
            studentId = "1",
            name = "Lucía Moreno García",
            subjects = "Matemáticas",
            course = "4º ESO",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent("ponle una clase extra a lucia el viernes") } returns student
        coEvery { studentTools.extractRelevantStudent("17") } returns null

        val pendingAction = SuePendingAction.AddExtraClass(
            studentName = "Lucía Moreno García",
            studentId = "1",
            date = 1758240000000L,
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "17:00", "18:00") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        // Turn 1
        val turn1 = agent.detectActionIntent("ponle una clase extra a Lucia el viernes")
        assertTrue(turn1 is SueOperationResult.Prepare.Error)

        // Turn 2: User replies "17"
        val turn2 = agent.detectActionIntent("17")
        assertTrue("Turn 2 should successfully prepare action", turn2 is SueOperationResult.Prepare.Success)
        val action = (turn2 as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("17:00", action.startTime)
    }

    @Test
    fun `add extra class in 2 turns when user replies with 5 de la tarde`() = runTest {
        val student = AgentStudentDetail(
            studentId = "1",
            name = "Lucía Moreno García",
            subjects = "Matemáticas",
            course = "4º ESO",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent(any()) } returnsMany listOf(student, null)

        val pendingAction = SuePendingAction.AddExtraClass(
            studentName = "Lucía Moreno García",
            studentId = "1",
            date = 1758240000000L,
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "17:00", "18:00") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        agent.detectActionIntent("añade una clase extra para Lucia el viernes")
        val turn2 = agent.detectActionIntent("5 de la tarde")
        assertTrue("Turn 2 should resolve 5 de la tarde", turn2 is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `add extra class in 2 turns when user replies with 5 y media`() = runTest {
        val student = AgentStudentDetail(
            studentId = "1",
            name = "Lucía Moreno García",
            subjects = "Matemáticas",
            course = "4º ESO",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent(any()) } returnsMany listOf(student, null)

        val pendingAction = SuePendingAction.AddExtraClass(
            studentName = "Lucía Moreno García",
            studentId = "1",
            date = 1758240000000L,
            startTime = "17:30",
            endTime = "18:30"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "17:30", "18:30") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        agent.detectActionIntent("añade una clase extra para Lucia el viernes")
        val turn2 = agent.detectActionIntent("5 y media")
        assertTrue("Turn 2 should resolve 5 y media to 17:30", turn2 is SueOperationResult.Prepare.Success)
        val action = (turn2 as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("17:30", action.startTime)
        assertEquals("18:30", action.endTime)
    }

    @Test
    fun `query cancelled classes does not use stale session student filter`() = runTest {
        // Step 1: User previously talked about Carlos
        coEvery { studentTools.extractRelevantStudent("carlos me ha pagado 20 euros") } returns AgentStudentDetail(
            studentId = "2",
            name = "Carlos",
            subjects = "Física",
            course = "Bachillerato",
            pendingBalance = 0.0
        )
        agent.detectActionIntent("Carlos me ha pagado 20 euros")

        // Step 2: User asks general cancellation query without specifying student
        coEvery { studentTools.extractRelevantStudent("¿que clases tengo canceladas?") } returns null
        coEvery { scheduleTools.getCancelledClassesDescription(null, null) } returns
                "Clases canceladas:\n• Viernes 19/09/2025: la clase de Lucía (17:00 - 18:30) está cancelada."

        val result = agent.detectActionIntent("¿Qué clases tengo canceladas?")
        assertTrue("Result should be ReadSuccess", result is SueOperationResult.ReadSuccess)
        val message = (result as SueOperationResult.ReadSuccess).message
        assertTrue("Should include Lucía's cancelled class", message.contains("Lucía"))
    }

    @Test
    fun `add extra class in 2 turns when user replies with 21 or veintiuno`() = runTest {
        val student = AgentStudentDetail(
            studentId = "1",
            name = "Lucía Moreno García",
            subjects = "Matemáticas",
            course = "4º ESO",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent(any()) } answers {
            if (firstArg<String>().contains("lucia", ignoreCase = true)) student else null
        }

        val pendingAction = SuePendingAction.AddExtraClass(
            studentName = "Lucía Moreno García",
            studentId = "1",
            date = 1758240000000L,
            startTime = "21:00",
            endTime = "22:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass(any(), any(), "21:00", "22:00") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        // Case A: 21 digits
        agent.detectActionIntent("añade una clase extra para Lucia el viernes")
        val turn2A = agent.detectActionIntent("21")
        assertTrue("Turn 2 with 21 should succeed", turn2A is SueOperationResult.Prepare.Success)
        val actionA = (turn2A as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("21:00", actionA.startTime)
        assertEquals("22:00", actionA.endTime)

        // Case B: a las veintiuno in words
        agent.detectActionIntent("añade una clase extra para Lucia el viernes")
        val turn2B = agent.detectActionIntent("a las veintiuno")
        assertTrue("Turn 2 with 'a las veintiuno' should succeed", turn2B is SueOperationResult.Prepare.Success)
        val actionB = (turn2B as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("21:00", actionB.startTime)
        assertEquals("22:00", actionB.endTime)
    }

    @Test
    fun `mock backup JSON has zero schedule overlaps`() {
        val jsonFile = listOf(
            File("docs/mock_sample_students_backup.json"),
            File("../docs/mock_sample_students_backup.json")
        ).firstOrNull { it.exists() }
        assertNotNull("Mock file exists", jsonFile)

        val text = jsonFile!!.readText()
        val schedulesByDay = mutableMapOf<String, MutableList<Pair<LocalTime, LocalTime>>>()

        val scheduleRegex = Regex(""""dayOfWeek":\s*"(\w+)",\s*"startTime":\s*"(\d{2}:\d{2})",\s*"endTime":\s*"(\d{2}:\d{2})"""")
        for (match in scheduleRegex.findAll(text)) {
            val day = match.groupValues[1]
            val start = LocalTime.parse(match.groupValues[2])
            val end = LocalTime.parse(match.groupValues[3])
            schedulesByDay.getOrPut(day) { mutableListOf() }.add(Pair(start, end))
        }

        assertTrue("Found schedules in JSON", schedulesByDay.isNotEmpty())

        // Verify each day has no overlapping intervals
        for ((day, intervals) in schedulesByDay) {
            intervals.sortBy { it.first }
            for (j in 0 until intervals.size - 1) {
                val current = intervals[j]
                val next = intervals[j + 1]
                assertTrue(
                    "Overlap found on $day between ${current.first}-${current.second} and ${next.first}-${next.second}",
                    !current.second.isAfter(next.first)
                )
            }
        }
    }
}
