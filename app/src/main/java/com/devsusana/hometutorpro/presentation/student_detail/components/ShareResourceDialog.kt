package com.devsusana.hometutorpro.presentation.student_detail.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.FilePickerHelper
import com.devsusana.hometutorpro.core.utils.ShareHelper
import com.devsusana.hometutorpro.domain.entities.ShareMethod
import com.devsusana.hometutorpro.domain.entities.Student

/**
 * Dialog for sharing a file with a student.
 * Allows selecting share method (Email/WhatsApp) and adding notes.
 */
@Composable
fun ShareResourceDialog(
    student: Student,
    fileUri: Uri,
    fileName: String,
    fileType: String,
    fileSizeBytes: Long,
    notes: String,
    onNotesChange: (String) -> Unit,
    onShare: (ShareMethod) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasEmail = student.studentEmail?.isNotBlank()
    val hasPhone = student.studentPhone.isNotBlank()
    
    // Determine initial selection based on availability
    var selectedMethod by remember { 
        mutableStateOf(
            when {
                hasEmail == true -> ShareMethod.EMAIL
                hasPhone -> ShareMethod.WHATSAPP
                else -> ShareMethod.EMAIL // Default fallback
            }
        ) 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.student_detail_share_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // File info
                Text(
                    text = stringResource(R.string.student_detail_file_name, fileName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(
                        R.string.student_detail_file_size,
                        FilePickerHelper.formatFileSize(fileSizeBytes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider()
                
                // Share method selection
                Text(
                    text = stringResource(R.string.student_detail_share_method),
                    style = MaterialTheme.typography.labelLarge
                )
                
                val canShareEmail = hasEmail == true
                val canShareWhatsApp = hasPhone
                
                if (!canShareEmail && !canShareWhatsApp) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Este alumno no tiene email ni teléfono configurado. Añade estos datos en la pestaña Personal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val selectedStateDescription = stringResource(R.string.cd_state_selected)
                    val notSelectedStateDescription = stringResource(R.string.cd_state_not_selected)
                    FilterChip(
                        selected = selectedMethod == ShareMethod.EMAIL,
                        onClick = { if (canShareEmail) selectedMethod = ShareMethod.EMAIL },
                        label = { 
                            Text(
                                stringResource(R.string.student_detail_share_via_email) + 
                                if (!canShareEmail) " (sin email)" else ""
                            )
                        },
                        enabled = canShareEmail,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                stateDescription = if (selectedMethod == ShareMethod.EMAIL) {
                                    selectedStateDescription
                                } else {
                                    notSelectedStateDescription
                                }
                            }
                    )
                    FilterChip(
                        selected = selectedMethod == ShareMethod.WHATSAPP,
                        onClick = { if (canShareWhatsApp) selectedMethod = ShareMethod.WHATSAPP },
                        label = { 
                            Text(
                                stringResource(R.string.student_detail_share_via_whatsapp) + 
                                if (!canShareWhatsApp) " (sin tlf)" else ""
                            )
                        },
                        enabled = canShareWhatsApp,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                stateDescription = if (selectedMethod == ShareMethod.WHATSAPP) {
                                    selectedStateDescription
                                } else {
                                    notSelectedStateDescription
                                }
                            }
                    )
                }
                
                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.student_detail_share_notes)) },
                    placeholder = { Text(stringResource(R.string.student_detail_share_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // First share via the selected method
                    when (selectedMethod) {
                        ShareMethod.EMAIL -> ShareHelper.shareViaEmail(
                            context, student, fileUri, fileName, notes
                        )
                        ShareMethod.WHATSAPP -> ShareHelper.shareViaWhatsApp(
                            context, student, fileUri, fileName, notes
                        )
                    }
                    // Then save the record
                    onShare(selectedMethod)
                }
            ) {
                Text(stringResource(R.string.student_detail_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.student_detail_cancel))
            }
        }
    )
}
