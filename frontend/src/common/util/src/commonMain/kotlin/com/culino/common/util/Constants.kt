package com.culino.common.util

object Constants {
    // 启动时由各平台入口(CulinoApplication / MainViewController / Web main)覆盖
    // Android: BuildConfig.API_BASE_URL
    // iOS: Info.plist API_BASE_URL
    // Web: window.location 推导
    var API_BASE_URL = ""
    const val TOKEN_KEY = "auth_token"
    const val CACHE_TTL_MINUTES = 30L
    const val PAGE_SIZE = 20
}
