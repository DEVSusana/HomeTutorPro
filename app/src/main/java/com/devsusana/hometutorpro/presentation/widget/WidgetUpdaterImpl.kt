package com.devsusana.hometutorpro.presentation.widget

import android.content.Context
import com.devsusana.hometutorpro.domain.repository.IWidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [IWidgetUpdater] that triggers [ClassTimerWidget] updates.
 */
@Singleton
class WidgetUpdaterImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : IWidgetUpdater {
    override fun updateWidget() {
        ClassTimerWidget.updateWidget(context)
    }
}
