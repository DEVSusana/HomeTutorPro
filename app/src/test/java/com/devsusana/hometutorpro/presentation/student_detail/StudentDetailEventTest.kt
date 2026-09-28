package com.devsusana.hometutorpro.presentation.student_detail

import android.net.Uri
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ShareMethod
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.components.BulkScheduleEntry
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.DayOfWeek

class StudentDetailEventTest {

    @Test
    fun `test student events creation and properties`() {
        val student = Student(id = "1", name = "Test", pricePerHour = 15.0)
        val eventStudentChange = StudentDetailEvent.StudentChange(student)
        assertEquals(student, eventStudentChange.student)

        assertNotNull(StudentDetailEvent.SaveStudent)
        assertNotNull(StudentDetailEvent.DeleteStudent)

        val priceEvent = StudentDetailEvent.PriceChange("20.0")
        assertEquals("20.0", priceEvent.input)

        val balanceEvent = StudentDetailEvent.BalanceChange("100.0")
        assertEquals("100.0", balanceEvent.input)

        assertNotNull(StudentDetailEvent.ToggleBalanceEdit)
        assertNotNull(StudentDetailEvent.SaveBalance)

        val paymentEvent = StudentDetailEvent.RegisterPayment(50.0, PaymentType.BIZUM)
        assertEquals(50.0, paymentEvent.amount, 0.001)
        assertEquals(PaymentType.BIZUM, paymentEvent.type)

        val startClassEvent = StudentDetailEvent.StartClass(60)
        assertEquals(60, startClassEvent.durationMinutes)

        val schedule = Schedule(id = "s1", studentId = "1", dayOfWeek = DayOfWeek.MONDAY, startTime = "10:00", endTime = "11:00")
        val saveScheduleEvent = StudentDetailEvent.SaveSchedule(schedule)
        assertEquals(schedule, saveScheduleEvent.schedule)

        val deleteScheduleEvent = StudentDetailEvent.DeleteSchedule("s1")
        assertEquals("s1", deleteScheduleEvent.scheduleId)

        assertNotNull(StudentDetailEvent.ClearFeedback)

        val uri = mockk<Uri>()
        val fileSelectedEvent = StudentDetailEvent.FileSelected(uri, "doc.pdf", "application/pdf", 1024L)
        assertEquals(uri, fileSelectedEvent.uri)
        assertEquals("doc.pdf", fileSelectedEvent.name)
        assertEquals("application/pdf", fileSelectedEvent.type)
        assertEquals(1024L, fileSelectedEvent.size)

        val shareEvent = StudentDetailEvent.ShareResource(ShareMethod.WHATSAPP, "pdf", 2048L)
        assertEquals(ShareMethod.WHATSAPP, shareEvent.method)
        assertEquals("pdf", shareEvent.fileType)
        assertEquals(2048L, shareEvent.size)

        val deleteSharedEvent = StudentDetailEvent.DeleteSharedResource("res1")
        assertEquals("res1", deleteSharedEvent.resourceId)

        assertNotNull(StudentDetailEvent.DismissShareDialog)

        val notesEvent = StudentDetailEvent.ShareNotesChange("Test notes")
        assertEquals("Test notes", notesEvent.notes)

        val tabEvent = StudentDetailEvent.TabChange(2)
        assertEquals(2, tabEvent.index)

        assertNotNull(StudentDetailEvent.ContinueToNextStep)
        assertNotNull(StudentDetailEvent.ToggleBulkScheduleMode)

        val bulkEntry = BulkScheduleEntry(id = 1, dayOfWeek = DayOfWeek.FRIDAY, startTime = "09:00", endTime = "10:00")
        val bulkEvent = StudentDetailEvent.BulkSchedulesChange(listOf(bulkEntry))
        assertEquals(1, bulkEvent.schedules.size)
        assertEquals(bulkEntry, bulkEvent.schedules[0])

        assertNotNull(StudentDetailEvent.SaveBulkSchedules)
        assertNotNull(StudentDetailEvent.ShowExtraClassDialog)
        assertNotNull(StudentDetailEvent.HideExtraClassDialog)

        val extraClassEvent = StudentDetailEvent.SaveExtraClass(123456789L, "16:00", "17:00", DayOfWeek.FRIDAY)
        assertEquals(123456789L, extraClassEvent.date)
        assertEquals("16:00", extraClassEvent.startTime)
        assertEquals("17:00", extraClassEvent.endTime)
        assertEquals(DayOfWeek.FRIDAY, extraClassEvent.dayOfWeek)
    }
}
