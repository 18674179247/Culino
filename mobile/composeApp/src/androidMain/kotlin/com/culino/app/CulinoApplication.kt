package com.culino.app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import com.culino.core.common.Constants
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class CulinoApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        Constants.API_BASE_URL = BuildConfig.API_BASE_URL
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
