package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.AppFeatureFlags
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case to observe the current application feature flags and remote configuration.
 */
interface IGetAppFeatureFlagsUseCase {
    operator fun invoke(): StateFlow<AppFeatureFlags>
}
