package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.sue.SueResponseFormatter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        coEvery { studentTools.extractRelevantStudent(any()) } returns null
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
        coEvery { scheduleTools.getCancelledClassesDescription(null, null, any()) } returns
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

    @Test
    fun `add extra class in 4 turns asking student, day and hour progressively`() = runTest {
        val student = AgentStudentDetail(
            studentId = "c1",
            name = "Carlos Santana",
            subjects = "Física",
            course = "2º Bachillerato",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent("anade una clase extra") } returns null
        coEvery { studentTools.extractRelevantStudent("para carlos") } returns student
        coEvery { studentTools.extractRelevantStudent("carlos") } returns student
        coEvery { studentTools.extractRelevantStudent("el viernes") } returns null
        coEvery { studentTools.extractRelevantStudent("a las 17:00") } returns null

        val pendingAction = SuePendingAction.AddExtraClass(
            studentName = "Carlos Santana",
            studentId = "c1",
            date = 1758240000000L,
            startTime = "17:00",
            endTime = "18:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass("Carlos Santana", any(), "17:00", "18:00") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        // Turn 1: User says "añade una clase extra" without student
        val turn1 = agent.detectActionIntent("añade una clase extra")
        assertTrue("Turn 1 should ask for student", turn1 is SueOperationResult.Prepare.Error)
        assertTrue((turn1 as SueOperationResult.Prepare.Error).details?.contains("alumno") == true)

        // Turn 2: User responds with "para Carlos"
        val turn2 = agent.detectActionIntent("para Carlos")
        assertTrue("Turn 2 should ask for day and hour", turn2 is SueOperationResult.Prepare.Error)
        assertTrue((turn2 as SueOperationResult.Prepare.Error).details?.contains("día") == true)

        // Turn 3: User responds with "el viernes"
        val turn3 = agent.detectActionIntent("el viernes")
        assertTrue("Turn 3 should ask for hour on Friday", turn3 is SueOperationResult.Prepare.Error)
        assertTrue((turn3 as SueOperationResult.Prepare.Error).details?.contains("viernes") == true)
        assertTrue(turn3.details?.contains("hora") == true)

        // Turn 4: User responds with "a las 17:00"
        val turn4 = agent.detectActionIntent("a las 17:00")
        assertTrue("Turn 4 should successfully prepare action", turn4 is SueOperationResult.Prepare.Success)
        val action = (turn4 as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("Carlos Santana", action.studentName)
        assertEquals("17:00", action.startTime)
        assertEquals("18:00", action.endTime)
    }

    @Test
    fun `query cancelled classes on specific day routes to read success`() = runTest {
        coEvery { studentTools.extractRelevantStudent(any()) } returns null
        coEvery { scheduleTools.getCancelledClassesDescription(5, null, any()) } returns "• Viernes 27/10/2023: la clase de Lucía está cancelada."

        val result = agent.detectActionIntent("¿Qué cancelaciones tengo el viernes?")
        assertTrue("Should return ReadSuccess", result is SueOperationResult.ReadSuccess)
        assertTrue((result as SueOperationResult.ReadSuccess).message.contains("cancelada"))
    }

    @Test
    fun `preserveContextForConflict preserves student and day so user can simply supply new time`() = runTest {
        coEvery { studentTools.extractRelevantStudent(any()) } returns null
        val originalAction = SuePendingAction.AddExtraClass(
            studentName = "Daniel Torres",
            studentId = "d1",
            date = 1758240000000L,
            startTime = "15:00",
            endTime = "16:00"
        )
        agent.preserveContextForConflict(originalAction)

        val newPendingAction = SuePendingAction.AddExtraClass(
            studentName = "Daniel Torres",
            studentId = "d1",
            date = 1758240000000L,
            startTime = "16:00",
            endTime = "17:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass("Daniel Torres", any(), "16:00", "17:00") } returns
                SueOperationResult.Prepare.Success(newPendingAction)

        // User simply says "a las 16:00"
        val result = agent.detectActionIntent("a las 16:00")
        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("Daniel Torres", action.studentName)
        assertEquals("16:00", action.startTime)
        assertEquals("17:00", action.endTime)
    }

    @Test
    fun `hour parsing infers afternoon 15_00 for a las 3 without dawn fallback`() = runTest {
        val student = AgentStudentDetail(
            studentId = "1",
            name = "Claudia Serrano",
            subjects = "Matemáticas",
            course = "4º ESO",
            pendingBalance = 0.0
        )
        coEvery { studentTools.extractRelevantStudent(any()) } returns student
        val pendingAction = SuePendingAction.AddExtraClass(
            studentName = "Claudia Serrano",
            studentId = "1",
            date = 1758240000000L,
            startTime = "15:00",
            endTime = "16:00"
        )
        coEvery { scheduleTools.prepareAddExtraClass("Claudia Serrano", any(), "15:00", "16:00") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        val result = agent.detectActionIntent("Añade una clase extra para Claudia el viernes a las 3")
        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.AddExtraClass
        assertEquals("15:00", action.startTime)
        assertEquals("16:00", action.endTime)
    }

    @Test
    fun `querying weekly extra classes returns read success and does not trigger add action`() = runTest {
        coEvery { studentTools.extractRelevantStudent(any()) } returns null
        coEvery { scheduleTools.getCancelledClassesDescription(null, null, any()) } returns
                "Clases extra programadas (activas):\n• Domingo 31/05/2026: clase extra con Claudia de 17:00 a 18:00."

        val result = agent.detectActionIntent("Dime las clases extra de la semana")
        assertTrue("Should return ReadSuccess", result is SueOperationResult.ReadSuccess)
        assertTrue((result as SueOperationResult.ReadSuccess).message.contains("17:00 a 18:00"))
    }

    @Test
    fun `querying weekly rescheduled classes returns read success`() = runTest {
        coEvery { studentTools.extractRelevantStudent(any()) } returns null
        coEvery { scheduleTools.getCancelledClassesDescription(null, null, any()) } returns
                "Clases reprogramadas:\n• Domingo 31/05/2026: clase de Juan movida a nuevo horario de 15:00 a 16:00."

        val result = agent.detectActionIntent("Dime las clases reprogramadas de la semana")
        assertTrue("Should return ReadSuccess", result is SueOperationResult.ReadSuccess)
        assertTrue((result as SueOperationResult.ReadSuccess).message.contains("Juan"))
    }

    @Test
    fun `buildPromptWithContext injects global shared resources when asked about sent material`() = runTest {
        val resources = listOf(
            com.devsusana.hometutorpro.domain.entities.SharedResource(
                id = "res-1",
                studentId = "stu-1",
                professorId = "prof_123",
                fileName = "algebra_ejercicios.pdf",
                fileType = "pdf",
                fileSizeBytes = 1024,
                sharedVia = com.devsusana.hometutorpro.domain.entities.ShareMethod.WHATSAPP,
                sharedAt = 1758240000000L
            )
        )
        val schedules = listOf(
            com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
                scheduleId = "sch-1",
                studentId = "stu-1",
                studentName = "Claudia",
                dayOfWeek = 1,
                startTime = "17:00",
                endTime = "18:00"
            )
        )
        coEvery { studentTools.extractRelevantStudent(any()) } returns null
        coEvery { studentTools.getAllSharedResources() } returns resources
        coEvery { scheduleTools.getScheduleDetails() } returns schedules

        val prompt = agent.buildPromptWithContext("¿Que material le he enviado a cada alumno?", emptyList())

        assertTrue("Prompt should contain section for shared resources", prompt.contains("RECURSOS / MATERIALES COMPARTIDOS"))
        assertTrue("Prompt should contain student name Claudia", prompt.contains("Claudia"))
        assertTrue("Prompt should contain file name algebra_ejercicios.pdf", prompt.contains("algebra_ejercicios.pdf"))
    }

    @Test
    fun `buildPromptWithContext handles empty shared resources gracefully`() = runTest {
        coEvery { studentTools.extractRelevantStudent(any()) } returns null
        coEvery { studentTools.getAllSharedResources() } returns emptyList()
        coEvery { scheduleTools.getScheduleDetails() } returns emptyList()

        val prompt = agent.buildPromptWithContext("¿He enviado recursos a algun alumno?", emptyList())

        assertTrue("Prompt should contain section for shared resources", prompt.contains("RECURSOS / MATERIALES COMPARTIDOS"))
        assertTrue("Prompt should state no resources shared", prompt.contains("No se han compartido materiales"))
    }

    @Test
    fun `buildPromptWithContext groups shared resources by student when count exceeds 15`() = runTest {
        val resources = (1..20).map { i ->
            com.devsusana.hometutorpro.domain.entities.SharedResource(
                id = "res-$i",
                studentId = if (i <= 10) "stu-1" else "stu-2",
                professorId = "prof_123",
                fileName = "tema_$i.pdf",
                fileType = "pdf",
                fileSizeBytes = 1024,
                sharedVia = com.devsusana.hometutorpro.domain.entities.ShareMethod.WHATSAPP,
                sharedAt = 1758240000000L + (i * 1000L)
            )
        }
        val scheduleDetails = listOf(
            com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
                scheduleId = "sch-1",
                studentId = "stu-1",
                studentName = "Claudia",
                dayOfWeek = 1,
                startTime = "17:00",
                endTime = "18:00"
            ),
            com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail(
                scheduleId = "sch-2",
                studentId = "stu-2",
                studentName = "Marcos",
                dayOfWeek = 2,
                startTime = "18:00",
                endTime = "19:00"
            )
        )
        coEvery { studentTools.extractRelevantStudent(any()) } returns null
        coEvery { studentTools.getAllSharedResources() } returns resources
        coEvery { scheduleTools.getScheduleDetails() } returns scheduleDetails

        val prompt = agent.buildPromptWithContext("¿Que material le he enviado a cada alumno?", emptyList())

        assertTrue("Prompt should contain section for shared resources", prompt.contains("RECURSOS / MATERIALES COMPARTIDOS"))
        assertTrue("Prompt should show total count and students count", prompt.contains("20 entre 2 alumnos"))
        assertTrue("Prompt should show Claudia with count", prompt.contains("Claudia: 10 archivo(s)"))
        assertTrue("Prompt should show Marcos with count", prompt.contains("Marcos: 10 archivo(s)"))
    }

    @Test
    fun `detectActionIntent creates student in one turn with llamalo and y ponle`() = runTest {
        coEvery { studentTools.prepareCreateStudent("Nuevo", "", "", 9.0) } returns
            SueOperationResult.Prepare.Success(
                SuePendingAction.CreateStudent("Nuevo", "", "", 9.0)
            )

        agent.resetConversationContext()
        val result = agent.detectActionIntent("crea un alumno llamalo nuevo y ponle la clase a 9€ la hora.")

        assertTrue("Should detect CreateStudent directly without loop", result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.CreateStudent
        assertEquals("Nuevo", action.name)
        assertEquals(9.0, action.pricePerHour, 0.001)
        assertEquals("", action.subjects)
    }

    @Test
    fun `detectActionIntent handles multi-turn student creation with 9 euro symbol without looping`() = runTest {
        agent.resetConversationContext()

        // Turn 1: create student name only
        val turn1Result = agent.detectActionIntent("crea un alumno llamalo nuevo")
        assertTrue("Turn 1 should ask for price", turn1Result is SueOperationResult.Prepare.Error)
        assertEquals("¿Cuál será el precio por hora de Nuevo?", (turn1Result as SueOperationResult.Prepare.Error).details)

        // Turn 2: answer with "9€"
        coEvery { studentTools.prepareCreateStudent("Nuevo", "", "", 9.0) } returns
            SueOperationResult.Prepare.Success(
                SuePendingAction.CreateStudent("Nuevo", "", "", 9.0)
            )

        val turn2Result = agent.detectActionIntent("9€")
        assertTrue("Turn 2 should successfully prepare CreateStudent", turn2Result is SueOperationResult.Prepare.Success)
        val action = (turn2Result as SueOperationResult.Prepare.Success).action as SuePendingAction.CreateStudent
        assertEquals("Nuevo", action.name)
        assertEquals(9.0, action.pricePerHour, 0.001)
        assertEquals("", action.subjects)
    }

    @Test
    fun `detectActionIntent handles multi-turn student creation with bare number without looping`() = runTest {
        agent.resetConversationContext()

        // Turn 1
        agent.detectActionIntent("crea un alumno llamalo nuevo")

        // Turn 2: bare number "9"
        coEvery { studentTools.prepareCreateStudent("Nuevo", "", "", 9.0) } returns
            SueOperationResult.Prepare.Success(
                SuePendingAction.CreateStudent("Nuevo", "", "", 9.0)
            )

        val turn2Result = agent.detectActionIntent("9")
        assertTrue("Turn 2 bare number should prepare CreateStudent", turn2Result is SueOperationResult.Prepare.Success)
        val action = (turn2Result as SueOperationResult.Prepare.Success).action as SuePendingAction.CreateStudent
        assertEquals("Nuevo", action.name)
        assertEquals(9.0, action.pricePerHour, 0.001)
    }

    @Test
    fun `detectActionIntent supports multiple subjects joined by comma`() = runTest {
        coEvery { studentTools.prepareCreateStudent("Marcos", "", "Matemáticas, Física", 15.0) } returns
            SueOperationResult.Prepare.Success(
                SuePendingAction.CreateStudent("Marcos", "", "Matemáticas, Física", 15.0)
            )

        agent.resetConversationContext()
        val result = agent.detectActionIntent("crea un alumno Marcos para matemáticas y física a 15 la hora")

        assertTrue("Should extract multiple subjects", result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.CreateStudent
        assertEquals("Marcos", action.name)
        assertEquals("Matemáticas, Física", action.subjects)
        assertEquals(15.0, action.pricePerHour, 0.001)
    }

    @Test
    fun `detectActionIntent prepares update address in single turn`() = runTest {
        val student = AgentStudentDetail(
            studentId = "stu-carlos",
            name = "Carlos",
            subjects = "Matemáticas",
            course = "2º Bachillerato",
            pricePerHour = 15.0,
            address = "Calle Antigua 1"
        )
        coEvery { studentTools.extractRelevantStudent("cambia la direccion de carlos a calle mayor 12, portero 4") } returns student
        val pendingAction = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-carlos",
            studentName = "Carlos",
            address = "Calle Mayor 12, portero 4"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Carlos", any()) } returns
            SueOperationResult.Prepare.Success(pendingAction)

        agent.resetConversationContext()
        val result = agent.detectActionIntent("cambia la dirección de Carlos a Calle Mayor 12, portero 4")

        assertTrue("Should prepare UpdateStudentAddress", result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.UpdateStudentAddress
        assertEquals("Carlos", action.studentName)
        assertEquals("Calle Mayor 12, portero 4", action.address)
    }

    @Test
    fun `detectActionIntent handles update address in 2 turns slot filling`() = runTest {
        val student = AgentStudentDetail(
            studentId = "stu-carlos",
            name = "Carlos",
            subjects = "Matemáticas",
            course = "2º Bachillerato",
            pricePerHour = 15.0,
            address = "Calle Antigua 1"
        )
        coEvery { studentTools.extractRelevantStudent("cambia la direccion de carlos") } returns student
        agent.resetConversationContext()

        // Turn 1: Ask to change address without specifying it
        val turn1 = agent.detectActionIntent("cambia la dirección de Carlos")
        assertTrue("Turn 1 should ask for address", turn1 is SueOperationResult.Prepare.Error)
        assertEquals("¿Cuál es la nueva dirección para Carlos?", (turn1 as SueOperationResult.Prepare.Error).details)

        // Turn 2: User provides address
        val pendingAction = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-carlos",
            studentName = "Carlos",
            address = "Calle Mayor 12, portero 4B"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Carlos", "Calle Mayor 12, portero 4B") } returns
            SueOperationResult.Prepare.Success(pendingAction)

        val turn2 = agent.detectActionIntent("Calle Mayor 12, portero 4B")
        assertTrue("Turn 2 should successfully prepare UpdateStudentAddress", turn2 is SueOperationResult.Prepare.Success)
        val action = (turn2 as SueOperationResult.Prepare.Success).action as SuePendingAction.UpdateStudentAddress
        assertEquals("Carlos", action.studentName)
        assertEquals("Calle Mayor 12, portero 4B", action.address)
    }

    @Test
    fun `parseLlmActionResponse parses UPDATE_STUDENT_ADDRESS tag`() = runTest {
        val pendingAction = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-carlos",
            studentName = "Carlos",
            address = "Avenida Constitución 10, portero 2"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Carlos", "Avenida Constitución 10, portero 2") } returns
            SueOperationResult.Prepare.Success(pendingAction)

        val llmOutput = """
            [ACTION: UPDATE_STUDENT_ADDRESS, student: "Carlos", address: "Avenida Constitución 10, portero 2"]
            He preparado la actualización de la dirección de Carlos.
        """.trimIndent()

        val result = agent.parseLlmActionResponse(llmOutput)
        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.UpdateStudentAddress
        assertEquals("Carlos", action.studentName)
        assertEquals("Avenida Constitución 10, portero 2", action.address)
    }

    @Test
    fun `query student details with only name and price outputs only available fields`() = runTest {
        val student = AgentStudentDetail(
            studentId = "stu-pepe",
            name = "Pepe",
            subjects = "",
            course = "",
            pricePerHour = 12.0,
            address = "",
            studentPhone = "",
            parentPhones = "",
            pendingBalance = 0.0,
            lastPaymentDate = null,
            notes = ""
        )
        val detailsResult = SueOperationResult.StudentDetails("Pepe", listOf(student))
        val formatted = SueResponseFormatter.format(detailsResult)

        assertTrue(formatted.contains("Pepe"))
        assertTrue(formatted.contains("Precio/hora: 12.0 €/h"))
        assertFalse(formatted.contains("Dirección"))
        assertFalse(formatted.contains("Sin dirección registrada"))
        assertFalse(formatted.contains("Último pago"))
        assertFalse(formatted.contains("Saldo pendiente"))
        assertFalse(formatted.contains("Asignaturas"))
    }

    @Test
    fun `query address for student without address returns polite no address message`() = runTest {
        val student = AgentStudentDetail(
            studentId = "stu-pepe",
            name = "Pepe",
            subjects = "",
            course = "",
            pricePerHour = 12.0,
            address = ""
        )
        coEvery { studentTools.searchStudent("Pepe") } returns SueOperationResult.StudentDetails("Pepe", listOf(student))
        agent.resetConversationContext()

        val result = agent.detectActionIntent("¿Dónde vive Pepe?")
        assertTrue(result is SueOperationResult.ReadSuccess)
        assertEquals("Pepe no tiene ninguna dirección registrada.", (result as SueOperationResult.ReadSuccess).message)
    }

    @Test
    fun `query address for student with address returns registered address`() = runTest {
        val student = AgentStudentDetail(
            studentId = "stu-pepe",
            name = "Pepe",
            subjects = "",
            course = "",
            pricePerHour = 12.0,
            address = "Calle Gran Vía 12, Portero 4"
        )
        coEvery { studentTools.searchStudent("Pepe") } returns SueOperationResult.StudentDetails("Pepe", listOf(student))
        agent.resetConversationContext()

        val result = agent.detectActionIntent("¿Cuál es la dirección de Pepe?")
        assertTrue(result is SueOperationResult.ReadSuccess)
        assertEquals("La dirección de Pepe es: Calle Gran Vía 12, Portero 4.", (result as SueOperationResult.ReadSuccess).message)
    }

    @Test
    fun `add door code to student address in single turn`() = runTest {
        val pendingAction = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-hugo",
            studentName = "Hugo Morales",
            address = "Calle Mayor 1, Portero: 387"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Hugo Morales", "Portero: 387") } returns
            SueOperationResult.Prepare.Success(pendingAction)
        agent.resetConversationContext()

        val result = agent.detectActionIntent("añade a hugo morales en su direccion el codigo de porte 387")
        assertTrue("Should detect UpdateStudentAddress", result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.UpdateStudentAddress
        assertEquals("Hugo Morales", action.studentName)
        assertEquals("Calle Mayor 1, Portero: 387", action.address)
    }

    @Test
    fun `ponle and cambia address variations are detected in single turn`() = runTest {
        val pendingAction1 = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-hugo",
            studentName = "Hugo",
            address = "Portero: 14B"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Hugo", "Portero: 14B") } returns
            SueOperationResult.Prepare.Success(pendingAction1)
        agent.resetConversationContext()

        val result1 = agent.detectActionIntent("ponle a Hugo en su direccion el portero 14B")
        assertTrue(result1 is SueOperationResult.Prepare.Success)

        val pendingAction2 = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-hugo",
            studentName = "Hugo",
            address = "Portero: 387"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Hugo", "Portero: 387") } returns
            SueOperationResult.Prepare.Success(pendingAction2)
        agent.resetConversationContext()

        val result2 = agent.detectActionIntent("cambia el portero de Hugo a 387")
        assertTrue(result2 is SueOperationResult.Prepare.Success)

        val pendingAction3 = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-hugo",
            studentName = "Hugo",
            address = "Calle Luna 5"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Hugo", "Calle Luna 5") } returns
            SueOperationResult.Prepare.Success(pendingAction3)
        agent.resetConversationContext()

        val result3 = agent.detectActionIntent("edita la direccion de Hugo a Calle Luna 5")
        assertTrue(result3 is SueOperationResult.Prepare.Success)

        val pendingAction4 = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-hugo",
            studentName = "Hugo",
            address = "Portero: 4B"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Hugo", "Portero: 4B") } returns
            SueOperationResult.Prepare.Success(pendingAction4)
        agent.resetConversationContext()

        val result4 = agent.detectActionIntent("edítale el portero a Hugo a 4B")
        assertTrue(result4 is SueOperationResult.Prepare.Success)

        val pendingAction5 = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-hugo",
            studentName = "Hugo",
            address = "Portero: 387"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Hugo", "Portero: 387") } returns
            SueOperationResult.Prepare.Success(pendingAction5)
        agent.resetConversationContext()

        val result5 = agent.detectActionIntent("añade a Hugo el portero 387")
        assertTrue(result5 is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `relocation phrases are detected and prepare update address action`() = runTest {
        val pendingAction1 = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-pepe",
            studentName = "Pepe",
            address = "Calle Mayor 12"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Pepe", "Calle Mayor 12") } returns
            SueOperationResult.Prepare.Success(pendingAction1)
        agent.resetConversationContext()

        val result1 = agent.detectActionIntent("Pepe se ha mudado a Calle Mayor 12")
        assertTrue("Should detect relocation for Pepe", result1 is SueOperationResult.Prepare.Success)
        val action1 = (result1 as SueOperationResult.Prepare.Success).action as SuePendingAction.UpdateStudentAddress
        assertEquals("Pepe", action1.studentName)
        assertEquals("Calle Mayor 12", action1.address)

        val pendingAction2 = SuePendingAction.UpdateStudentAddress(
            studentId = "stu-carlos",
            studentName = "Carlos",
            address = "Avenida de la Paz 8"
        )
        coEvery { studentTools.prepareUpdateAddressAction("Carlos", "Avenida de la Paz 8") } returns
            SueOperationResult.Prepare.Success(pendingAction2)
        agent.resetConversationContext()

        val result2 = agent.detectActionIntent("Carlos ahora vive en Avenida de la Paz 8")
        assertTrue("Should detect relocation for Carlos", result2 is SueOperationResult.Prepare.Success)
        val action2 = (result2 as SueOperationResult.Prepare.Success).action as SuePendingAction.UpdateStudentAddress
        assertEquals("Carlos", action2.studentName)
        assertEquals("Avenida de la Paz 8", action2.address)
    }

    @Test
    fun `update price is detected in single turn and multi-turn`() = runTest {
        val pendingAction = SuePendingAction.UpdateStudentPrice(
            studentId = "stu-carlos",
            studentName = "Carlos",
            pricePerHour = 18.0
        )
        coEvery { studentTools.prepareUpdatePriceAction("Carlos", 18.0) } returns
            SueOperationResult.Prepare.Success(pendingAction)
        agent.resetConversationContext()

        // Single turn
        val singleTurnResult = agent.detectActionIntent("cambia el precio de Carlos a 18 la hora")
        assertTrue("Single turn should detect UpdateStudentPrice", singleTurnResult is SueOperationResult.Prepare.Success)
        val action = (singleTurnResult as SueOperationResult.Prepare.Success).action as SuePendingAction.UpdateStudentPrice
        assertEquals("Carlos", action.studentName)
        assertEquals(18.0, action.pricePerHour, 0.001)

        // Multi-turn: Turn 1 asks for price
        agent.resetConversationContext()
        val turn1 = agent.detectActionIntent("cambia el precio de Carlos")
        assertTrue("Turn 1 should ask for price", turn1 is SueOperationResult.Prepare.Error)

        // Turn 2: User gives price
        val turn2 = agent.detectActionIntent("a 18 euros")
        assertTrue("Turn 2 should successfully prepare action", turn2 is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `update phone is detected in single turn and multi-turn`() = runTest {
        val pendingAction = SuePendingAction.UpdateStudentPhone(
            studentId = "stu-carlos",
            studentName = "Carlos",
            phone = "612345678"
        )
        coEvery { studentTools.prepareUpdatePhoneAction("Carlos", "612345678") } returns
            SueOperationResult.Prepare.Success(pendingAction)
        agent.resetConversationContext()

        // Single turn
        val singleTurnResult = agent.detectActionIntent("el nuevo teléfono de Carlos es 612345678")
        assertTrue("Single turn should detect UpdateStudentPhone", singleTurnResult is SueOperationResult.Prepare.Success)
        val action = (singleTurnResult as SueOperationResult.Prepare.Success).action as SuePendingAction.UpdateStudentPhone
        assertEquals("Carlos", action.studentName)
        assertEquals("612345678", action.phone)

        // Multi-turn: Turn 1 asks for phone
        agent.resetConversationContext()
        val turn1 = agent.detectActionIntent("cambia el teléfono de Carlos")
        assertTrue("Turn 1 should ask for phone", turn1 is SueOperationResult.Prepare.Error)

        // Turn 2: User gives phone
        val turn2 = agent.detectActionIntent("612345678")
        assertTrue("Turn 2 should successfully prepare action", turn2 is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `update course is detected in single turn and multi-turn`() = runTest {
        val pendingAction = SuePendingAction.UpdateStudentCourse(
            studentId = "stu-carlos",
            studentName = "Carlos",
            course = "2º de Bachillerato"
        )
        coEvery { studentTools.prepareUpdateCourseAction("Carlos", "2º de Bachillerato") } returns
            SueOperationResult.Prepare.Success(pendingAction)
        agent.resetConversationContext()

        // Single turn
        val singleTurnResult = agent.detectActionIntent("cambia el curso de Carlos a 2º de Bachillerato")
        assertTrue("Single turn should detect UpdateStudentCourse", singleTurnResult is SueOperationResult.Prepare.Success)
        val action = (singleTurnResult as SueOperationResult.Prepare.Success).action as SuePendingAction.UpdateStudentCourse
        assertEquals("Carlos", action.studentName)
        assertEquals("2º de Bachillerato", action.course)

        // Multi-turn: Turn 1 asks for course
        agent.resetConversationContext()
        val turn1 = agent.detectActionIntent("cambia el curso de Carlos")
        assertTrue("Turn 1 should ask for course", turn1 is SueOperationResult.Prepare.Error)

        // Turn 2: User gives course
        val turn2 = agent.detectActionIntent("2º de Bachillerato")
        assertTrue("Turn 2 should successfully prepare action", turn2 is SueOperationResult.Prepare.Success)
    }
}
