package com.studentreviewer.offline.app

import android.app.Application
import android.content.ComponentCallbacks2
import com.studentreviewer.offline.core.di.AppContainer
import com.studentreviewer.offline.core.di.DefaultAppContainer

class ReviewerApp : Application(), ComponentCallbacks2 {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }
}
