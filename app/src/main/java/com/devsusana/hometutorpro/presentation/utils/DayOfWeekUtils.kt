package com.devsusana.hometutorpro.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.devsusana.hometutorpro.R
import java.time.DayOfWeek

object DayOfWeekUtils {
    fun getResourceId(dayOfWeek: DayOfWeek): Int {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> R.string.day_monday
            DayOfWeek.TUESDAY -> R.string.day_tuesday
            DayOfWeek.WEDNESDAY -> R.string.day_wednesday
            DayOfWeek.THURSDAY -> R.string.day_thursday
            DayOfWeek.FRIDAY -> R.string.day_friday
            DayOfWeek.SATURDAY -> R.string.day_saturday
            DayOfWeek.SUNDAY -> R.string.day_sunday
        }
    }

    @Composable
    fun getLocalizedName(dayOfWeek: DayOfWeek): String {
        return stringResource(id = getResourceId(dayOfWeek))
    }

    @Composable
    fun getShortLocalizedName(dayOfWeek: DayOfWeek): String {
        return getLocalizedName(dayOfWeek).take(3)
    }
}
