package com.devsusana.hometutorpro.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.components.ScheduleExceptionDialog
import com.devsusana.hometutorpro.presentation.student_detail.components.StartClassDialog
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import com.devsusana.hometutorpro.presentation.utils.ColorUtils
import com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.devsusana.hometutorpro.presentation.dashboard.components.DashboardStatCard
import com.devsusana.hometutorpro.presentation.dashboard.components.NextClassCard
import com.devsusana.hometutorpro.presentation.dashboard.components.QuickActionButton

@Composable
fun DashboardScreen(
    onNavigateToStudents: () -> Unit,
    onNavigateToResources: () -> Unit,
    onAddStudent: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text(
                    text = today.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.dashboard_hello)}, ${state.userName}",
                    style = MaterialTheme.typography.displaySmall, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardStatCard(
                    title = stringResource(R.string.dashboard_pending_classes_today),
                    value = state.todayPendingClassesCount.toString(),
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                DashboardStatCard(
                    title = stringResource(R.string.dashboard_pending_income),
                    value = "${String.format("%.0f", state.totalPendingIncome)}", 
                    icon = Icons.Default.EuroSymbol,
                    color = MaterialTheme.colorScheme.secondary, 
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToStudents() }
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.dashboard_next_class),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (state.nextClass != null) {
                NextClassCard(
                    item = state.nextClass!!,
                    onClick = { viewModel.onScheduleClick(state.nextClass!!) }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.dashboard_no_upcoming_classes), 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.dashboard_quick_actions),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionButton(
                    title = stringResource(R.string.dashboard_action_add_student),
                    subtitle = stringResource(R.string.dashboard_action_add_student_subtitle),
                    icon = Icons.Default.PersonAdd,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onAddStudent
                )
                
                QuickActionButton(
                    title = stringResource(R.string.dashboard_action_view_students),
                    subtitle = stringResource(R.string.dashboard_action_view_students_subtitle),
                    icon = Icons.Default.School,
                    color = MaterialTheme.colorScheme.tertiary, 
                    onClick = onNavigateToStudents
                )

                QuickActionButton(
                    title = stringResource(R.string.dashboard_action_shared_resources),
                    subtitle = stringResource(R.string.dashboard_action_shared_resources_subtitle),
                    icon = Icons.Default.Folder,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToResources
                )
            }
        }
    }

    var showStartClassDialog by remember { mutableStateOf(false) }
    var selectedStudentForClass by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    // Extra Class Dialog state
    var showExtraClassDialog by remember { mutableStateOf(false) }
    var studentIdForExtraClass by remember { mutableStateOf<String?>(null) }

    if (state.showExceptionDialog && state.selectedSchedule != null) {
        ScheduleExceptionDialog(
            item = state.selectedSchedule!!,
            allRegularSchedules = state.allSchedules,
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
            onConfirm = { dateMillis, startStr, endStr ->
                studentIdForExtraClass?.let { studentId ->
                    viewModel.addExtraClass(studentId, dateMillis, startStr, endStr)
                }
                showExtraClassDialog = false
                studentIdForExtraClass = null
            }
        )
    }
    
    if (state.successMessage != null) {
        FeedbackDialog(
            isSuccess = true,
            message = {
                when (val message = state.successMessage) {
                    is Int -> Text(stringResource(id = message))
                    is String -> Text(message)
                    is Pair<*, *> -> {
                        val resId = message.first as Int
                        val arg = message.second ?: ""
                        Text(stringResource(id = resId, arg))
                    }
                }
            },
            onDismiss = viewModel::clearFeedback
        )
    }

    if (state.errorMessage != null) {
        FeedbackDialog(
            isSuccess = false,
            message = {
                when (val message = state.errorMessage) {
                    is Int -> Text(stringResource(id = message))
                    is String -> Text(message)
                }
            },
            onDismiss = viewModel::clearFeedback
        )
    }
}






