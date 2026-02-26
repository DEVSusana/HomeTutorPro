package com.devsusana.hometutorpro.presentation.student_detail.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.FilePickerHelper
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import com.devsusana.hometutorpro.presentation.components.PaymentDialog
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailEvent
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import kotlinx.coroutines.launch

/**
 * Stateless student detail UI. All state is provided by [state] and UI-only flags/handlers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailContent(
    state: StudentDetailState,
    onEvent: (StudentDetailEvent) -> Unit,
    onBack: () -> Unit,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onSetEditMode: (Boolean) -> Unit,
    showPaymentDialog: Boolean,
    selectedPaymentType: PaymentType?,
    onPaymentClick: () -> Unit,
    onDismissPaymentDialog: () -> Unit,
    showStartClassDialog: Boolean,
    onStartClassClick: () -> Unit,
    onDismissStartClassDialog: () -> Unit,
    showDeleteDialog: Boolean,
    onShowDeleteDialog: () -> Unit,
    onDismissDeleteDialog: () -> Unit
) {
    val context = LocalContext.current
    val student = state.student
    val isNewStudent = student?.id?.isEmpty() == true || student?.id == "new"
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileInfo = FilePickerHelper.getFileInfo(context, it)
            fileInfo?.let { info ->
                onEvent(StudentDetailEvent.FileSelected(info.uri, info.name, info.type, info.sizeBytes))
            }
        }
    }

    val successMsg = state.successMessage
    val errorMsg = state.errorMessage
    if (successMsg != null || errorMsg != null) {
        FeedbackDialog(
            isSuccess = successMsg != null,
            message = {
                Text(text = successMsg ?: errorMsg ?: "")
            },
            onDismiss = { onEvent(StudentDetailEvent.ClearFeedback) }
        )
    }
    
    if (state.showShareDialog && state.selectedFileUri != null && student != null) {
        ShareResourceDialog(
            student = student,
            fileUri = state.selectedFileUri,
            fileName = state.selectedFileName,
            fileType = "",
            fileSizeBytes = 0L,
            notes = state.shareNotes,
            onNotesChange = { onEvent(StudentDetailEvent.ShareNotesChange(it)) },
            onShare = { method ->
                val fileInfo = FilePickerHelper.getFileInfo(context, state.selectedFileUri)
                fileInfo?.let {
                    onEvent(StudentDetailEvent.ShareResource(method, it.type, it.sizeBytes))
                }
            },
            onDismiss = { onEvent(StudentDetailEvent.DismissShareDialog) }
        )
    }

    val paymentType = selectedPaymentType
    if (showPaymentDialog && paymentType != null && student != null) {
        PaymentDialog(
            defaultAmount = student.pricePerHour,
            onDismiss = onDismissPaymentDialog,
            onConfirm = { amount ->
                onEvent(StudentDetailEvent.RegisterPayment(amount, paymentType))
                onDismissPaymentDialog()
            }
        )
    }

    val newStudentString = stringResource(id = R.string.student_detail_new_student)
    val permissionNeededText = stringResource(R.string.notification_permission_needed)
    val settingsText = stringResource(R.string.settings)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    if (showStartClassDialog) {
        StartClassDialog(
            onDismiss = onDismissStartClassDialog,
            onConfirm = { duration ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val scheduled = NotificationHelper.scheduleClassEndNotification(context, student?.name ?: newStudentString, duration.toLong())
                
                
                if (!scheduled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = permissionNeededText,
                            actionLabel = settingsText,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                }
                
                onEvent(StudentDetailEvent.StartClass(duration))
                onDismissStartClassDialog()
            }
        )
    }

    if (state.showExtraClassDialog) {
        AddExtraClassDialog(
            onDismiss = { onEvent(StudentDetailEvent.HideExtraClassDialog) },
            onConfirm = { date, start, end, dayOfWeek -> onEvent(StudentDetailEvent.SaveExtraClass(date, start, end, dayOfWeek)) }
        )
    }

    Scaffold(
        modifier = Modifier.testTag("student_detail_screen"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNewStudent) stringResource(id = R.string.student_detail_new_student) else student?.name ?: "",
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.student_detail_back))
                    }
                },
                actions = {
                    if (!isNewStudent) {
                        IconButton(
                            onClick = onToggleEditMode,
                            modifier = Modifier.testTag("edit_button")
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = if (isEditMode) stringResource(id = R.string.student_detail_cancel_edit) else stringResource(id = R.string.student_detail_edit),
                                tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (isEditMode && student != null) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                if (isNewStudent && state.currentTab < 2) {
                                    onEvent(StudentDetailEvent.ContinueToNextStep)
                                } else {
                                    onEvent(StudentDetailEvent.SaveStudent)
                                    if (!isNewStudent) {
                                        onSetEditMode(false)
                                        if(state.isBalanceEditable) {
                                            onEvent(StudentDetailEvent.ToggleBalanceEdit)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag(
                                if (isNewStudent && state.currentTab < 2) "continue_button" else "save_student_button"
                            ),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                if (isNewStudent) {
                                    if (state.currentTab < 2) stringResource(id = R.string.student_detail_continue) 
                                    else stringResource(id = R.string.student_detail_create_student)
                                } else {
                                    stringResource(id = R.string.student_detail_save_changes)
                                }
                            )
                        }

                        if (!isNewStudent) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedButton(
                                onClick = onShowDeleteDialog,
                                modifier = Modifier.fillMaxWidth().testTag("delete_student_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(id = R.string.student_detail_delete_student))
                            }

                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = onDismissDeleteDialog,
                                    title = { Text(stringResource(id = R.string.student_detail_delete_student_dialog_title)) },
                                    text = { Text(stringResource(id = R.string.student_detail_delete_student_dialog_text)) },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                onEvent(StudentDetailEvent.DeleteStudent)
                                                onDismissDeleteDialog()
                                            },
                                            modifier = Modifier.testTag("confirm_delete_button")
                                        ) {
                                            Text(stringResource(id = R.string.student_detail_delete), color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = onDismissDeleteDialog) {
                                            Text(stringResource(id = R.string.student_detail_cancel))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            student != null -> {
                val tabs = listOf(
                    stringResource(R.string.student_detail_tab_personal),
                    stringResource(R.string.student_detail_tab_schedules),
                    stringResource(R.string.student_detail_tab_finance),
                    stringResource(R.string.student_detail_tab_resources)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    val selectedStateDescription = stringResource(R.string.cd_state_selected)
                    val notSelectedStateDescription = stringResource(R.string.cd_state_not_selected)
                    PrimaryTabRow(
                        selectedTabIndex = state.currentTab,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = state.currentTab == index,
                                onClick = { onEvent(StudentDetailEvent.TabChange(index)) },
                                text = { 
                                    Text(
                                        text = title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    ) 
                                },
                                modifier = Modifier.semantics {
                                    stateDescription = if (state.currentTab == index) {
                                        selectedStateDescription
                                    } else {
                                        notSelectedStateDescription
                                    }
                                }
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (state.currentTab) {
                            0 -> PersonalInfoTab(
                                student = student,
                                isEditMode = isEditMode,
                                isNewStudent = isNewStudent,
                                context = context,
                                onEvent = onEvent
                            )
                            1 -> SchedulesTab(
                                student = student,
                                state = state,
                                isNewStudent = isNewStudent,
                                onEvent = onEvent
                            )
                            2 -> FinanceTab(
                                student = student,
                                state = state,
                                isEditMode = isEditMode,
                                isNewStudent = isNewStudent,
                                onEvent = onEvent,
                                onPaymentClick = onPaymentClick,
                                onStartClassClick = onStartClassClick
                            )
                            3 -> ResourcesTab(
                                student = student,
                                isNewStudent = isNewStudent,
                                sharedResources = state.sharedResources,
                                onSelectFileClick = { filePickerLauncher.launch("*/*") },
                                onEvent = onEvent
                            )
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.student_not_found))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Student Detail - View Student")
@Composable
private fun StudentDetailContentNewStudentPreview() {
    HomeTutorProTheme {
        StudentDetailContent(
            state = StudentDetailState(student = Student(id = "new")),
            onEvent = {},
            onBack = {},
            isEditMode = true,
            onToggleEditMode = {},
            onSetEditMode = {},
            showPaymentDialog = false,
            selectedPaymentType = null,
            onPaymentClick = {},
            onDismissPaymentDialog = {},
            showStartClassDialog = false,
            onStartClassClick = {},
            onDismissStartClassDialog = {},
            showDeleteDialog = false,
            onShowDeleteDialog = {},
            onDismissDeleteDialog = {}
        )
    }
}

@Preview(showBackground = true, name = "Student Detail - View Student")
@Composable
private fun StudentDetailContentViewStudentPreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana Gonzalez",
        age = 25,
        course = "Kotlin",
        pendingBalance = 150.0,
        pricePerHour = 50.0,
        address = "123 Main Street, Anytown",
        educationalAttention = "Extra time for exams",
        parentPhones = "123-456-7890",
        studentPhone = "987-654-3210",
        studentEmail = "student@example.com",
        subjects = "Math, Science"
    )
    HomeTutorProTheme {
        StudentDetailContent(
            state = StudentDetailState(student = mockStudent),
            onEvent = {},
            onBack = {},
            isEditMode = false,
            onToggleEditMode = {},
            onSetEditMode = {},
            showPaymentDialog = false,
            selectedPaymentType = null,
            onPaymentClick = {},
            onDismissPaymentDialog = {},
            showStartClassDialog = false,
            onStartClassClick = {},
            onDismissStartClassDialog = {},
            showDeleteDialog = false,
            onShowDeleteDialog = {},
            onDismissDeleteDialog = {}
        )
    }
}

@Preview(showBackground = true, name = "Student Detail - Loading")
@Composable
private fun StudentDetailContentLoadingPreview() {
    HomeTutorProTheme {
        StudentDetailContent(
            state = StudentDetailState(isLoading = true),
            onEvent = {},
            onBack = {},
            isEditMode = false,
            onToggleEditMode = {},
            onSetEditMode = {},
            showPaymentDialog = false,
            selectedPaymentType = null,
            onPaymentClick = {},
            onDismissPaymentDialog = {},
            showStartClassDialog = false,
            onStartClassClick = {},
            onDismissStartClassDialog = {},
            showDeleteDialog = false,
            onShowDeleteDialog = {},
            onDismissDeleteDialog = {}
        )
    }
}

@Preview(showBackground = true, name = "Student Detail - Not Found")
@Composable
private fun StudentDetailContentNotFoundPreview() {
    HomeTutorProTheme {
        StudentDetailContent(
            state = StudentDetailState(student = null),
            onEvent = {},
            onBack = {},
            isEditMode = false,
            onToggleEditMode = {},
            onSetEditMode = {},
            showPaymentDialog = false,
            selectedPaymentType = null,
            onPaymentClick = {},
            onDismissPaymentDialog = {},
            showStartClassDialog = false,
            onStartClassClick = {},
            onDismissStartClassDialog = {},
            showDeleteDialog = false,
            onShowDeleteDialog = {},
            onDismissDeleteDialog = {}
        )
    }
}
