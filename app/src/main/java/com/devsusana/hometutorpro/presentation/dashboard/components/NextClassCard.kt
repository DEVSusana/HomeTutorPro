package com.devsusana.hometutorpro.presentation.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.devsusana.hometutorpro.presentation.utils.ColorUtils
import com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.entities.Schedule
import java.time.DayOfWeek
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

@Composable
fun NextClassCard(
    item: WeeklyScheduleItem.Regular,
    onClick: () -> Unit
) {
    val studentColor = item.student.color?.let { Color(it) } ?: ColorUtils.getStudentColor(item.student.id)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.schedule.startTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = DayOfWeekUtils.getShortLocalizedName(item.schedule.dayOfWeek).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.student.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Extra info (course) removed as requested
                }
                
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = studentColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, studentColor.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.student.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = studentColor
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NextClassCardPreview() {
    val mockStudent = StudentSummary(
        id = "1",
        name = "Maria Garcia",
        subjects = "Mathematics",
        color = null,
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        isActive = true,
        lastClassDate = null
    )
    val mockSchedule = Schedule(
        studentId = "1",
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = "10:00",
        endTime = "11:00"
    )
    val mockScheduleWithStudent = WeeklyScheduleItem.Regular(
        schedule = mockSchedule,
        student = mockStudent,
        date = java.time.LocalDate.now()
    )

    HomeTutorProTheme {
        NextClassCard(
            item = mockScheduleWithStudent,
            onClick = {}
        )
    }
}
