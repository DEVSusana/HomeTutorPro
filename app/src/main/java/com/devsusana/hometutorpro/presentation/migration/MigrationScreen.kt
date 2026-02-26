package com.devsusana.hometutorpro.presentation.migration

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.migration.MigrationProgress
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MigrationScreen(
    onMigrationComplete: () -> Unit,
    viewModel: MigrationViewModel = hiltViewModel()
) {
    val state by viewModel.migrationState.collectAsStateWithLifecycle()

    val isCompleted = state is MigrationProgress.Completed
    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            onMigrationComplete()
        }
    }

    MigrationContent(
        state = state,
        onStartMigration = viewModel::startMigration
    )
}

@Composable
fun MigrationContent(
    state: MigrationProgress,
    onStartMigration: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.migration_title),
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        when (val currentState = state) {
            is MigrationProgress.Idle -> {
                Text(stringResource(R.string.migration_ready_message))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onStartMigration) {
                    Text(stringResource(R.string.migration_start))
                }
            }
            is MigrationProgress.Error -> {
                Text(
                    text = stringResource(R.string.migration_error_message, currentState.message),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onStartMigration) {
                    Text(stringResource(R.string.migration_retry))
                }
            }
            else -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = getProgressMessage(currentState))
            }
        }
    }
}

@Composable
private fun getProgressMessage(progress: MigrationProgress): String {
    return when (progress) {
        is MigrationProgress.Started -> stringResource(R.string.migration_msg_starting)
        is MigrationProgress.RegisteringUser -> stringResource(R.string.migration_msg_registering)
        is MigrationProgress.MigratingStudents -> stringResource(R.string.migration_msg_students)
        is MigrationProgress.MigratingSchedules -> stringResource(R.string.migration_msg_schedules)
        is MigrationProgress.MigratingResources -> stringResource(R.string.migration_msg_resources)
        is MigrationProgress.CleaningUp -> stringResource(R.string.migration_msg_cleanup)
        is MigrationProgress.Completed -> stringResource(R.string.migration_msg_complete)
        is MigrationProgress.Error -> stringResource(R.string.migration_msg_error)
        is MigrationProgress.Idle -> stringResource(R.string.migration_msg_ready)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun MigrationContentPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        MigrationContent(
            state = MigrationProgress.Idle,
            onStartMigration = {}
        )
    }
}