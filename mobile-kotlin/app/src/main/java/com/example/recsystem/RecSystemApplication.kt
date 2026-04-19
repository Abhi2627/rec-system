package com.example.recsystem

import android.app.Application
import coil.Coil
import coil.ImageLoader
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class RecSystemApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure Coil with a generous timeout so TMDB poster images
        // load reliably on slower networks and don't fail silently.
        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
            }
            .crossfade(true)
            .crossfade(300)
            .build()

        Coil.setImageLoader(imageLoader)
    }
}
