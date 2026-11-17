package com.devsusana.hometutorpro.presentation.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.repository.IConfigRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ClassTimerWidgetKillSwitchRobolectricTest {

    private lateinit var context: Context
    private lateinit var configRepository: IConfigRepository
    private lateinit var widget: ClassTimerWidget

    private val sampleStudent = StudentSummary(
        id = "student_1",
        name = "Lucas Martinez",
        subjects = "Math",
        color = 0xFF4285F4.toInt(),
        pendingBalance = 25.0,
        pricePerHour = 15.0,
        isActive = true,
        lastClassDate = null
    )

    private val sampleNextClass = CalendarOccurrence(
        schedule = Schedule(
            id = "sched_1",
            studentId = "student_1",
            startTime = "17:00",
            endTime = "18:00"
        ),
        student = sampleStudent,
        date = LocalDate.now()
    )

    private val sampleActiveSession = ActiveSession(
        studentId = "student_1",
        studentName = "Lucas Martinez",
        startTimeMillis = System.currentTimeMillis() - 60000L,
        durationMinutes = 60L,
        isOngoing = true,
        tutorId = "tutor_1"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        configRepository = mockk(relaxed = true)
        widget = ClassTimerWidget()
        widget.configRepository = configRepository
    }

    @Test
    fun `setupWidgetUi with active session and actions enabled configures stop action`() {
        every { configRepository.isWidgetActionsEnabled() } returns true

        val views = RemoteViews(context.packageName, R.layout.widget_class_timer_active)
        widget.setupWidgetUi(
            context = context,
            views = views,
            activeSession = sampleActiveSession,
            nextClass = null,
            currentTutorId = "tutor_1"
        )
    }

    @Test
    fun `setupWidgetUi with active session and actions disabled via kill-switch still configures stop action`() {
        every { configRepository.isWidgetActionsEnabled() } returns false

        val views = RemoteViews(context.packageName, R.layout.widget_class_timer_active)
        widget.setupWidgetUi(
            context = context,
            views = views,
            activeSession = sampleActiveSession,
            nextClass = null,
            currentTutorId = "tutor_1"
        )
    }

    @Test
    fun `setupWidgetUi with upcoming class and actions enabled configures start button`() {
        every { configRepository.isWidgetActionsEnabled() } returns true

        val views = RemoteViews(context.packageName, R.layout.widget_class_timer)
        widget.setupWidgetUi(
            context = context,
            views = views,
            activeSession = null,
            nextClass = sampleNextClass,
            currentTutorId = "tutor_1"
        )
    }

    @Test
    fun `setupWidgetUi with upcoming class and actions disabled hides start button`() {
        every { configRepository.isWidgetActionsEnabled() } returns false

        val views = RemoteViews(context.packageName, R.layout.widget_class_timer)
        widget.setupWidgetUi(
            context = context,
            views = views,
            activeSession = null,
            nextClass = sampleNextClass,
            currentTutorId = "tutor_1"
        )
    }

    @Test
    fun `whenActionsToggledFromFalseToTrue_startButtonReappears`() {
        val viewsDisabled = RemoteViews(context.packageName, R.layout.widget_class_timer)
        every { configRepository.isWidgetActionsEnabled() } returns false
        widget.setupWidgetUi(
            context = context,
            views = viewsDisabled,
            activeSession = null,
            nextClass = sampleNextClass,
            currentTutorId = "tutor_1"
        )

        val viewsEnabled = RemoteViews(context.packageName, R.layout.widget_class_timer)
        every { configRepository.isWidgetActionsEnabled() } returns true
        widget.setupWidgetUi(
            context = context,
            views = viewsEnabled,
            activeSession = null,
            nextClass = sampleNextClass,
            currentTutorId = "tutor_1"
        )
    }

    @Test
    fun `setupWidgetUi with config exception defaults to actions enabled safely`() {
        every { configRepository.isWidgetActionsEnabled() } throws RuntimeException("Config error")

        val views = RemoteViews(context.packageName, R.layout.widget_class_timer)
        widget.setupWidgetUi(
            context = context,
            views = views,
            activeSession = null,
            nextClass = sampleNextClass,
            currentTutorId = "tutor_1"
        )
    }
}
