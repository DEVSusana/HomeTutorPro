package com.devsusana.hometutorpro.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.navigation.Route

data class BottomNavItem(
    val route: Route,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun rememberNavigationItems(): List<BottomNavItem> {
    return listOf(
        BottomNavItem(
            route = Route.Dashboard,
            label = stringResource(R.string.nav_dashboard),
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = Route.WeeklySchedule,
            label = stringResource(R.string.nav_schedule),
            icon = Icons.Default.CalendarToday
        ),
        BottomNavItem(
            route = Route.StudentList,
            label = stringResource(R.string.nav_students),
            icon = Icons.Default.People
        ),
        BottomNavItem(
            route = Route.Settings,
            label = stringResource(R.string.nav_settings),
            icon = Icons.Default.Settings
        )
    )
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = rememberNavigationItems()

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
            
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
