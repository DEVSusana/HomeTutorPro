package com.devsusana.hometutorpro.presentation.viewmodels

import android.util.Log
import com.devsusana.hometutorpro.domain.entities.SpeechState
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.repository.SpeechService
import com.devsusana.hometutorpro.domain.repository.InferenceRepository
import com.devsusana.hometutorpro.domain.usecases.ISueAgent
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.presentation.sue.SueResponseFormatter
import com.devsusana.hometutorpro.presentation.sue.SueUiState
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

    private lateinit var speechService: SpeechService
    private lateinit var sueAgent: ISueAgent
    private lateinit var inferenceRepository: InferenceRepository
    private lateinit var scheduleTools: ScheduleTools
    private lateinit var studentTools: com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
    private lateinit var viewModel: SueViewModel

    // Shared flows used to drive speechService state from tests
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

        speechService = mockk(relaxed = true) {
            every { state } returns speechStateFlow
            every { transcriptions } returns transcriptionFlow
            every { partialTranscriptions } returns partialFlow
            every { errors } returns errorFlow
        }
        sueAgent = mockk(relaxed = true)
        inferenceRepository = mockk(relaxed = true) {
            every { isModelLoaded } returns modelLoadedFlow
        }
        scheduleTools = mockk(relaxed = true)
        studentTools = mockk(relaxed = true)

        viewModel = SueViewModel(speechService, sueAgent, inferenceRepository, scheduleTools, studentTools)
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
            date = 1700000000000L,
            startTime = "09:00",
            endTime = "10:00"
        )
        val executeResult = SueOperationResult.Execute.Success(pendingAction)
        coEvery { scheduleTools.executeCancelAction(pendingAction) } returns executeResult
        coEvery { sueAgent.detectActionIntent(any()) } returns null

        // Inject the pending action into the VM's state
        // We do this by first having the agent return it from detectActionIntent
        coEvery { sueAgent.detectActionIntent("cancela la clase de María el lunes") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        // Simulate first transcription — sets pending action
        transcriptionFlow.emit("cancela la clase de María el lunes")
        advanceUntilIdle()

        // Then a "sí" transcription arrives
        transcriptionFlow.emit("sí")
        advanceUntilIdle()

        // Then — pending action cleared, response set to success message
        val expectedMessage = SueResponseFormatter.format(executeResult)
        val state: SueUiState = viewModel.uiState.value
        assertNull("Pending action should be cleared after execution", state.pendingAction)
        assertEquals(expectedMessage, state.agentResponse)
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
            date = 1700000000000L,
            startTime = "11:00",
            endTime = "12:00"
        )
        coEvery { sueAgent.detectActionIntent("cancela la clase de Juan el martes") } returns
                SueOperationResult.Prepare.Success(pendingAction)

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
            date = 1700000000000L, startTime = "10:00", endTime = "11:00"
        )
        coEvery { sueAgent.detectActionIntent("cancela la clase de Ana el miércoles") } returns
                SueOperationResult.Prepare.Success(pendingAction)

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
            date = 1700000000000L, startTime = "16:00", endTime = "17:00"
        )
        coEvery { sueAgent.detectActionIntent("cancela la clase de Pedro el jueves") } returns
                SueOperationResult.Prepare.Success(pendingAction)
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
        val executeResult = SueOperationResult.Execute.Success(paymentAction)
        coEvery { studentTools.executeRegisterPayment(paymentAction) } returns executeResult
        coEvery { sueAgent.detectActionIntent(any()) } returns null

        coEvery { sueAgent.detectActionIntent("registra un pago de maría") } returns
                SueOperationResult.Prepare.Success(paymentAction)
        
        transcriptionFlow.emit("registra un pago de maría")
        advanceUntilIdle()

        transcriptionFlow.emit("sí")
        advanceUntilIdle()

        val expectedMessage = SueResponseFormatter.format(executeResult)
        assertNull(viewModel.uiState.value.pendingAction)
        assertEquals(expectedMessage, viewModel.uiState.value.agentResponse)
        coVerify { studentTools.executeRegisterPayment(paymentAction) }
    }

    @Test
    fun `when pendingAction is AddBalance and user says si it should add balance`() = runTest {
        val addBalanceAction = SuePendingAction.AddBalance(
            studentName = "Juan",
            studentId = "student2",
            amount = 15.5
        )
        val executeResult = SueOperationResult.Execute.Success(addBalanceAction)
        coEvery { studentTools.executeAddBalance(addBalanceAction) } returns executeResult
        coEvery { sueAgent.detectActionIntent(any()) } returns null

        coEvery { sueAgent.detectActionIntent("suma 15.5 euros a la deuda de Juan") } returns
                SueOperationResult.Prepare.Success(addBalanceAction)
        
        transcriptionFlow.emit("suma 15.5 euros a la deuda de Juan")
        advanceUntilIdle()

        transcriptionFlow.emit("sí")
        advanceUntilIdle()

        val expectedMessage = SueResponseFormatter.format(executeResult)
        assertNull(viewModel.uiState.value.pendingAction)
        assertEquals(expectedMessage, viewModel.uiState.value.agentResponse)
        coVerify { studentTools.executeAddBalance(addBalanceAction) }
    }

    @Test
    fun `when execute returns conflict error it should append suggested free slots`() = runTest {
        val pendingAction = SuePendingAction.CreateSchedule(
            studentName = "María",
            studentId = "student1",
            dayOfWeek = 1,
            startTime = "09:00",
            endTime = "10:00"
        )
        val conflictError = SueOperationResult.Execute.Error(
            com.devsusana.hometutorpro.domain.core.DomainError.ConflictingStudent("Pedro", "09:00-10:00")
        )
        coEvery { scheduleTools.executeCreateSchedule(pendingAction) } returns conflictError
        coEvery { scheduleTools.getFreeSlots() } returns SueOperationResult.FreeSlots(listOf(3, 5))
        coEvery { sueAgent.detectActionIntent(any()) } returns null

        coEvery { sueAgent.detectActionIntent("agenda clase con maría") } returns
                SueOperationResult.Prepare.Success(pendingAction)

        transcriptionFlow.emit("agenda clase con maría")
        advanceUntilIdle()

        transcriptionFlow.emit("sí")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val expectedBase = "No se puede realizar la acción debido a un conflicto de horario. El hueco está ocupado por Pedro de 09:00-10:00."
        val expectedFree = "Los siguientes días están completamente libres esta semana: miércoles, viernes."
        assertEquals("$expectedBase $expectedFree", state.agentResponse)
        coVerify { scheduleTools.executeCreateSchedule(pendingAction) }
        coVerify { scheduleTools.getFreeSlots() }
    }
}
