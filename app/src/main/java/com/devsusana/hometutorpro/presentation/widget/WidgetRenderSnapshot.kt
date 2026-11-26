package com.devsusana.hometutorpro.presentation.widget

/**
 * Snapshot data class holding the last rendered widget state to avoid redundant RemoteViews re-inflation.
 */
data class WidgetRenderSnapshot(
    val isActive: Boolean,
    val activeStudentId: String?,
    val activeStartTimeMillis: Long,
    val nextStudentId: String?,
    val nextStartTime: String?,
    val nextDate: String?,
    val doorCode: String?,
    val studentAddress: String? = null,
    val completedToday: Int,
    val totalToday: Int,
    val isActionsEnabled: Boolean = true
)
