package com.devsusana.hometutorpro.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    EditProfileContent(
        state = state,
        onBackClick = onBackClick,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileContent(
    state: EditProfileState,
    onBackClick: () -> Unit,
    onEvent: (EditProfileUiEvent) -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag("edit_profile_screen"),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.edit_profile_title),
                        modifier = Modifier.semantics { heading() }
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_card"),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.settings_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Name
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onEvent(EditProfileUiEvent.NameChanged(it)) },
                        label = { Text(stringResource(R.string.edit_profile_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_field"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onEvent(EditProfileUiEvent.EmailChanged(it)) },
                        label = { Text(stringResource(R.string.edit_profile_email)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_field"),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onEvent(EditProfileUiEvent.PasswordChanged(it)) },
                        label = { Text(stringResource(R.string.edit_profile_password)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_field"),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            val icon = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                            val contentDesc = if (state.isPasswordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                            
                            IconButton(onClick = { onEvent(EditProfileUiEvent.TogglePasswordVisibility) }) {
                                Icon(imageVector = icon, contentDescription = contentDesc)
                            }
                        },
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Working Hours Section
                    Text(
                        text = stringResource(R.string.edit_profile_working_hours),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Start Time Picker
                    var showStartTimePicker by remember { mutableStateOf(false) }
                    Column {
                        val startTimeContentDescription = stringResource(
                            R.string.cd_select_start_time,
                            state.workingStartTime
                        )
                        Text(
                            text = stringResource(R.string.edit_profile_working_start, state.workingStartTime),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = startTimeContentDescription
                                }
                                .testTag("start_time_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.workingStartTime)
                        }
                    }

                    if (showStartTimePicker) {
                        com.devsusana.hometutorpro.presentation.components.TimePickerDialog(
                            initialTime = state.workingStartTime,
                            onDismiss = { showStartTimePicker = false },
                            onTimeSelected = { 
                                onEvent(EditProfileUiEvent.WorkingStartTimeChanged(it))
                                showStartTimePicker = false
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // End Time Picker
                    var showEndTimePicker by remember { mutableStateOf(false) }
                    Column {
                        val endTimeContentDescription = stringResource(
                            R.string.cd_select_end_time,
                            state.workingEndTime
                        )
                        Text(
                            text = stringResource(R.string.edit_profile_working_end, state.workingEndTime),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = endTimeContentDescription
                                }
                                .testTag("end_time_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.workingEndTime)
                        }
                    }

                    if (showEndTimePicker) {
                        com.devsusana.hometutorpro.presentation.components.TimePickerDialog(
                            initialTime = state.workingEndTime,
                            onDismiss = { showEndTimePicker = false },
                            onTimeSelected = { 
                                onEvent(EditProfileUiEvent.WorkingEndTimeChanged(it))
                                showEndTimePicker = false
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onEvent(EditProfileUiEvent.SaveProfile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_button"),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.edit_profile_save))
                        }
                    }
                }
            }
        }
    }

    // Success/Error Feedback
    if (state.successMessage != null || state.errorMessage != null) {
        FeedbackDialog(
            isSuccess = state.successMessage != null,
            message = { 
                Text(text = state.successMessage ?: state.errorMessage ?: "") 
            },
            onDismiss = { onEvent(EditProfileUiEvent.DismissFeedback) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileContentPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        EditProfileContent(
            state = EditProfileState(
                name = "Professor Name",
                email = "professor@example.com",
                workingStartTime = "09:00",
                workingEndTime = "18:00"
            ),
            onBackClick = {},
            onEvent = {}
        )
    }
}
