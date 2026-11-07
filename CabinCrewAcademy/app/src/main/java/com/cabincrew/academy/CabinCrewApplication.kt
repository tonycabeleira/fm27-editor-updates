package com.cabincrew.academy

import android.app.Application

class CabinCrewApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
