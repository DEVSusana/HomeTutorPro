package com.devsusana.hometutorpro.presentation.student_list.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.presentation.utils.ColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableStudentCard(
    student: StudentSummary,
    onClick: () -> Unit,
    onToggleActive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onToggleActive()
                false // Don't dismiss, wait for confirmation/dialog handling
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> if (student.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                }, label = "swipe_background_color"
            )
            
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
                label = "swipe_icon_scale"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.scale(scale)
                ) {
                    Icon(
                        imageVector = if (student.isActive) Icons.Default.PersonOff else Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = stringResource(
                            if (student.isActive) R.string.student_list_action_deactivate 
                            else R.string.student_list_action_reactivate
                        ),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        ModernStudentCard(
            student = student,
            onClick = onClick
        )
    }
}

@Composable
fun ModernStudentCard(
    student: StudentSummary,
    onClick: () -> Unit
) {
    val studentColor = student.color?.let { Color(it) }
        ?: ColorUtils.getStudentColor(student.id)
    
    // Inactive styling
    val containerAlpha = if (student.isActive) 1f else 0.6f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("student_item_${student.id}"),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (student.isActive) 4.dp else 1.dp,
            pressedElevation = if (student.isActive) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = containerAlpha)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initial
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = if (student.isActive) studentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                border = if (student.isActive) 
                    BorderStroke(1.dp, studentColor.copy(alpha = 0.5f)) else null
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = student.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (student.isActive) studentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Student info - Name and Subjects
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name.ifEmpty { stringResource(id = R.string.student_list_unnamed_student) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (student.isActive) 1f else 0.6f)
                )
                
                // Show subjects if available
                if (student.subjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = student.subjects,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (student.isActive) 1f else 0.6f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                // Inactive Badge
                if (!student.isActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.student_status_inactive).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Balance badge
            if (student.pendingBalance > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (student.isActive) 1f else 0.6f),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = stringResource(id = R.string.currency_format, student.pendingBalance),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = if (student.isActive) 1f else 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else if (student.pendingBalance < 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (student.isActive) 1f else 0.6f),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = stringResource(id = R.string.currency_format, student.pendingBalance),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (student.isActive) 1f else 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else if (student.isActive) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Active Student Card")
@Composable
fun ActiveStudentCardPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        ModernStudentCard(
            student = StudentSummary(
                id = "1", 
                name = "Susana Gonzalez", 
                subjects = "Kotlin", 
                pendingBalance = 150.0, 
                pricePerHour = 20.0,
                isActive = true,
                color = null,
                lastClassDate = null
            ), 
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Inactive Student Card")
@Composable
fun InactiveStudentCardPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        ModernStudentCard(
            student = StudentSummary(
                id = "2", 
                name = "John Doe", 
                subjects = "Android", 
                pendingBalance = 0.0,  
                pricePerHour = 15.0,
                isActive = false,
                color = null,
                lastClassDate = null
            ), 
            onClick = {}
        )
    }
}
