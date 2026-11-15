package com.devsusana.hometutorpro.presentation.student_detail.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Host composable managing all overlays and dialogs for the Student Detail screen:
 * feedback, file sharing, payment registration, starting class, and extra class addition.
 */
@Composable
fun StudentDetailDialogHost(
    context: Context,
    scope: CoroutineScope,
    state: StudentDetailState,
    student: Student?,
    snackbarHostState: SnackbarHostState,
    showPaymentDialog: Boolean,
    selectedPaymentType: PaymentType?,
    onDismissPaymentDialog: () -> Unit,
    showStartClassDialog: Boolean,
    onDismissStartClassDialog: () -> Unit,
    notificationPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onEvent: (StudentDetailEvent) -> Unit
) {
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

    if (showStartClassDialog) {
        StartClassDialog(
            onDismiss = onDismissStartClassDialog,
            onConfirm = { duration ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val scheduled = NotificationHelper.scheduleClassEndNotification(
                    context,
                    student?.name ?: newStudentString,
                    duration.toLong()
                )

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
            onConfirm = { date, start, end, dayOfWeek ->
                onEvent(StudentDetailEvent.SaveExtraClass(date, start, end, dayOfWeek))
            }
        )
    }
}
