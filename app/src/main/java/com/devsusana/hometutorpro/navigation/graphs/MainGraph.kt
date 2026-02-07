package com.devsusana.hometutorpro.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.devsusana.hometutorpro.navigation.Route
import com.devsusana.hometutorpro.presentation.dashboard.DashboardScreen
import com.devsusana.hometutorpro.presentation.migration.MigrationScreen
import com.devsusana.hometutorpro.presentation.premium.PremiumPurchaseScreen
import com.devsusana.hometutorpro.presentation.resources.ResourcesScreen
import com.devsusana.hometutorpro.presentation.settings.EditProfileScreen
import com.devsusana.hometutorpro.presentation.settings.SettingsScreen
import com.devsusana.hometutorpro.presentation.splash.SplashScreen

fun NavGraphBuilder.mainGraph(navController: NavController) {
    composable<Route.Splash> {
        SplashScreen(
            onNavigateToLogin = {
                navController.navigate(Route.Login) {
                    popUpTo(Route.Splash) { inclusive = true }
                }
            },
            onNavigateToHome = {
                navController.navigate(Route.Dashboard) {
                    popUpTo(Route.Splash) { inclusive = true }
                }
            }
        )
    }

    composable<Route.Dashboard> {
        DashboardScreen(
            onNavigateToStudents = { navController.navigate(Route.StudentList) },
            onNavigateToResources = { navController.navigate(Route.Resources) },
            onAddStudent = { navController.navigate(Route.StudentDetail("new")) },
            onNavigateToNotes = { navController.navigate(Route.Notes) }
        )
    }

    composable<Route.Notes> {
        com.devsusana.hometutorpro.presentation.notes.NotesScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<Route.Settings> {
        SettingsScreen(
            onLogoutClick = {
                navController.navigate(Route.Login) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onPremiumClick = {
                navController.navigate(Route.PremiumPurchase)
            },
            onEditProfileClick = {
                navController.navigate(Route.EditProfile)
            }
        )
    }

    composable<Route.EditProfile> {
        EditProfileScreen(
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<Route.Resources> {
        ResourcesScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable<Route.Migration> {
        MigrationScreen(
            onMigrationComplete = {
                navController.navigate(Route.WeeklySchedule) {
                    popUpTo(Route.Migration) { inclusive = true }
                }
            }
        )
    }

    composable<Route.PremiumPurchase> {
        PremiumPurchaseScreen(
            onPurchaseSuccess = {
                navController.popBackStack()
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}
