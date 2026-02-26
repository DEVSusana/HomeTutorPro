package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailEvent
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tab displaying finance information and actions for a student.
 */
@Composable
fun FinanceTab(
    student: Student,
    state: StudentDetailState,
    isEditMode: Boolean,
    isNewStudent: Boolean,
    onEvent: (StudentDetailEvent) -> Unit,
    onPaymentClick: () -> Unit,
    onStartClassClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Finance Section
        SectionCard(
            title = stringResource(R.string.student_detail_finance),
            icon = Icons.Default.AttachMoney
        ) {
            OutlinedTextField(
                value = state.priceInput,
                onValueChange = { onEvent(StudentDetailEvent.PriceChange(it)) },
                label = { Text(stringResource(id = R.string.student_detail_price_per_hour)) },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().testTag("price_field"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            if (!isNewStudent) {
                Spacer(modifier = Modifier.height(16.dp))

                // Balance Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (student.pendingBalance > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isBalanceEditable) {
                                OutlinedTextField(
                                    value = state.balanceInput,
                                    onValueChange = { onEvent(StudentDetailEvent.BalanceChange(it)) },
                                    label = { Text(stringResource(id = R.string.student_detail_pending_balance_label)) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.student_detail_pending_balance, student.pendingBalance),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isEditMode) {
                                IconButton(onClick = { onEvent(StudentDetailEvent.ToggleBalanceEdit) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(id = R.string.student_detail_edit_balance),
                                        tint = if (state.isBalanceEditable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (student.lastPaymentDate != null) {
                            val formatPattern = stringResource(id = R.string.date_time_format)
                            val date = SimpleDateFormat(formatPattern, Locale.getDefault()).format(
                                Date(student.lastPaymentDate)
                            )
                            Text(
                                text = stringResource(id = R.string.student_detail_last_payment, date),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onPaymentClick,
                    enabled = student.pendingBalance > 0,
                    modifier = Modifier.fillMaxWidth().testTag("register_payment_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.student_detail_register_payment))
                }
            }
        }

        // Quick Actions for starting class
        if (!isNewStudent) {
            SectionCard(title = stringResource(R.string.student_detail_actions), icon = Icons.Default.Bolt) {
                Button(
                    onClick = onStartClassClick,
                    modifier = Modifier.fillMaxWidth().testTag("start_class_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.cd_play_icon))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.student_detail_start_class))
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onEvent(StudentDetailEvent.ShowExtraClassDialog) },
                    modifier = Modifier.fillMaxWidth().testTag("add_extra_class_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.student_detail_add_extra_class))
                }
            }
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Preview(showBackground = true, name = "Finance Tab - With Balance")
@Composable
private fun FinanceTabWithBalancePreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        pendingBalance = 150.0,
        pricePerHour = 20.0,
        lastPaymentDate = System.currentTimeMillis() - 86400000, // 1 day ago
        studentEmail = "susana@example.com",
        isActive = true
    )
    
    HomeTutorProTheme {
        FinanceTab(
            student = mockStudent,
            state = StudentDetailState(student = mockStudent),
            isEditMode = false,
            isNewStudent = false,
            onEvent = {},
            onPaymentClick = {},
            onStartClassClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Finance Tab - No Balance")
@Composable
private fun FinanceTabNoBalancePreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        studentEmail = "susana@example.com",
        isActive = true
    )
    
    HomeTutorProTheme {
        FinanceTab(
            student = mockStudent,
            state = StudentDetailState(student = mockStudent),
            isEditMode = false,
            isNewStudent = false,
            onEvent = {},
            onPaymentClick = {},
            onStartClassClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Finance Tab - Edit Mode")
@Composable
private fun FinanceTabEditModePreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        pendingBalance = 150.0,
        pricePerHour = 20.0,
        studentEmail = "susana@example.com",
        isActive = true
    )
    
    HomeTutorProTheme {
        FinanceTab(
            student = mockStudent,
            state = StudentDetailState(student = mockStudent, isBalanceEditable = true, balanceInput = "150.0"),
            isEditMode = true,
            isNewStudent = false,
            onEvent = {},
            onPaymentClick = {},
            onStartClassClick = {}
        )
    }
}
