package com.culino.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import com.culino.app.di.AppComponent
import com.culino.common.ui.theme.CulinoTheme

@Composable
fun App(dataStorePath: String, fontFamily: FontFamily? = null) {
    val appComponent = remember { AppComponent(dataStorePath) }
    CulinoTheme(fontFamily = fontFamily) {
        CulinoNavHost(appComponent = appComponent)
    }
}
