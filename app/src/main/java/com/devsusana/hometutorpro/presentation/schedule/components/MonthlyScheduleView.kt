package com.devsusana.hometutorpro.presentation.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import com.devsusana.hometutorpro.presentation.utils.ColorUtils
import com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils
import java.time.DayOfWeek

@Composable
fun MonthlyScheduleView(
    schedulesByDay: Map<DayOfWeek, List<WeeklyScheduleItem>>,
    onScheduleClick: (WeeklyScheduleItem.Regular) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simplified Monthly View showing a 4-week grid
    // Since we only have weekly schedule data, we'll repeat the weekly pattern
    // In a real app, this would need real dates
    
    val daysInOrder = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    Column(modifier = modifier.padding(8.dp)) {
        // Header Row
        Row(modifier = Modifier.fillMaxWidth()) {
            daysInOrder.forEach { day ->
                Text(
                    text = DayOfWeekUtils.getShortLocalizedName(day),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid (4 weeks simulation)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(4) { weekIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysInOrder.forEach { day ->
                        val schedulesForDay = (schedulesByDay[day] ?: emptyList()).filterIsInstance<WeeklyScheduleItem.Regular>()
                        
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(2.dp)) {
                                Text(
                                    text = "${(weekIndex * 7) + daysInOrder.indexOf(day) + 1}", // Fake date
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.End)
                                )
                                
                                schedulesForDay.take(3).forEach { regularItem ->
                                    val isCancelled = regularItem.exception?.type == ExceptionType.CANCELLED
                                    val studentColor = regularItem.student.color?.let { Color(it) }
                                        ?: ColorUtils.getStudentColor(regularItem.student.id)
                                    
                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 1.dp)
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .background(
                                                if (isCancelled) studentColor.copy(alpha = 0.3f) 
                                                else studentColor, 
                                                RoundedCornerShape(2.dp)
                                            )
                                    ) {
                                        // Add strikethrough effect for cancelled classes
                                        if (isCancelled) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .align(Alignment.Center)
                                                    .background(MaterialTheme.colorScheme.error)
                                            )
                                        }
                                    }
                                }
                                
                                if (schedulesForDay.size > 3) {
                                    Text(
                                        text = "+${schedulesForDay.size - 3}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
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
fun MonthlyScheduleViewPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        MonthlyScheduleView(
            schedulesByDay = emptyMap(),
            onScheduleClick = {}
        )
    }
}
