package com.devsusana.hometutorpro.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.navigation.graphs.authGraph
import com.devsusana.hometutorpro.navigation.graphs.mainGraph
import com.devsusana.hometutorpro.navigation.graphs.scheduleGraph
import com.devsusana.hometutorpro.navigation.graphs.studentGraph
import com.devsusana.hometutorpro.presentation.components.BottomNavigationBar
import com.devsusana.hometutorpro.presentation.components.rememberNavigationItems
import kotlinx.coroutines.launch

@Composable
fun NavigationHost() {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navigationItems = rememberNavigationItems()
    
    var forceHideBottomBar by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun isRoute(route: Route): Boolean {
        return currentDestination?.hierarchy?.any { 
           it.hasRoute(route::class)
        } == true
    }

    val showNavigation = 
        isRoute(Route.Dashboard) ||
        isRoute(Route.WeeklySchedule) ||
        isRoute(Route.StudentList) ||
        isRoute(Route.Settings)

    val navigationControl = remember(scope, drawerState) {
        NavigationControl(
            setHideBottomBar = { forceHideBottomBar = it },
            openDrawer = { scope.launch { drawerState.open() } }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = (isLandscape || forceHideBottomBar) && showNavigation,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                navigationItems.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        selected = isSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        CompositionLocalProvider(LocalNavigationControl provides navigationControl) {
            Scaffold(
                bottomBar = {
                    if (showNavigation && !isLandscape && !forceHideBottomBar) {
                        BottomNavigationBar(navController)
                    }
                },
                            floatingActionButton = {
                                if (showNavigation && isLandscape) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.cd_menu))
                            }
                                }
                            }            ) { paddingValues ->
                NavHost(
                    navController = navController, 
                    startDestination = Route.Splash,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    mainGraph(navController)
                    authGraph(navController)
                    studentGraph(navController)
                    scheduleGraph(navController)
                }
            }
        }
    }
}