package com.devsusana.hometutorpro.core.sue

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Unit tests for [SueAgent] intent-detection logic.
 *
 * Covers the keyword-based routing in [SueAgent.extractDayOfWeek]
 * and [SueAgent.detectActionIntent]. Schedule tools are stubbed with
 * explicit [coEvery] to avoid type-erasure issues with relaxed mocks
 * returning wrong generic types for [Pair].
 */
class SueAgentIntentDetectionTest {

    private lateinit var scheduleTools: com.devsusana.hometutorpro.core.sue.tools.ScheduleTools
    private lateinit var sueAgent: SueAgent

    private lateinit var studentTools: com.devsusana.hometutorpro.core.sue.tools.StudentTools

    @Before
    fun setup() {
        scheduleTools = mockk(relaxed = true)
        studentTools = mockk(relaxed = true)
        // Provide correctly-typed stubs so the Pair<SuePendingAction.CancelClass?, String>
        // generic doesn't cause a ClassCastException when the agent destructures it.
        coEvery { scheduleTools.prepareCancelAction(any(), any()) } returns
                Pair(null, "No se encontró la clase.")
        coEvery { scheduleTools.prepareRescheduleAction(any(), any(), any(), any()) } returns
                Pair(null, "No se encontró la clase.")
        coEvery { studentTools.prepareRegisterPayment(any(), any(), any()) } returns
                Pair(null, "No se encontró al alumno.")
        coEvery { studentTools.prepareAddBalance(any(), any()) } returns
                Pair(null, "No se encontró al alumno.")

        sueAgent = SueAgent(
            studentTools = studentTools,
            scheduleTools = scheduleTools
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
    fun `detectActionIntent should return null when cancel intent has no day`() = runTest {
        // "cancela" detected but no day → extractDayOfWeek returns null → returns null
        val result = sueAgent.detectActionIntent("cancela la clase de María")
        assertNull(result)
    }

    @Test
    fun `detectActionIntent should return non-null pair for valid cancel intent`() = runTest {
        // Tools are stubbed to return Pair(null, message) — agent propagates it
        val result = sueAgent.detectActionIntent("cancela la clase de María el lunes")
        assertNotNull(result)
        // Message should be the stubbed confirmation text
        assertEquals("No se encontró la clase.", result!!.second)
    }

    @Test
    fun `detectActionIntent should detect payment and return non-null`() = runTest {
        val result = sueAgent.detectActionIntent("registra un pago de 20 euros de María")
        assertNotNull(result)
        assertEquals("No se encontró al alumno.", result!!.second)
    }

    @Test
    fun `detectActionIntent should detect add balance and return non-null`() = runTest {
        val result = sueAgent.detectActionIntent("súmale 15.5 euros a la deuda de Juan")
        assertNotNull(result)
        assertEquals("No se encontró al alumno.", result!!.second)
    }
}
