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

@Composable
fun MigrationScreen(
    onMigrationComplete: () -> Unit,
    viewModel: MigrationViewModel = hiltViewModel()
) {
    val state by viewModel.migrationState.collectAsState()

    LaunchedEffect(state) {
        if (state is MigrationProgress.Completed) {
            onMigrationComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Data Migration",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        when (val currentState = state) {
            is MigrationProgress.Idle -> {
                Text("Ready to migrate your local data to the cloud.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.startMigration() }) {
                    Text("Start Migration")
                }
            }
            is MigrationProgress.Error -> {
                Text(
                    text = "Error: ${currentState.message}",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.startMigration() }) {
                    Text("Retry")
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

private fun getProgressMessage(progress: MigrationProgress): String {
    return when (progress) {
        is MigrationProgress.Started -> "Starting migration..."
        is MigrationProgress.RegisteringUser -> "Registering user..."
        is MigrationProgress.MigratingStudents -> "Migrating students..."
        is MigrationProgress.MigratingSchedules -> "Migrating schedules..."
        is MigrationProgress.MigratingResources -> "Migrating resources..."
        is MigrationProgress.CleaningUp -> "Cleaning up..."
        is MigrationProgress.Completed -> "Migration complete!"
        is MigrationProgress.Error -> "Error occurred"
        is MigrationProgress.Idle -> "Ready"
    }
}
