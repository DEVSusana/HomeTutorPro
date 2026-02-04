package com.devsusana.hometutorpro.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {

    @Serializable
    data object Splash : Route

    @Serializable
    data object Dashboard : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object Register : Route

    @Serializable
    data object StudentList : Route

    @Serializable
    data class StudentDetail(val studentId: String) : Route

    @Serializable
    data class Schedule(val studentId: String) : Route

    @Serializable
    data class ScheduleForm(val studentId: String) : Route

    @Serializable
    data object WeeklySchedule : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Resources : Route

    @Serializable
    data object Migration : Route

    @Serializable
    data object PremiumPurchase : Route

    @Serializable
    data object EditProfile : Route
}
