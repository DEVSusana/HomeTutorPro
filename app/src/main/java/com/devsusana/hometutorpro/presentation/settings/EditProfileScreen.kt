package com.devsusana.hometutorpro.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import com.devsusana.hometutorpro.presentation.components.TimePickerDialog
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

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
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = state.workingStartTime,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = {
                onEvent(EditProfileUiEvent.WorkingStartTimeChanged(it))
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = state.workingEndTime,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = {
                onEvent(EditProfileUiEvent.WorkingEndTimeChanged(it))
                showEndTimePicker = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.testTag("edit_profile_screen"),
        containerColor = MaterialTheme.colorScheme.background,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── Profile Avatar & Header ──────────────────────────────────────────
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = state.name.ifBlank { stringResource(R.string.dashboard_default_user_name) },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (state.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Card 1: Personal / Account Information ─────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.edit_profile_personal_info),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() }
                        )
                    }

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
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Card 2: Working Hours / Availability ───────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.edit_profile_working_hours),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() }
                        )
                    }

                    Text(
                        text = stringResource(R.string.edit_profile_working_hours_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Time
                        val startTimeContentDescription = stringResource(
                            R.string.cd_select_start_time,
                            state.workingStartTime
                        )
                        OutlinedTextField(
                            value = state.workingStartTime,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.start_time)) },
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = startTimeContentDescription
                                }
                                .testTag("start_time_button")
                                .clickable { showStartTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            interactionSource = remember { MutableInteractionSource() }
                                .also { interactionSource ->
                                    LaunchedEffect(interactionSource) {
                                        interactionSource.interactions.collect {
                                            if (it is PressInteraction.Release) {
                                                showStartTimePicker = true
                                            }
                                        }
                                    }
                                }
                        )

                        // End Time
                        val endTimeContentDescription = stringResource(
                            R.string.cd_select_end_time,
                            state.workingEndTime
                        )
                        OutlinedTextField(
                            value = state.workingEndTime,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.end_time)) },
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = endTimeContentDescription
                                }
                                .testTag("end_time_button")
                                .clickable { showEndTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            interactionSource = remember { MutableInteractionSource() }
                                .also { interactionSource ->
                                    LaunchedEffect(interactionSource) {
                                        interactionSource.interactions.collect {
                                            if (it is PressInteraction.Release) {
                                                showEndTimePicker = true
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Save Button ───────────────────────────────────────────────────
            Button(
                onClick = { onEvent(EditProfileUiEvent.SaveProfile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_button"),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.edit_profile_save))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Success/Error Feedback Dialog
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

@Preview(showBackground = true, name = "Edit Profile - Light")
@Composable
private fun EditProfileContentPreview() {
    HomeTutorProTheme(themeMode = com.devsusana.hometutorpro.domain.entities.AppThemeMode.LIGHT) {
        EditProfileContent(
            state = EditProfileState(
                name = "Susana Córdoba",
                email = "susana@example.com",
                workingStartTime = "08:00",
                workingEndTime = "23:00"
            ),
            onBackClick = {},
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Profile - Dark")
@Composable
private fun EditProfileContentDarkPreview() {
    HomeTutorProTheme(themeMode = com.devsusana.hometutorpro.domain.entities.AppThemeMode.DARK) {
        EditProfileContent(
            state = EditProfileState(
                name = "Susana Córdoba",
                email = "susana@example.com",
                workingStartTime = "08:00",
                workingEndTime = "23:00"
            ),
            onBackClick = {},
            onEvent = {}
        )
    }
}
