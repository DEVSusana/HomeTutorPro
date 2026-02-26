package com.devsusana.hometutorpro.presentation.schedule.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils
import com.devsusana.hometutorpro.presentation.schedule.components.CompactScheduleCard
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import java.time.DayOfWeek

@Composable
fun WeeklyScheduleGrid(
    schedulesByDay: Map<DayOfWeek, List<WeeklyScheduleItem>>,
    onScheduleClick: (WeeklyScheduleItem.Regular) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysInOrder = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    BoxWithConstraints(modifier = modifier.padding(8.dp)) {
        val screenWidth = maxWidth
        val columnWidth = screenWidth / 7
        
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            daysInOrder.forEach { day ->
                val schedulesForDay = (schedulesByDay[day] ?: emptyList())
                    .filterIsInstance<WeeklyScheduleItem.Regular>()
                    .sortedBy { it.startTime }

                Column(
                    modifier = Modifier
                        .width(columnWidth)
                        .padding(horizontal = 2.dp)
                ) {
                    // Day header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = DayOfWeekUtils.getShortLocalizedName(day),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                                .semantics { heading() },
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Classes for this day
                    if (schedulesForDay.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.weekly_schedule_no_classes_grid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp),
                            fontSize = 10.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            schedulesForDay.forEach { regularItem ->
                                CompactScheduleCard(
                                    item = regularItem,
                                    onClick = { onScheduleClick(regularItem) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeeklyScheduleGridPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        WeeklyScheduleGrid(
            schedulesByDay = emptyMap(),
            onScheduleClick = {}
        )
    }
}
