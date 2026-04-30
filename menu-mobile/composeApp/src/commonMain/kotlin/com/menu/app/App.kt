package com.menu.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.menu.app.di.AppComponent
import com.menu.core.ui.theme.MenuTheme

@Composable
fun App(dataStorePath: String) {
    val appComponent = remember { AppComponent(dataStorePath) }
    MenuTheme {
        MenuNavHost(appComponent = appComponent)
    }
}
