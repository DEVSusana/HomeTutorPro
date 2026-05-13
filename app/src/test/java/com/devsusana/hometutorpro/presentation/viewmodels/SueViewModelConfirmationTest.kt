package com.devsusana.hometutorpro.presentation.viewmodels

import com.devsusana.hometutorpro.core.sue.SpeechManager
import com.devsusana.hometutorpro.core.sue.SpeechState
import com.devsusana.hometutorpro.core.sue.SueAgent
import com.devsusana.hometutorpro.core.sue.SuePendingAction
import com.devsusana.hometutorpro.core.sue.inference.MediaPipeModelManager
import com.devsusana.hometutorpro.core.sue.tools.ScheduleTools
import com.devsusana.hometutorpro.presentation.sue.SueUiState
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SueViewModel]'s confirmation flow and timeout mechanism.
 *
 * All external dependencies are mocked so tests run without Android framework or Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SueViewModelConfirmationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var speechManager: SpeechManager
    private lateinit var sueAgent: SueAgent
    private lateinit var modelManager: MediaPipeModelManager
    private lateinit var scheduleTools: ScheduleTools
    private lateinit var studentTools: com.devsusana.hometutorpro.core.sue.tools.StudentTools
    private lateinit var viewModel: SueViewModel

    // Shared flows used to drive speechManager state from tests
    private val speechStateFlow = MutableStateFlow(SpeechState.IDLE)
    private val transcriptionFlow = MutableSharedFlow<String>()
    private val partialFlow = MutableSharedFlow<String>()
    private val errorFlow = MutableSharedFlow<String>()
    private val modelLoadedFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)

        speechManager = mockk(relaxed = true) {
            every { state } returns speechStateFlow
            every { transcriptions } returns transcriptionFlow
            every { partialTranscriptions } returns partialFlow
            every { errors } returns errorFlow
        }
        sueAgent = mockk(relaxed = true)
        // Instantiate real MediaPipeModelManager with a mocked context.
        // This avoids MockKException on its StateFlow properties while testing the ViewModel.
        modelManager = MediaPipeModelManager(mockk(relaxed = true))
        scheduleTools = mockk(relaxed = true)
        studentTools = mockk(relaxed = true)

        viewModel = SueViewModel(speechManager, sueAgent, modelManager, scheduleTools, studentTools)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        io.mockk.unmockkAll()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Confirmation flow — "sí" executes the pending action
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `when pendingAction exists and user says si it should execute cancel and clear pending`() = runTest {
        // Given — a pending cancel action stored in the VM
        val pendingAction = SuePendingAction.CancelClass(
            studentName = "María",
            studentId = "student1",
            scheduleId = "schedule1",
            date = 1_700_000_000_000L,
            dayLabel = "lunes 19 de mayo",
            startTime = "09:00",
            endTime = "10:00"
        )
        val successMessage = "Hecho. La clase de María del lunes 19 de mayo ha sido cancelada."
        coEvery { scheduleTools.executeCancelAction(pendingAction) } returns successMessage
        coEvery { sueAgent.detectActionIntent(any()) } returns null

        // Inject the pending action into the VM's state
        // We do this by first having the agent return it from detectActionIntent
        val confirmText = "¿Confirmas cancelar la clase de María el lunes 19 a las 09:00?"
        coEvery { sueAgent.detectActionIntent("cancela la clase de María el lunes") } returns
                Pair(pendingAction, confirmText)

        // Simulate first transcription — sets pending action
        transcriptionFlow.emit("cancela la clase de María el lunes")
        advanceUntilIdle()

        // Then a "sí" transcription arrives
        transcriptionFlow.emit("sí")
        advanceUntilIdle()

        // Then — pending action cleared, response set to success message
        val state: SueUiState = viewModel.uiState.value
        assertNull("Pending action should be cleared after execution", state.pendingAction)
        assertEquals(successMessage, state.agentResponse)
        coVerify { scheduleTools.executeCancelAction(pendingAction) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Confirmation flow — "no" cancels the pending action
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `when pendingAction exists and user says no it should clear pending without executing`() = runTest {
        // Given — inject a pending action via the agent
        val pendingAction = SuePendingAction.CancelClass(
            studentName = "Juan",
            studentId = "student2",
            scheduleId = "schedule2",
            date = 1_700_000_000_000L,
            dayLabel = "martes 20 de mayo",
            startTime = "11:00",
            endTime = "12:00"
        )
        coEvery { sueAgent.detectActionIntent("cancela la clase de Juan el martes") } returns
                Pair(pendingAction, "¿Confirmas...?")

        transcriptionFlow.emit("cancela la clase de Juan el martes")
        advanceUntilIdle()

        // When — user says "no"
        transcriptionFlow.emit("no")
        advanceUntilIdle()

        // Then — pending action cleared, execute never called
        assertNull(viewModel.uiState.value.pendingAction)
        coVerify(exactly = 0) { scheduleTools.executeCancelAction(any()) }
        coVerify(exactly = 0) { scheduleTools.executeRescheduleAction(any()) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Confirmation flow — ambiguous response asks again
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `when pendingAction exists and response is ambiguous it should keep pending and ask again`() = runTest {
        // Given
        val pendingAction = SuePendingAction.CancelClass(
            studentName = "Ana", studentId = "s3", scheduleId = "sch3",
            date = 0L, dayLabel = "miércoles", startTime = "10:00", endTime = "11:00"
        )
        coEvery { sueAgent.detectActionIntent("cancela la clase de Ana el miércoles") } returns
                Pair(pendingAction, "¿Confirmas...?")

        transcriptionFlow.emit("cancela la clase de Ana el miércoles")
        advanceUntilIdle()

        // When — user says something ambiguous
        transcriptionFlow.emit("quizás")
        advanceUntilIdle()

        // Then — pending action still set, response is the ask-again message
        assertEquals(pendingAction, viewModel.uiState.value.pendingAction)
        assertEquals("No he entendido. Di «sí» para confirmar o «no» para cancelar.",
            viewModel.uiState.value.agentResponse)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // onDismiss clears pending action
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `onDismiss should clear pendingAction and hide overlay`() = runTest {
        // Given — inject pending action
        val pendingAction = SuePendingAction.CancelClass(
            studentName = "Pedro", studentId = "s4", scheduleId = "sch4",
            date = 0L, dayLabel = "jueves", startTime = "16:00", endTime = "17:00"
        )
        coEvery { sueAgent.detectActionIntent("cancela la clase de Pedro el jueves") } returns
                Pair(pendingAction, "¿Confirmas...?")
        transcriptionFlow.emit("cancela la clase de Pedro el jueves")
        advanceUntilIdle()

        // When
        viewModel.onDismiss()
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.pendingAction)
        assertEquals(false, viewModel.uiState.value.isOverlayVisible)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Financial confirmation flow
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `when pendingAction is RegisterPayment and user says si it should execute payment`() = runTest {
        val paymentAction = SuePendingAction.RegisterPayment(
            studentName = "María",
            studentId = "student1",
            amount = 20.0,
            paymentType = com.devsusana.hometutorpro.domain.entities.PaymentType.EFFECTIVE
        )
        val successMessage = "Hecho. Se ha registrado el pago de 20.0 euros de María."
        coEvery { studentTools.executeRegisterPayment(paymentAction) } returns successMessage
        coEvery { sueAgent.detectActionIntent(any()) } returns null

        coEvery { sueAgent.detectActionIntent("registra un pago de maría") } returns
                Pair(paymentAction, "¿Confirmas?")
        
        transcriptionFlow.emit("registra un pago de maría")
        advanceUntilIdle()

        transcriptionFlow.emit("sí")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingAction)
        assertEquals(successMessage, viewModel.uiState.value.agentResponse)
        coVerify { studentTools.executeRegisterPayment(paymentAction) }
    }

    @Test
    fun `when pendingAction is AddBalance and user says si it should add balance`() = runTest {
        val addBalanceAction = SuePendingAction.AddBalance(
            studentName = "Juan",
            studentId = "student2",
            amount = 15.5
        )
        val successMessage = "Hecho. Se han sumado 15.5 euros a la cuenta de Juan."
        coEvery { studentTools.executeAddBalance(addBalanceAction) } returns successMessage
        coEvery { sueAgent.detectActionIntent(any()) } returns null

        coEvery { sueAgent.detectActionIntent("suma 15.5 euros a la deuda de Juan") } returns
                Pair(addBalanceAction, "¿Confirmas?")
        
        transcriptionFlow.emit("suma 15.5 euros a la deuda de Juan")
        advanceUntilIdle()

        transcriptionFlow.emit("sí")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingAction)
        assertEquals(successMessage, viewModel.uiState.value.agentResponse)
        coVerify { studentTools.executeAddBalance(addBalanceAction) }
    }
}
