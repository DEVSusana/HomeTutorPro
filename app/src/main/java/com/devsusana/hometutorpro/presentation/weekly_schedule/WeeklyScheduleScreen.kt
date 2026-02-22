package com.devsusana.hometutorpro.presentation.weekly_schedule

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.NotificationHelper
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
    val state by viewModel.state.collectAsState()
    var isGridView by remember { mutableStateOf(false) }
    var isMonthlyView by remember { mutableStateOf(false) }
    var showStartClassDialog by remember { mutableStateOf(false) }
    var selectedStudentForClass by remember { mutableStateOf<Pair<String, String>?>(null) } // studentId, studentName

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
    
    if (showStartClassDialog && selectedStudentForClass != null) {
        StartClassDialog(
            onDismiss = { 
                showStartClassDialog = false
                selectedStudentForClass = null
            },
            onConfirm = { duration ->
                selectedStudentForClass?.let { (studentId, studentName) ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    
                    viewModel.startClass(studentId, duration)
                }
                showStartClassDialog = false
                selectedStudentForClass = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(if (isMonthlyView) R.string.monthly_schedule else R.string.weekly_schedule)
                    ) 
                },
                navigationIcon = {
                    if (isMonthlyView || isGridView) {
                        FilledTonalIconButton(
                            onClick = navigationControl.openDrawer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.cd_menu)
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { isMonthlyView = !isMonthlyView }) {
                        Text(
                            text = stringResource(if (isMonthlyView) R.string.schedule_view_weekly else R.string.schedule_view_monthly),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isMonthlyView) {
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier.testTag(if (isGridView) "list_view_button" else "grid_view_button")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = stringResource(if (isGridView) R.string.cd_list_view else R.string.cd_grid_view)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (isMonthlyView) {
                    MonthlyScheduleView(
                        schedulesByDay = state.schedulesByDay,
                        onScheduleClick = viewModel::onScheduleClick,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isGridView) {
                    WeeklyScheduleGrid(
                        schedulesByDay = state.schedulesByDay,
                        onScheduleClick = viewModel::onScheduleClick,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    WeeklyScheduleList(
                        schedulesByDay = state.schedulesByDay,
                        onScheduleClick = { viewModel.onScheduleClick(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }


            if (state.showExceptionDialog && state.selectedSchedule != null) {
                val allRegularSchedules = state.schedulesByDay.values.flatten().filterIsInstance<WeeklyScheduleItem.Regular>()

                key(state.selectedSchedule!!.schedule.id) {
                    ScheduleExceptionDialog(
                        item = state.selectedSchedule!!,
                        allRegularSchedules = allRegularSchedules,
                        onDismiss = viewModel::dismissDialog,
                        onSave = viewModel::saveException,
                        onDelete = viewModel::deleteException,
                        onStartClass = { studentId, studentName ->
                            selectedStudentForClass = Pair(studentId, studentName)
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
        }
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
