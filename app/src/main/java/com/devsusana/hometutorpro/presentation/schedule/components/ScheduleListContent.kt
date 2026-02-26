package com.devsusana.hometutorpro.presentation.schedule.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.presentation.schedule.ScheduleState
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListContent(
    state: ScheduleState,
    onAddScheduleClick: () -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag("schedule_list_screen"),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(id = R.string.schedule_list_title),
                        modifier = Modifier.semantics { heading() }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.schedule_list_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddScheduleClick, modifier = Modifier.testTag("add_schedule_button")) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.schedule_list_add_schedule))
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
            state.schedules.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_schedules))
                }
            }
            else -> {
                LazyColumn(contentPadding = padding) {
                    items(state.schedules) { schedule ->
                        ListItem(
                            headlineContent = { Text(schedule.dayOfWeek.toString()) },
                            supportingContent = { Text("${schedule.startTime} - ${schedule.endTime}") },
                            trailingContent = {
                                IconButton(
                                    onClick = { onDeleteSchedule(schedule.id) },
                                    modifier = Modifier.testTag("delete_schedule_${schedule.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(id = R.string.schedule_list_delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Schedule List - Empty")
@Composable
private fun ScheduleListContentEmptyPreview() {
    HomeTutorProTheme {
        ScheduleListContent(
            state = ScheduleState(),
            onAddScheduleClick = {},
            onDeleteSchedule = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Schedule List - With Schedules")
@Composable
private fun ScheduleListContentWithSchedulesPreview() {
    val schedules = listOf(
        Schedule(id = "1", dayOfWeek = DayOfWeek.MONDAY, startTime = "10:00", endTime = "11:00"),
        Schedule(id = "2", dayOfWeek = DayOfWeek.WEDNESDAY, startTime = "15:00", endTime = "16:30")
    )
    HomeTutorProTheme {
        ScheduleListContent(
            state = ScheduleState(schedules = schedules),
            onAddScheduleClick = {},
            onDeleteSchedule = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Schedule List - Loading")
@Composable
private fun ScheduleListContentLoadingPreview() {
    HomeTutorProTheme {
        ScheduleListContent(
            state = ScheduleState(isLoading = true),
            onAddScheduleClick = {},
            onDeleteSchedule = {},
            onBack = {}
        )
    }
}
