package com.devsusana.hometutorpro.presentation.migration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.migration.MigrationProgress
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

/**
 * Pure presentation composable for local data migration.
 */
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

@Preview(showBackground = true)
@Composable
private fun MigrationContentPreview() {
    HomeTutorProTheme {
        MigrationContent(
            state = MigrationProgress.Idle,
            onStartMigration = {}
        )
    }
}
