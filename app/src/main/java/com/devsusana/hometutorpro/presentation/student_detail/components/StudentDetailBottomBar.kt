package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailEvent
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState

/**
 * Bottom action bar shown in edit mode with Save, Continue, and Delete actions.
 *
 * @param state Current UI state for student detail.
 * @param isNewStudent Whether this screen represents creating a new student.
 * @param showDeleteDialog Whether the delete confirmation dialog is visible.
 * @param onEvent Callback to dispatch UI events.
 * @param onSetEditMode Callback to toggle edit mode.
 * @param onShowDeleteDialog Callback to request showing the delete dialog.
 * @param onDismissDeleteDialog Callback to dismiss the delete dialog.
 */
@Composable
fun StudentDetailBottomBar(
    state: StudentDetailState,
    isNewStudent: Boolean,
    showDeleteDialog: Boolean,
    onEvent: (StudentDetailEvent) -> Unit,
    onSetEditMode: (Boolean) -> Unit,
    onShowDeleteDialog: () -> Unit,
    onDismissDeleteDialog: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
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
                            if (state.isBalanceEditable) {
                                onEvent(StudentDetailEvent.ToggleBalanceEdit)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_student_button"),
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
                                Text(
                                    stringResource(id = R.string.student_detail_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
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
