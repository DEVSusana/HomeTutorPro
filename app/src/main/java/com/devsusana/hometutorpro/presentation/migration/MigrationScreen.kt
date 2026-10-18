package com.devsusana.hometutorpro.presentation.migration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsusana.hometutorpro.domain.migration.MigrationProgress

/**
 * Screen entry point for local data migration. Connects [MigrationViewModel] with [MigrationContent].
 */
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