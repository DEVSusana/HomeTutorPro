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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.presentation.dashboard.components.DashboardStatCard
import com.devsusana.hometutorpro.presentation.dashboard.components.NextClassCard
import com.devsusana.hometutorpro.presentation.dashboard.components.QuickActionButton
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Stateless dashboard UI that renders content based on [state] and emits UI events.
 */
@Composable
fun DashboardContent(
    state: DashboardState,
    onNavigateToStudents: () -> Unit,
    onNavigateToResources: () -> Unit,
    onAddStudent: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onScheduleClick: (WeeklyScheduleItem) -> Unit
) {
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
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

            Column {
                Text(
                    text = stringResource(R.string.dashboard_next_class),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (state.nextClass != null) {
                    NextClassCard(
                        item = state.nextClass,
                        onClick = { onScheduleClick(state.nextClass) }
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

            Column {
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

                    QuickActionButton(
                        title = stringResource(R.string.dashboard_action_notes),
                        subtitle = stringResource(R.string.dashboard_action_notes_subtitle),
                        icon = Icons.Default.Edit,
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = onNavigateToNotes
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, name = "Dashboard Content")
@Composable
private fun DashboardContentPreview() {
    val sampleOccurrence = CalendarOccurrence(
        schedule = Schedule(
            id = "schedule1",
            studentId = "student1",
            startTime = "10:00",
            endTime = "11:00"
        ),
        student = StudentSummary(
            id = "student1",
            name = "Ana",
            subjects = "Math",
            color = null,
            pendingBalance = 0.0,
            pricePerHour = 20.0,
            isActive = true,
            lastClassDate = null
        ),
        date = LocalDate.now()
    )

    HomeTutorProTheme {
        DashboardContent(
            state = DashboardState(
                activeStudentsCount = 4,
                todayPendingClassesCount = 2,
                totalPendingIncome = 120.0,
                classesThisWeek = 6,
                nextClass = WeeklyScheduleItem.Regular(sampleOccurrence),
                isLoading = false,
                userName = "Susana"
            ),
            onNavigateToStudents = {},
            onNavigateToResources = {},
            onAddStudent = {},
            onNavigateToNotes = {},
            onScheduleClick = {}
        )
    }
}
