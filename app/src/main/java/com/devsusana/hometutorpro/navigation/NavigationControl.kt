package com.devsusana.hometutorpro.navigation

import androidx.compose.runtime.compositionLocalOf

data class NavigationControl(
    val setHideBottomBar: (Boolean) -> Unit = {},
    val openDrawer: () -> Unit = {}
)

val LocalNavigationControl = compositionLocalOf { NavigationControl() }
