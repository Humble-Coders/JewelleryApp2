package com.humblecoders.jewelleryapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.humblecoders.jewelleryapp.utils.ImageLoaderConfig

class JewelleryApplication : Application(), ImageLoaderFactory {
    
    override fun onCreate() {
        super.onCreate()
        // Application initialization
    }
    
    // Provide custom ImageLoader for Coil
    override fun newImageLoader(): ImageLoader {
        return ImageLoaderConfig.createOptimizedImageLoader(this)
    }
}
