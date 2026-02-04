package com.devsusana.hometutorpro.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.navigation.Route

@Composable
fun NavigationSideRail(navController: NavController) {
    val items = listOf(
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

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
            
            NavigationRailItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
