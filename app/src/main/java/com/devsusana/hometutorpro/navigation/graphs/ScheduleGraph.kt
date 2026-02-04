package com.devsusana.hometutorpro.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.devsusana.hometutorpro.navigation.Route
import com.devsusana.hometutorpro.presentation.schedule.ScheduleScreen
import com.devsusana.hometutorpro.presentation.schedule_form.ScheduleFormScreen
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleScreen

fun NavGraphBuilder.scheduleGraph(navController: NavController) {
    composable<Route.WeeklySchedule> {
        WeeklyScheduleScreen(
            onBack = { navController.popBackStack() },
            onStudentListClick = {
                navController.navigate(Route.StudentList)
            },
            onResourcesClick = {
                navController.navigate(Route.Resources)
            },
            onPremiumClick = {
                navController.navigate(Route.PremiumPurchase)
            }
        )
    }

    composable<Route.Schedule> { backStackEntry ->
        val route: Route.Schedule = backStackEntry.toRoute()
        ScheduleScreen(
            onBack = { navController.popBackStack() },
            onAddScheduleClick = {
                navController.navigate(Route.ScheduleForm(route.studentId))
            }
        )
    }

    composable<Route.ScheduleForm> { 
        // studentId retrieved via SavedStateHandle in ViewModel
        ScheduleFormScreen(
            onBack = { navController.popBackStack() }
        )
    }
}
