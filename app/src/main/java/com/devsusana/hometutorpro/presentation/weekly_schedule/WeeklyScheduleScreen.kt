package com.devsusana.hometutorpro.presentation.weekly_schedule

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import com.devsusana.hometutorpro.presentation.components.ScheduleExceptionDialog
import com.devsusana.hometutorpro.presentation.schedule.components.MonthlyScheduleView
import com.devsusana.hometutorpro.presentation.schedule.components.WeeklyScheduleGrid
import com.devsusana.hometutorpro.presentation.schedule.components.WeeklyScheduleList
import com.devsusana.hometutorpro.presentation.student_detail.components.StartClassDialog
import com.devsusana.hometutorpro.presentation.utils.LockScreenOrientation
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Menu
import com.devsusana.hometutorpro.navigation.LocalNavigationControl
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import java.time.DayOfWeek

/**
 * Screen entry point for weekly schedules. Handles UI-only state and delegates actions to the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    viewModel: WeeklyScheduleViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onStudentListClick: () -> Unit,
    onResourcesClick: () -> Unit,
    onPremiumClick: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var isMonthlyView by rememberSaveable { mutableStateOf(false) }
    var showStartClassDialog by rememberSaveable { mutableStateOf(false) }
    var selectedStudentId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStudentName by rememberSaveable { mutableStateOf<String?>(null) }

    val navigationControl = LocalNavigationControl.current
    
    LaunchedEffect(isMonthlyView, isGridView) {
        navigationControl.setHideBottomBar(isMonthlyView || isGridView)
    }

    DisposableEffect(Unit) {
        onDispose {
            navigationControl.setHideBottomBar(false)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* No-op */ }
    )

    LockScreenOrientation(
        orientation = if (isGridView || isMonthlyView) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    )
    
    if (showStartClassDialog && selectedStudentId != null && selectedStudentName != null) {
        StartClassDialog(
            onDismiss = { 
                showStartClassDialog = false
                selectedStudentId = null
                selectedStudentName = null
            },
            onConfirm = { duration ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                
                viewModel.startClass(selectedStudentId ?: return@StartClassDialog, duration)
                showStartClassDialog = false
                selectedStudentId = null
                selectedStudentName = null
            }
        )
    }

    WeeklyScheduleContent(
        state = state,
        isMonthlyView = isMonthlyView,
        isGridView = isGridView,
        onToggleMonthlyView = { isMonthlyView = !isMonthlyView },
        onToggleGridView = { isGridView = !isGridView },
        onScheduleClick = viewModel::onScheduleClick,
        snackbarHostState = snackbarHostState,
        onOpenDrawer = navigationControl.openDrawer
    )

    val selectedSchedule = state.selectedSchedule
    if (state.showExceptionDialog && selectedSchedule != null) {
        val allRegularSchedules = state.schedulesByDay.values.flatten().filterIsInstance<WeeklyScheduleItem.Regular>()

        key(selectedSchedule.schedule.id) {
            ScheduleExceptionDialog(
                item = selectedSchedule,
                allRegularSchedules = allRegularSchedules,
                isSaving = state.isLoading,
                onDismiss = viewModel::dismissDialog,
                onSave = viewModel::saveException,
                onDelete = viewModel::deleteException,
                onStartClass = { studentId, studentName ->
                    selectedStudentId = studentId
                    selectedStudentName = studentName
                    showStartClassDialog = true
                },
                onAddExtraClass = viewModel::openAddExtraClassDialog
            )
        }
    }

    if (state.showExtraClassDialog) {
        com.devsusana.hometutorpro.presentation.student_detail.components.AddExtraClassDialog(
            onDismiss = viewModel::closeAddExtraClassDialog,
            onConfirm = { date, start, end, dayOfWeek -> viewModel.saveExtraClass(date, start, end, dayOfWeek) }
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

