package com.devsusana.hometutorpro.presentation.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.domain.migration.MigrationProgress
import com.devsusana.hometutorpro.domain.usecases.IMigrateDataFromLocalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val migrateDataFromLocalUseCase: IMigrateDataFromLocalUseCase
) : ViewModel() {

    private val _migrationState = MutableStateFlow<MigrationProgress>(MigrationProgress.Idle)
    val migrationState: StateFlow<MigrationProgress> = _migrationState.asStateFlow()

    fun startMigration() {
        viewModelScope.launch {
            migrateDataFromLocalUseCase().collect { progress ->
                _migrationState.value = progress
            }
        }
    }
    
    fun resetState() {
        _migrationState.value = MigrationProgress.Idle
    }
}
