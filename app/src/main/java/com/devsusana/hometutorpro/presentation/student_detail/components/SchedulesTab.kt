package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import com.devsusana.hometutorpro.domain.entities.Schedule
import java.time.format.TextStyle
import java.util.Locale
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

/**
 * Tab for managing student schedules with bulk creation support.
 */
@Composable
fun SchedulesTab(
    student: Student,
    state: StudentDetailState,
    isNewStudent: Boolean,
    onBulkScheduleModeToggle: () -> Unit,
    onBulkSchedulesChange: (List<BulkScheduleEntry>) -> Unit,
    onSaveBulkSchedules: () -> Unit,
    onDeleteSchedule: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isBulkScheduleMode) {
        BulkScheduleCreation(
            studentId = student.id,
            schedules = state.bulkSchedules,
            onSchedulesChange = onBulkSchedulesChange,
            onSaveAll = onSaveBulkSchedules,
            onCancel = onBulkScheduleModeToggle,
            isLoading = state.bulkScheduleSaving
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Button to add schedules (always visible now)
            Button(
                onClick = onBulkScheduleModeToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_schedules_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.bulk_schedule_create))
            }

            // List of schedules - show pending for new students, saved for existing
            val schedulesToShow = if (isNewStudent) state.pendingSchedules else state.schedules
            
            if (schedulesToShow.isNotEmpty()) {
                if (isNewStudent) {
                    Text(
                        text = stringResource(R.string.student_detail_pending_schedules_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = stringResource(R.string.student_detail_tab_schedules),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                schedulesToShow.forEach { schedule ->
                    ScheduleItem(
                        schedule = schedule,
                        onDelete = { onDeleteSchedule(schedule.id) }
                    )
                }
            } else {
                // Info card when no schedules
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isNewStudent) 
                                stringResource(R.string.student_detail_save_first_schedule_tip)
                            else
                                stringResource(R.string.no_schedules),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ScheduleItem(
    schedule: Schedule,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val dayName = schedule.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${schedule.startTime} - ${schedule.endTime}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.student_detail_delete), 
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Schedules Tab - New Student")
@Composable
private fun SchedulesTabNewStudentPreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        studentEmail = "susana@example.com",
        isActive = true
    )
    
    HomeTutorProTheme {
        SchedulesTab(
            student = mockStudent,
            state = StudentDetailState(student = mockStudent),
            isNewStudent = true,
            onBulkScheduleModeToggle = {},
            onBulkSchedulesChange = {},
            onSaveBulkSchedules = {},
            onDeleteSchedule = {}
        )
    }
}

@Preview(showBackground = true, name = "Schedules Tab - Existing Student")
@Composable
private fun SchedulesTabExistingStudentPreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        studentEmail = "susana@example.com",
        isActive = true
    )
    
    HomeTutorProTheme {
        SchedulesTab(
            student = mockStudent,
            state = StudentDetailState(student = mockStudent, isBulkScheduleMode = false),
            isNewStudent = false,
            onBulkScheduleModeToggle = {},
            onBulkSchedulesChange = {},
            onSaveBulkSchedules = {},
            onDeleteSchedule = {}
        )
    }
}
