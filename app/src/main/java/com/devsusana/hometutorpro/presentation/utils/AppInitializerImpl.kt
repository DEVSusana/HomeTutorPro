package com.devsusana.hometutorpro.presentation.utils

import android.content.Context
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.data.util.DuplicateCleanupUtil
import com.devsusana.hometutorpro.di.ApplicationScope
import com.devsusana.hometutorpro.domain.usecases.AppInitializer
import com.devsusana.hometutorpro.domain.usecases.ISyncCoordinator
import com.devsusana.hometutorpro.domain.usecases.implementations.RescueOrphanedDataUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AppInitializer] that handles application-wide startup tasks
 * without exposing Android framework classes (like [Context]) to the domain layer
 * or running business logic directly inside the Application or MainActivity classes.
 */
@Singleton
class AppInitializerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncCoordinator: ISyncCoordinator,
    private val rescueOrphanedDataUseCase: RescueOrphanedDataUseCase,
    private val duplicateCleanupUtil: DuplicateCleanupUtil,
    @ApplicationScope private val applicationScope: CoroutineScope
) : AppInitializer {

    override fun initialize() {
        // Create notification channel
        NotificationHelper.createNotificationChannel(context)

        // Start observing sync states
        syncCoordinator.startObserving()

        // Clean up duplicate students and rescue orphaned data asynchronously
        applicationScope.launch(Dispatchers.IO) {
            try {
                rescueOrphanedDataUseCase()
                duplicateCleanupUtil.removeDuplicateStudents()
                duplicateCleanupUtil.removeLocalDuplicates()
            } catch (e: Exception) {
                // Silently fail - startup cleanup is best effort
            }
        }
    }
}
