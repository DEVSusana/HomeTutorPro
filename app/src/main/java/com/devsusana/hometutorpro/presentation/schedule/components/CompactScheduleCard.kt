package com.devsusana.hometutorpro.presentation.schedule.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import com.devsusana.hometutorpro.presentation.utils.ColorUtils
import java.time.DayOfWeek

@Composable
fun CompactScheduleCard(
    item: WeeklyScheduleItem.Regular,
    onClick: () -> Unit
) {
    val exception = item.exception
    val isRescheduled = exception?.type == ExceptionType.RESCHEDULED
    val isCancelled = exception?.type == ExceptionType.CANCELLED
    
    val studentColor = item.student.color?.let { Color(it) }
        ?: ColorUtils.getStudentColor(item.student.id)

    val displayStartTime = item.startTime
    val contentDescription = when {
        isCancelled -> stringResource(
            R.string.cd_schedule_item_cancelled,
            item.student.name,
            item.startTime,
            item.endTime
        )
        isRescheduled -> stringResource(
            R.string.cd_schedule_item_rescheduled,
            item.student.name,
            item.startTime,
            item.endTime
        )
        else -> stringResource(
            R.string.cd_schedule_item,
            item.student.name,
            item.startTime,
            item.endTime
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else studentColor.copy(alpha = 0.2f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isRescheduled || isCancelled) MaterialTheme.colorScheme.error else studentColor.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = item.student.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = displayStartTime,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                textDecoration = if (isCancelled) TextDecoration.LineThrough else null
            )
            
            if (isRescheduled) {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompactScheduleCardPreview() {
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
        CompactScheduleCard(
            item = WeeklyScheduleItem.Regular(mockSchedule, mockStudent, date = java.time.LocalDate.now()),
            onClick = {}
        )
    }
}
