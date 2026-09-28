package com.devsusana.hometutorpro.presentation.student_detail

import android.net.Uri
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.components.BulkScheduleEntry
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class StudentDetailStateTest {

    @Test
    fun `default values are correct`() {
        val state = StudentDetailState()

        assertNull(state.student)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isStudentSaved)
        assertFalse(state.isPaymentRegistered)
        assertFalse(state.isStudentDeleted)
        assertNull(state.successMessage)
        assertNull(state.errorMessage)
        assertFalse(state.isBalanceEditable)
        assertEquals("", state.balanceInput)
        assertEquals("", state.priceInput)

        assertTrue(state.sharedResources.isEmpty())
        assertFalse(state.showShareDialog)
        assertNull(state.selectedFileUri)
        assertEquals("", state.selectedFileName)
        assertEquals("", state.shareNotes)

        assertEquals(0, state.currentTab)
        assertTrue(state.bulkSchedules.isEmpty())
        assertFalse(state.isBulkScheduleMode)
        assertFalse(state.bulkScheduleSaving)
        assertTrue(state.pendingSchedules.isEmpty())
        assertTrue(state.schedules.isEmpty())
        assertFalse(state.showExtraClassDialog)
        assertFalse(state.showStartClassDialog)
    }

    @Test
    fun `custom values and copy work as expected`() {
        val student = Student(
            id = "s1",
            name = "John",
            age = 15,
            course = "10th",
            subjects = "Math",
            parentPhones = "1234",
            address = "Address",
            pricePerHour = 20.0
        )
        val uri = mockk<Uri>()
        val schedule = Schedule(
            id = "sch1",
            studentId = "s1",
            dayOfWeek = DayOfWeek.TUESDAY,
            startTime = "10:00",
            endTime = "11:00"
        )
        val bulkEntry = BulkScheduleEntry(
            id = 1,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "09:00",
            endTime = "10:00"
        )

        val state = StudentDetailState(
            student = student,
            isLoading = true,
            error = "An error",
            isStudentSaved = true,
            isPaymentRegistered = true,
            isStudentDeleted = true,
            successMessage = "Success",
            errorMessage = "Failed",
            isBalanceEditable = true,
            balanceInput = "50.0",
            priceInput = "25.0",
            sharedResources = emptyList(),
            showShareDialog = true,
            selectedFileUri = uri,
            selectedFileName = "doc.pdf",
            shareNotes = "Notes",
            currentTab = 1,
            bulkSchedules = listOf(bulkEntry),
            isBulkScheduleMode = true,
            bulkScheduleSaving = true,
            pendingSchedules = listOf(schedule),
            schedules = listOf(schedule),
            showExtraClassDialog = true,
            showStartClassDialog = true
        )

        assertEquals(student, state.student)
        assertTrue(state.isLoading)
        assertEquals("An error", state.error)
        assertTrue(state.isStudentSaved)
        assertTrue(state.isPaymentRegistered)
        assertTrue(state.isStudentDeleted)
        assertEquals("Success", state.successMessage)
        assertEquals("Failed", state.errorMessage)
        assertTrue(state.isBalanceEditable)
        assertEquals("50.0", state.balanceInput)
        assertEquals("25.0", state.priceInput)
        assertTrue(state.showShareDialog)
        assertEquals(uri, state.selectedFileUri)
        assertEquals("doc.pdf", state.selectedFileName)
        assertEquals("Notes", state.shareNotes)
        assertEquals(1, state.currentTab)
        assertEquals(1, state.bulkSchedules.size)
        assertTrue(state.isBulkScheduleMode)
        assertTrue(state.bulkScheduleSaving)
        assertEquals(1, state.pendingSchedules.size)
        assertEquals(1, state.schedules.size)
        assertTrue(state.showExtraClassDialog)
        assertTrue(state.showStartClassDialog)

        val copied = state.copy(isLoading = false, isBulkScheduleMode = false)
        assertFalse(copied.isLoading)
        assertFalse(copied.isBulkScheduleMode)
        assertEquals(student, copied.student)
    }
}
