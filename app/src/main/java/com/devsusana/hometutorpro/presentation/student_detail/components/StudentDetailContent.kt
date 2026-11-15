package com.devsusana.hometutorpro.presentation.student_detail.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.FilePickerHelper
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailEvent
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    StudentDetailDialogHost(
        context = context,
        scope = scope,
        state = state,
        student = student,
        snackbarHostState = snackbarHostState,
        showPaymentDialog = showPaymentDialog,
        selectedPaymentType = selectedPaymentType,
        onDismissPaymentDialog = onDismissPaymentDialog,
        showStartClassDialog = showStartClassDialog,
        onDismissStartClassDialog = onDismissStartClassDialog,
        notificationPermissionLauncher = notificationPermissionLauncher,
        onEvent = onEvent
    )

    Scaffold(
        modifier = Modifier.testTag("student_detail_screen"),
        containerColor = MaterialTheme.colorScheme.background,
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (isEditMode && student != null) {
                StudentDetailBottomBar(
                    state = state,
                    isNewStudent = isNewStudent,
                    showDeleteDialog = showDeleteDialog,
                    onEvent = onEvent,
                    onSetEditMode = onSetEditMode,
                    onShowDeleteDialog = onShowDeleteDialog,
                    onDismissDeleteDialog = onDismissDeleteDialog
                )
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
                                onEvent = onEvent,
                                onToggleEditMode = onToggleEditMode
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
