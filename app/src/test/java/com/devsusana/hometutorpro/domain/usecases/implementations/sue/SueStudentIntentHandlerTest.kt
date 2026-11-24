package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.AgentClassLog
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class SueStudentIntentHandlerTest {

    private lateinit var studentTools: StudentTools
    private lateinit var scheduleTools: ScheduleTools
    private lateinit var entityExtractor: SueEntityExtractor
    private lateinit var context: SueConversationContext
    private lateinit var handler: SueStudentIntentHandler

    @Before
    fun setUp() {
        studentTools = mockk(relaxed = true)
        scheduleTools = mockk(relaxed = true)
        val dateTimeProvider = mockk<DateTimeProvider> {
            every { getNow() } returns LocalDateTime.of(2025, 9, 16, 10, 0)
            every { getLocale() } returns Locale.forLanguageTag("es-ES")
        }
        val authRepository = mockk<AuthRepository> {
            every { currentUser } returns MutableStateFlow(User(uid = "1", email = "p@t.com", displayName = "Prof"))
        }
        entityExtractor = SueEntityExtractor(dateTimeProvider, authRepository)
        context = SueConversationContext()
        handler = SueStudentIntentHandler(studentTools, scheduleTools, entityExtractor, context)
    }

    @Test
    fun `handleCreateStudent extracts name, price, course and prepares creation`() = runTest {
        val pending = SuePendingAction.CreateStudent("Carlos", "eso", "", 18.0)
        coEvery { studentTools.prepareCreateStudent("Carlos", "eso", any(), 18.0) } returns SueOperationResult.Prepare.Success(pending)

        val result = handler.handleCreateStudent(
            "crea un alumno llamado Carlos de eso a 18 euros",
            "crea un alumno llamado carlos de eso a 18 euros"
        )
        assertTrue(result is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `handleCreateStudent prompts when name or price missing`() = runTest {
        val missingName = handler.handleCreateStudent("crea un alumno", "crea un alumno")
        assertEquals(SlotType.STUDENT_NAME, context.activeIncompleteIntent?.missingSlot)
        assertTrue(missingName is SueOperationResult.Prepare.Error)

        context.lastMentionedStudentName = "Carlos"
        val missingPrice = handler.handleCreateStudent("carlos", "carlos")
        assertEquals(SlotType.PRICE, context.activeIncompleteIntent?.missingSlot)
        assertTrue(missingPrice is SueOperationResult.Prepare.Error)
    }

    @Test
    fun `handleDeleteStudent prompts when missing and prepares when present`() = runTest {
        assertTrue(handler.handleDeleteStudent() is SueOperationResult.Prepare.Error)
        context.lastMentionedStudentName = "Ana"
        coEvery { studentTools.prepareDeleteStudent("Ana") } returns SueOperationResult.Prepare.Success(
            SuePendingAction.DeleteStudent("1", "Ana")
        )
        assertTrue(handler.handleDeleteStudent() is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `handleUpdateNotes, Address, Price, Phone, Course handle missing and filled slots`() = runTest {
        assertTrue(handler.handleUpdateNotes("actualiza notas") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleUpdateAddress("cambia direccion", "cambia direccion") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleUpdatePrice("cambia precio") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleUpdatePhone("cambia telefono", "cambia telefono") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleUpdateCourse("cambia curso", "cambia curso") is SueOperationResult.Prepare.Error)

        context.lastMentionedStudentName = "Ana"
        assertTrue(handler.handleUpdateNotes("notas") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleUpdateAddress("direccion", "direccion") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleUpdatePrice("precio") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleUpdatePhone("telefono", "telefono") is SueOperationResult.Prepare.Error)
        assertTrue(handler.handleUpdateCourse("curso", "curso") is SueOperationResult.Prepare.Error)

        context.lastMentionedAddress = "Calle Mayor 5"
        coEvery { studentTools.prepareUpdateAddressAction("Ana", "Calle Mayor 5") } returns SueOperationResult.Prepare.Success(
            SuePendingAction.UpdateStudentAddress("1", "Ana", "Calle Mayor 5")
        )
        assertTrue(handler.handleUpdateAddress("ponle Calle Mayor 5", "ponle calle mayor 5") is SueOperationResult.Prepare.Success)

        context.lastMentionedPrice = 22.0
        coEvery { studentTools.prepareUpdatePriceAction("Ana", 22.0) } returns SueOperationResult.Prepare.Success(
            SuePendingAction.UpdateStudentPrice("1", "Ana", 22.0)
        )
        assertTrue(handler.handleUpdatePrice("ponle 22 euros") is SueOperationResult.Prepare.Success)

        context.lastMentionedPhone = "600112233"
        coEvery { studentTools.prepareUpdatePhoneAction("Ana", "600112233") } returns SueOperationResult.Prepare.Success(
            SuePendingAction.UpdateStudentPhone("1", "Ana", "600112233")
        )
        assertTrue(handler.handleUpdatePhone("600112233", "600112233") is SueOperationResult.Prepare.Success)

        context.lastMentionedCourse = "2º Bachillerato"
        coEvery { studentTools.prepareUpdateCourseAction("Ana", "2º Bachillerato") } returns SueOperationResult.Prepare.Success(
            SuePendingAction.UpdateStudentCourse("1", "Ana", "2º Bachillerato")
        )
        assertTrue(handler.handleUpdateCourse("2º Bachillerato", "2º bachillerato") is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `handleQueryStudentCount and handleQueryStudentDetails cover address and phone queries`() = runTest {
        coEvery { studentTools.getActiveStudentCount() } returns SueOperationResult.ActiveStudentCount(4)
        assertEquals(SueOperationResult.ActiveStudentCount(4), handler.handleQueryStudentCount())

        context.lastMentionedStudentName = "Ana"
        val detail = AgentStudentDetail(
            studentId = "1",
            name = "Ana",
            subjects = "Mates",
            course = "4º ESO",
            pendingBalance = 10.0,
            address = "Av. Libertad 3",
            studentPhone = "611223344",
            parentPhones = "699887766"
        )
        coEvery { studentTools.searchStudent("Ana") } returns SueOperationResult.StudentDetails("Ana", listOf(detail))

        val addr = handler.handleQueryStudentDetails("donde vive ana", "donde vive ana") as SueOperationResult.ReadSuccess
        assertTrue(addr.message.contains("Av. Libertad 3"))

        val phone = handler.handleQueryStudentDetails("cual es el telefono de ana", "cual es el telefono de ana") as SueOperationResult.ReadSuccess
        assertTrue(phone.message.contains("611223344"))
    }

    @Test
    fun `handleQueryStudentWeeklyClasses distinguishes past classes from weekly schedule`() = runTest {
        val noName = handler.handleQueryStudentWeeklyClasses("cuantas clases le he dado") as SueOperationResult.ReadSuccess
        assertTrue(noName.message.contains("nombre"))

        context.lastMentionedStudentName = "Ana"
        coEvery { studentTools.getClassLogs("Ana") } returns listOf(
            AgentClassLog("1", "s1", 1000L, "17:00", "18:00", isExtra = false),
            AgentClassLog("1", "s1", 2000L, "17:00", "18:00", isExtra = true)
        )
        coEvery { scheduleTools.getWeeklyClassesForStudent("Ana") } returns SueOperationResult.WeeklyClassesForStudent(
            "Ana",
            listOf(AgentScheduleSummary("Ana", 1, "17:00", "18:00"))
        )

        val past = handler.handleQueryStudentWeeklyClasses("cuantas clases le he dado a ana") as SueOperationResult.ReadSuccess
        assertTrue(past.message.contains("2 clases"))
        assertTrue(past.message.contains("1 ordinarias y 1 extra"))

        val weekly = handler.handleQueryStudentWeeklyClasses("que horario tiene ana")
        assertTrue(weekly is SueOperationResult.WeeklyClassesForStudent)
    }
}
