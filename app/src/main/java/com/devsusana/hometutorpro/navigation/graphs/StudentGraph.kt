package com.devsusana.hometutorpro.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.devsusana.hometutorpro.navigation.Route
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailScreen
import com.devsusana.hometutorpro.presentation.student_list.StudentListScreen

fun NavGraphBuilder.studentGraph(navController: NavController) {
    composable<Route.StudentList> {
        StudentListScreen(
            onStudentClick = { studentId ->
                navController.navigate(Route.StudentDetail(studentId))
            },
            onAddStudentClick = {
                navController.navigate(Route.StudentDetail("new"))
            },
            onWeeklyScheduleClick = {
                navController.navigate(Route.WeeklySchedule) {
                    popUpTo(Route.WeeklySchedule) { inclusive = true }
                }
            },
            onLogout = {
                navController.navigate(Route.Login) {
                    popUpTo(Route.StudentList) { inclusive = true }
                }
            }
        )
    }
    
    composable<Route.StudentDetail> { backStackEntry ->
        val route: Route.StudentDetail = backStackEntry.toRoute()
        // StudentDetailViewModel uses SavedStateHandle to retrieve studentId, 
        // so we don't strictly need to pass it, but it's good practice for the route object to be available.
        StudentDetailScreen(
            onBack = { navController.popBackStack() }
        )
    }
}
