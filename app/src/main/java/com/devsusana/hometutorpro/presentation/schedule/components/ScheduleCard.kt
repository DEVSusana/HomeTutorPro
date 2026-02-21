package com.devsusana.hometutorpro.presentation.schedule.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import com.devsusana.hometutorpro.presentation.utils.ColorUtils
import java.time.DayOfWeek

@Composable
fun ScheduleCard(
    item: WeeklyScheduleItem.Regular,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exception = item.exception
    val isRescheduled = exception?.type == ExceptionType.RESCHEDULED
    val studentColor = item.student.color?.let { Color(it) }
        ?: ColorUtils.getStudentColor(item.student.id)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .clickable(onClick = onClick)
            .testTag("schedule_item"),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRescheduled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) 
                             else studentColor.copy(alpha = 0.15f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isRescheduled) MaterialTheme.colorScheme.error.copy(alpha=0.5f) else studentColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.student.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // Dark text
                )

                if (exception != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (exception.type == ExceptionType.CANCELLED) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "❌",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(id = R.string.weekly_schedule_cancelled),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (exception.type == ExceptionType.RESCHEDULED) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(id = R.string.weekly_schedule_rescheduled),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Time text (No pill background)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val displayStartTime = item.startTime
                val displayEndTime = item.endTime

                Text(
                    text = displayStartTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRescheduled) MaterialTheme.colorScheme.error else studentColor
                )
                Text(
                    text = displayEndTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRescheduled) MaterialTheme.colorScheme.error.copy(alpha=0.8f) else studentColor.copy(alpha=0.8f)
                )

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleCardPreview() {
    val mockSchedule = Schedule(
        studentId = "1",
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = "10:00",
        endTime = "11:00"
    )
    val mockStudent = StudentSummary(
        id = "1",
        name = "Susana",
        subjects = "Kotlin",
        color = null,
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        isActive = true,
        lastClassDate = null
    )
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        ScheduleCard(
            item = WeeklyScheduleItem.Regular(mockSchedule, mockStudent, date = java.time.LocalDate.now()),
            onClick = {}
        )
    }
}
