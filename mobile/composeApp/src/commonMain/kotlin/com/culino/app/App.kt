package com.culino.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.culino.app.di.AppComponent
import com.culino.core.ui.theme.CulinoTheme

@Composable
fun App(dataStorePath: String) {
    val appComponent = remember { AppComponent(dataStorePath) }
    CulinoTheme {
        CulinoNavHost(appComponent = appComponent)
    }
}
