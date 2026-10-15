package com.devsusana.hometutorpro.presentation.weekly_schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.presentation.schedule.components.MonthlyScheduleView
import com.devsusana.hometutorpro.presentation.schedule.components.WeeklyScheduleGrid
import com.devsusana.hometutorpro.presentation.schedule.components.WeeklyScheduleList
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import java.time.DayOfWeek

/**
 * Stateless weekly schedule UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleContent(
    state: WeeklyScheduleState,
    isMonthlyView: Boolean,
    isGridView: Boolean,
    onToggleMonthlyView: () -> Unit,
    onToggleGridView: () -> Unit,
    onScheduleClick: (WeeklyScheduleItem.Regular) -> Unit,
    snackbarHostState: SnackbarHostState,
    onOpenDrawer: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(if (isMonthlyView) R.string.monthly_schedule else R.string.weekly_schedule),
                        modifier = Modifier.semantics { heading() }
                    ) 
                },
                navigationIcon = {
                    if (isMonthlyView || isGridView) {
                        FilledTonalIconButton(
                            onClick = onOpenDrawer,
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
                    TextButton(onClick = onToggleMonthlyView) {
                        Text(
                            text = stringResource(if (isMonthlyView) R.string.schedule_view_weekly else R.string.schedule_view_monthly),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isMonthlyView) {
                        IconButton(
                            onClick = onToggleGridView,
                            modifier = Modifier.testTag(if (isGridView) "list_view_button" else "grid_view_button")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = stringResource(if (isGridView) R.string.cd_list_view else R.string.cd_grid_view)
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
                        onScheduleClick = onScheduleClick,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isGridView) {
                    WeeklyScheduleGrid(
                        schedulesByDay = state.schedulesByDay,
                        onScheduleClick = onScheduleClick,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    WeeklyScheduleList(
                        schedulesByDay = state.schedulesByDay,
                        onScheduleClick = onScheduleClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Weekly Schedule Content")
@Composable
fun WeeklyScheduleContentPreview() {
    val sampleOccurrence = CalendarOccurrence(
        schedule = Schedule(
            id = "schedule1",
            studentId = "student1",
            dayOfWeek = DayOfWeek.MONDAY,
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
        date = java.time.LocalDate.now()
    )

    val state = WeeklyScheduleState(
        schedulesByDay = mapOf(
            DayOfWeek.MONDAY to listOf(WeeklyScheduleItem.Regular(sampleOccurrence))
        ),
        isLoading = false
    )

    HomeTutorProTheme {
        WeeklyScheduleContent(
            state = state,
            isMonthlyView = false,
            isGridView = false,
            onToggleMonthlyView = {},
            onToggleGridView = {},
            onScheduleClick = {},
            snackbarHostState = remember { SnackbarHostState() },
            onOpenDrawer = {}
        )
    }
}
