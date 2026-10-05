package com.example.spaceplan

import android.app.Application
import com.example.spaceplan.data.AppContainer

class SpacePlanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
