package com.devsusana.hometutorpro.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.devsusana.hometutorpro.navigation.Route
import com.devsusana.hometutorpro.presentation.login.LoginScreen
import com.devsusana.hometutorpro.presentation.register.RegisterScreen

fun NavGraphBuilder.authGraph(navController: NavController) {
    composable<Route.Login> {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Route.Dashboard) {
                    popUpTo(Route.Login) { inclusive = true }
                }
            },
            onRegisterClick = { navController.navigate(Route.Register) }
        )
    }
    composable<Route.Register> {
        RegisterScreen(
            onRegisterSuccess = {
                navController.navigate(Route.Dashboard) {
                    popUpTo(Route.Register) { inclusive = true }
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}
