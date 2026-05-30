package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
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
 * to return [SueOperationResult.Prepare.Error] with CLASS_NOT_FOUND or
 * STUDENT_NOT_FOUND to simulate "no match" scenarios cleanly.
 */
class SueAgentIntentDetectionTest {

    private lateinit var scheduleTools: ScheduleTools
    private lateinit var studentTools: StudentTools
    private lateinit var dateTimeProvider: DateTimeProvider
    private lateinit var sueAgent: SueAgentImpl

    @Before
    fun setup() {
        scheduleTools = mockk(relaxed = true)
        studentTools = mockk(relaxed = true)
        dateTimeProvider = mockk(relaxed = true)

        every { dateTimeProvider.getNow() } returns LocalDateTime.of(2026, 5, 27, 8, 35) // Wednesday
        every { dateTimeProvider.getLocale() } returns Locale.US

        // Stub tools to return Prepare.Error for missing entities
        coEvery { scheduleTools.prepareCancelAction(any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.CLASS_NOT_FOUND)
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.CLASS_NOT_FOUND)
        coEvery { studentTools.prepareRegisterPayment(any(), any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        coEvery { studentTools.prepareAddBalance(any(), any()) } returns
            SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)

        sueAgent = SueAgentImpl(
            studentTools = studentTools,
            scheduleTools = scheduleTools,
            dateTimeProvider = dateTimeProvider
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
        // Tools stubbed to CLASS_NOT_FOUND → Prepare.Error
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
        val result = sueAgent.detectActionIntent("mueve la clase de Ana del lunes al martes")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.UNKNOWN,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `detectActionIntent should detect cancel intent with relative day hoy`() = runTest {
        val result = sueAgent.detectActionIntent("cancela la clase de María de hoy")
        assertNotNull(result)
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.CLASS_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }
}
