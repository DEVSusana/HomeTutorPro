package com.devsusana.hometutorpro.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.components.ScheduleExceptionDialog
import com.devsusana.hometutorpro.presentation.student_detail.components.StartClassDialog
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.devsusana.hometutorpro.presentation.dashboard.components.DashboardStatCard
import com.devsusana.hometutorpro.presentation.dashboard.components.NextClassCard
import com.devsusana.hometutorpro.presentation.dashboard.components.QuickActionButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

/**
 * Screen entry point for the dashboard. Collects state and wires UI events to the ViewModel.
 */
@Composable
fun DashboardScreen(
    onNavigateToStudents: () -> Unit,
    onNavigateToResources: () -> Unit,
    onAddStudent: () -> Unit,
    onNavigateToNotes: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showStartClassDialog by rememberSaveable { mutableStateOf(false) }
    var selectedStudentForClass by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var showExtraClassDialog by rememberSaveable { mutableStateOf(false) }
    var studentIdForExtraClass by rememberSaveable { mutableStateOf<String?>(null) }
    
    DashboardContent(
        state = state,
        onNavigateToStudents = onNavigateToStudents,
        onNavigateToResources = onNavigateToResources,
        onAddStudent = onAddStudent,
        onNavigateToNotes = onNavigateToNotes,
        onScheduleClick = { item -> if (item is WeeklyScheduleItem.Regular) viewModel.onScheduleClick(item) }
    )

    val selectedSchedule = state.selectedSchedule
    if (state.showExceptionDialog && selectedSchedule != null) {
        ScheduleExceptionDialog(
            item = selectedSchedule,
            allRegularSchedules = state.allSchedules,
            isSaving = state.isLoading,
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::saveException,
            onDelete = viewModel::deleteException,
            onStartClass = { studentId, studentName ->
                selectedStudentForClass = Pair(studentId, studentName)
                showStartClassDialog = true
            },
            onAddExtraClass = { studentId ->
                studentIdForExtraClass = studentId
                showExtraClassDialog = true
            }
        )
    }

    if (showStartClassDialog && selectedStudentForClass != null) {
        StartClassDialog(
            onDismiss = { 
                showStartClassDialog = false
                selectedStudentForClass = null
            },
            onConfirm = { duration ->
                selectedStudentForClass?.let { (studentId, _) ->
                    viewModel.startClass(studentId, duration)
                }
                showStartClassDialog = false
                selectedStudentForClass = null
            }
        )
    }

    if (showExtraClassDialog && studentIdForExtraClass != null) {
        com.devsusana.hometutorpro.presentation.student_detail.components.AddExtraClassDialog(
            onDismiss = { 
                showExtraClassDialog = false
                studentIdForExtraClass = null
            },
            onConfirm = { dateMillis, startStr, endStr, dayOfWeek ->
                studentIdForExtraClass?.let { studentId ->
                    viewModel.addExtraClass(studentId, dateMillis, startStr, endStr, dayOfWeek)
                }
                showExtraClassDialog = false
                studentIdForExtraClass = null
            }
        )
    }
    
    val successMessage = state.successMessage
    if (successMessage != null) {
        FeedbackDialog(
            isSuccess = true,
            message = { Text(successMessage) },
            onDismiss = viewModel::clearFeedback
        )
    }

    val errorMessage = state.errorMessage
    if (errorMessage != null) {
        FeedbackDialog(
            isSuccess = false,
            message = { Text(errorMessage) },
            onDismiss = viewModel::clearFeedback
        )
    }
}

