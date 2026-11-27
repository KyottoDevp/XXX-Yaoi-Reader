package com.independent.xxxyaoireader

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.independent.xxxyaoireader.data.AppStore
import com.independent.xxxyaoireader.data.XXXYaoiClient
import okhttp3.OkHttpClient

class XXXYaoiApplication : Application(), ImageLoaderFactory {
    val networkClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        XXXYaoiClient(this, AppStore(this)).okHttpClient
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { networkClient }
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()
}
